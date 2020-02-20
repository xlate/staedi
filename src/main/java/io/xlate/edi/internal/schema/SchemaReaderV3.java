package io.xlate.edi.internal.schema;

import static io.xlate.edi.internal.schema.StaEDISchemaFactory.schemaException;
import static io.xlate.edi.internal.schema.StaEDISchemaFactory.unexpectedElement;
import static io.xlate.edi.internal.schema.StaEDISchemaFactory.unexpectedEvent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import io.xlate.edi.internal.schema.implementation.CompositeImpl;
import io.xlate.edi.internal.schema.implementation.DiscriminatorImpl;
import io.xlate.edi.internal.schema.implementation.ElementImpl;
import io.xlate.edi.internal.schema.implementation.LoopImpl;
import io.xlate.edi.internal.schema.implementation.Positioned;
import io.xlate.edi.internal.schema.implementation.SegmentImpl;
import io.xlate.edi.internal.schema.implementation.TransactionImpl;
import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.implementation.Discriminator;
import io.xlate.edi.schema.implementation.EDITypeImplementation;
import io.xlate.edi.schema.implementation.LoopImplementation;

class SchemaReaderV3 extends SchemaReaderBase implements SchemaReader {

    private static final Logger LOGGER = Logger.getLogger(SchemaReaderV3.class.getName());

    private static final String ATTR_MIN_OCCURS = "minOccurs";
    private static final String ATTR_MAX_OCCURS = "maxOccurs";
    private static final String ATTR_POSITION = "position";
    private static final String ATTR_DISCRIMINATOR = "discriminator";

    final QName qnImplementation;
    final Deque<EDITypeImplementation> implementedTypes = new LinkedList<>();

    public SchemaReaderV3(XMLStreamReader reader) {
        super(StaEDISchemaFactory.XMLNS_V3, reader);
        qnImplementation = new QName(xmlns, "implementation");
    }

    @Override
    public String getImplementationName() {
        return qnImplementation.toString();
    }

    @Override
    protected String readReferencedId(XMLStreamReader reader) {
        String id = reader.getAttributeValue(null, "type");
        if (id == null) {
            id = reader.getAttributeValue(null, "ref");

            if (id != null) {
                Location parseLocation = reader.getLocation();
                LOGGER.warning("Attribute 'ref' is deprecated at line "
                        + parseLocation.getLineNumber()
                        + ", column " + parseLocation.getColumnNumber());
            }
        }
        return id;
    }

    @Override
    void readTransaction(XMLStreamReader reader, Map<String, EDIType> types) throws XMLStreamException {
        super.readTransaction(reader, types);

        reader.nextTag();
        QName element = reader.getName();

        LoopImplementation impl = readImplementation(reader, element, types);
        if (impl != null) {
            types.put(qnImplementation.toString(), impl);
        }
    }

    @Override
    void setReferences(Map<String, EDIType> types) {
        super.setReferences(types);

        // FIXME: index bounds checks
        /*
         * throw schemaException("Position " + position + " does not correspond to an entry in " + standardParent.getId(), reader);
         */
        implementedTypes.descendingIterator().forEachRemaining(i -> {
            switch (i.getType()) {
            case COMPOSITE: {
                CompositeImpl impl = (CompositeImpl) i;
                EDIComplexType standard = (EDIComplexType) impl.getReferencedType();
                for (EDITypeImplementation t : impl.getSequence()) {
                    if (t instanceof Positioned) {
                        Positioned p = (Positioned) t;
                        int offset = p.getPosition() - 1;
                        EDIType pStd = standard.getReferences().get(offset).getReferencedType();
                        ((ElementImpl) p).setStandard((EDISimpleType) pStd);
                    }
                }
                break;
            }
            case LOOP: {
                LoopImpl impl = (LoopImpl) i;
                String typeId = QN_LOOP.toString() + '.' + impl.getTypeId();
                EDIComplexType standard = (EDIComplexType) types.get(typeId);
                impl.setStandard(standard);
                break;
            }
            case SEGMENT: {
                SegmentImpl impl = (SegmentImpl) i;
                String typeId = impl.getTypeId();
                EDIComplexType standard = (EDIComplexType) types.get(typeId);
                impl.setStandard(standard);
                for (EDITypeImplementation t : impl.getSequence()) {
                    if (t instanceof Positioned) {
                        Positioned p = (Positioned) t;
                        int offset = p.getPosition() - 1;
                        EDIType pStd = standard.getReferences().get(offset).getReferencedType();
                        if (p instanceof ElementImpl) {
                            ((ElementImpl) p).setStandard((EDISimpleType) pStd);
                        } else {
                            ((CompositeImpl) p).setStandard((EDIComplexType) pStd);
                        }
                    }
                }
                break;
            }
            default:
                break;

            }
        });
    }

    LoopImplementation readImplementation(XMLStreamReader reader,
                                          QName complexType,
                                          Map<String, EDIType> types) throws XMLStreamException {

        if (!qnImplementation.equals(complexType)) {
            return null;
        }

        LoopImplementation loop = readLoopImplementation(reader, complexType, true);
        String typeId = QN_TRANSACTION.toString();
        EDIComplexType standard = (EDIComplexType) types.get(typeId);
        LoopImpl impl = new TransactionImpl(qnImplementation.toString(), typeId, loop.getSequence());
        impl.setStandard(standard);
        return impl;
    }

    LoopImplementation readLoopImplementation(XMLStreamReader reader,
                                              QName complexType,
                                              boolean transactionLoop) throws XMLStreamException {

        List<EDITypeImplementation> sequence = new ArrayList<>();
        String id;
        String typeId;
        int minOccurs = 0;
        int maxOccurs = 0;
        BigDecimal discriminatorAttr = null;

        if (transactionLoop) {
            id = qnImplementation.toString();
            typeId = null;
        } else {
            id = parseAttribute(reader, "code", String::valueOf);
            typeId = parseAttribute(reader, "type", String::valueOf);
            minOccurs = parseAttribute(reader, ATTR_MIN_OCCURS, Integer::parseInt, 0);
            maxOccurs = parseAttribute(reader, ATTR_MAX_OCCURS, Integer::parseInt, 0);
            discriminatorAttr = parseAttribute(reader, ATTR_DISCRIMINATOR, BigDecimal::new, null);
        }

        //TODO: Read title attribute and description element

        if (reader.nextTag() == XMLStreamConstants.START_ELEMENT) {
            readSequence(reader, e -> readLoopSequenceEntry(e, sequence));
        } else {
            throw unexpectedEvent(reader);
        }

        if (reader.nextTag() == XMLStreamConstants.END_ELEMENT) {
            QName element = reader.getName();

            if (element.equals(complexType)) {
                Discriminator disc = null;
                if (discriminatorAttr != null) {
                    SegmentImpl segImpl = (SegmentImpl) sequence.get(0);
                    disc = buildDiscriminator(discriminatorAttr, segImpl.getSequence());
                }

                LoopImpl loop = new LoopImpl(minOccurs, maxOccurs, id, typeId, disc, sequence);
                implementedTypes.add(loop);
                return loop;
            } else {
                throw unexpectedElement(element, reader);
            }
        } else {
            throw unexpectedEvent(reader);
        }
    }

    void readLoopSequenceEntry(QName entryName, List<EDITypeImplementation> sequence) {
        try {
            if (entryName.equals(QN_LOOP)) {
                if (sequence.isEmpty()) {
                    throw schemaException("segment element must be first child of loop sequence", reader);
                }
                sequence.add(readLoopImplementation(reader, entryName, false));
            } else if (entryName.equals(QN_SEGMENT)) {
                sequence.add(readSegmentImplementation(reader));
            } else {
                throw unexpectedElement(entryName, reader);
            }
        } catch (XMLStreamException xse) {
            throw schemaException("Exception reading loop sequence", reader, xse);
        }

    }

    SegmentImpl readSegmentImplementation(XMLStreamReader reader) throws XMLStreamException {
        //TODO: Read title attribute and description element
        List<EDITypeImplementation> sequence = new ArrayList<>();
        String typeId = parseAttribute(reader, "type", String::valueOf);
        int minOccurs = parseAttribute(reader, ATTR_MIN_OCCURS, Integer::parseInt, 0);
        int maxOccurs = parseAttribute(reader, ATTR_MAX_OCCURS, Integer::parseInt, 0);
        BigDecimal discriminatorAttr = parseAttribute(reader, ATTR_DISCRIMINATOR, BigDecimal::new, null);

        int event = reader.nextTag();

        if (event == XMLStreamConstants.START_ELEMENT) {
            readSequence(reader, e -> readSegmentSequenceEntry(e, sequence));
        } else if (event == XMLStreamConstants.END_ELEMENT) {
            return newSegmentImpl(reader, minOccurs, maxOccurs, typeId, discriminatorAttr, sequence);
        } else {
            throw unexpectedEvent(reader);
        }

        if (reader.nextTag() == XMLStreamConstants.END_ELEMENT) {
            return newSegmentImpl(reader, minOccurs, maxOccurs, typeId, discriminatorAttr, sequence);
        } else {
            throw unexpectedEvent(reader);
        }
    }

    void readSegmentSequenceEntry(QName entryName, List<EDITypeImplementation> sequence) {
        try {
            if (entryName.equals(QN_ELEMENT)) {
                ElementImpl element = readElementImplementation(reader);
                implementedTypes.add(element);
                sequence.add(element.getPosition() - 1, element);
            } else if (entryName.equals(QN_COMPOSITE)) {
                CompositeImpl element = readCompositeImplementation(reader);
                implementedTypes.add(element);
                sequence.add(element.getPosition() - 1, element);
            } else {
                throw unexpectedElement(entryName, reader);
            }
        } catch (XMLStreamException xse) {
            throw schemaException("Exception reading segment sequence", reader, xse);
        }
    }

    SegmentImpl newSegmentImpl(XMLStreamReader reader,
                               int minOccurs,
                               int maxOccurs,
                               String typeId,
                               BigDecimal discriminatorAttr,
                               List<EDITypeImplementation> sequence) {

        QName element = reader.getName();

        if (element.equals(QN_SEGMENT)) {
            Discriminator disc = buildDiscriminator(discriminatorAttr, sequence);
            SegmentImpl segment = new SegmentImpl(minOccurs, maxOccurs, typeId, typeId, disc, sequence);
            implementedTypes.add(segment);
            return segment;
        } else {
            throw unexpectedElement(element, reader);
        }
    }

    Discriminator buildDiscriminator(BigDecimal discriminatorAttr, List<EDITypeImplementation> sequence) {
        Discriminator disc = null;

        if (discriminatorAttr != null) {
            int elementPosition = discriminatorAttr.intValue();
            int componentPosition = discriminatorAttr.remainder(BigDecimal.ONE)
                                                     .movePointRight(discriminatorAttr.scale()).intValue();
            EDITypeImplementation eleImpl = sequence.get(elementPosition - 1);

            if (eleImpl.getType() == EDIType.Type.ELEMENT && componentPosition < 1) {
                disc = new DiscriminatorImpl(elementPosition,
                                             componentPosition,
                                             ((ElementImpl) eleImpl).getValueSet());
            } else if (eleImpl.getType() == EDIType.Type.COMPOSITE && componentPosition > 0) {
                eleImpl = ((CompositeImpl) eleImpl).getSequence().get(componentPosition - 1);
                disc = new DiscriminatorImpl(elementPosition,
                                             componentPosition,
                                             ((ElementImpl) eleImpl).getValueSet());
            } else {
                // TODO: error
            }
        }

        return disc;
    }

    CompositeImpl readCompositeImplementation(XMLStreamReader reader) throws XMLStreamException {
        List<EDITypeImplementation> sequence = new ArrayList<>();
        int position = parseAttribute(reader, ATTR_POSITION, Integer::parseInt, 0);
        int minOccurs = parseAttribute(reader, ATTR_MIN_OCCURS, Integer::parseInt, 0);
        int maxOccurs = parseAttribute(reader, ATTR_MAX_OCCURS, Integer::parseInt, 0);

        //TODO: Read title attribute and description element
        int event = reader.nextTag();

        if (event == XMLStreamConstants.START_ELEMENT) {
            readSequence(reader, e -> readCompositeSequenceEntry(e, sequence));
        } else if (event == XMLStreamConstants.END_ELEMENT) {
            return newCompositeImpl(reader, minOccurs, maxOccurs, position, sequence);
        } else {
            throw unexpectedEvent(reader);
        }

        if (reader.nextTag() == XMLStreamConstants.END_ELEMENT) {
            return newCompositeImpl(reader, minOccurs, maxOccurs, position, sequence);
        } else {
            throw unexpectedEvent(reader);
        }
    }

    void readCompositeSequenceEntry(QName entryName, List<EDITypeImplementation> sequence) {
        try {
            if (entryName.equals(QN_ELEMENT)) {
                ElementImpl element = readElementImplementation(reader);
                implementedTypes.add(element);
                sequence.add(element.getPosition() - 1, element);
            } else {
                throw unexpectedElement(entryName, reader);
            }
        } catch (XMLStreamException xse) {
            throw schemaException("Exception reading composite sequence", reader, xse);
        }
    }

    CompositeImpl newCompositeImpl(XMLStreamReader reader,
                                   int minOccurs,
                                   int maxOccurs,
                                   int position,
                                   List<EDITypeImplementation> sequence) {

        QName element = reader.getName();

        if (element.equals(QN_COMPOSITE)) {
            return new CompositeImpl(minOccurs, maxOccurs, null, position, sequence);
        } else {
            throw unexpectedElement(element, reader);
        }
    }

    void readSequence(XMLStreamReader reader, Consumer<QName> startHandler) throws XMLStreamException {

        QName element = reader.getName();

        if (element.equals(QN_SEQUENCE)) {
            boolean endOfType = false;

            while (!endOfType && reader.hasNext()) {
                switch (reader.next()) {
                case XMLStreamConstants.START_ELEMENT:
                    startHandler.accept(reader.getName());
                    break;

                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(element)) {
                        endOfType = true;
                    }
                    break;

                default:
                    checkEvent(reader);
                    break;
                }
            }
        } else {
            throw unexpectedElement(element, reader);
        }
    }

    ElementImpl readElementImplementation(XMLStreamReader reader) throws XMLStreamException {
        //TODO: Read title attribute and description element
        Set<String> valueSet = new HashSet<>();
        int position = parseAttribute(reader, ATTR_POSITION, Integer::parseInt, 0);
        int minOccurs = parseAttribute(reader, ATTR_MIN_OCCURS, Integer::parseInt, 0);
        int maxOccurs = parseAttribute(reader, ATTR_MAX_OCCURS, Integer::parseInt, 0);
        int event = reader.nextTag();

        if (event == XMLStreamConstants.START_ELEMENT) {
            QName element = reader.getName();

            if (!element.equals(QN_ENUMERATION)) {
                throw unexpectedElement(element, reader);
            }

            valueSet = super.readEnumerationValues(reader);
        } else if (event == XMLStreamConstants.END_ELEMENT) {
            return newElementImpl(reader, minOccurs, maxOccurs, position, valueSet);
        } else {
            throw unexpectedEvent(reader);
        }

        if (reader.nextTag() == XMLStreamConstants.END_ELEMENT) {
            return newElementImpl(reader, minOccurs, maxOccurs, position, valueSet);
        } else {
            throw unexpectedEvent(reader);
        }
    }

    ElementImpl newElementImpl(XMLStreamReader reader,
                               int minOccurs,
                               int maxOccurs,
                               int position,
                               Set<String> valueSet) {

        QName element = reader.getName();

        if (element.equals(QN_ELEMENT)) {
            return new ElementImpl(minOccurs, maxOccurs, (String) null, position, valueSet);
        } else {
            throw unexpectedElement(element, reader);
        }
    }
}
