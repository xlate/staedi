package io.xlate.edi.internal.schema;

import static io.xlate.edi.internal.schema.StaEDISchemaFactory.schemaException;
import static io.xlate.edi.internal.schema.StaEDISchemaFactory.unexpectedElement;
import static io.xlate.edi.internal.schema.StaEDISchemaFactory.unexpectedEvent;

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
import io.xlate.edi.internal.schema.implementation.ElementImpl;
import io.xlate.edi.internal.schema.implementation.LoopImpl;
import io.xlate.edi.internal.schema.implementation.Positioned;
import io.xlate.edi.internal.schema.implementation.SegmentImpl;
import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.implementation.EDITypeImplementation;
import io.xlate.edi.schema.implementation.LoopImplementation;
import io.xlate.edi.schema.implementation.SegmentImplementation;

class SchemaReaderV3 extends SchemaReaderBase implements SchemaReader {

    private static final Logger LOGGER = Logger.getLogger(SchemaReaderV3.class.getName());
    final QName qnImplementation;
    final Deque<EDITypeImplementation<? extends EDIType>> implementedTypes = new LinkedList<>();

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

        implementedTypes.descendingIterator().forEachRemaining(i -> {
            switch (i.getType()) {
            case COMPOSITE: {
                CompositeImpl impl = (CompositeImpl) i;
                EDIComplexType standard = impl.getStandard();
                for (EDITypeImplementation<? extends EDIType> t : impl.getSequence()) {
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
                for (EDITypeImplementation<? extends EDIType> t : impl.getSequence()) {
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
        LoopImpl impl = new LoopImpl(0, 0, qnImplementation.toString(), typeId, null, loop.getSequence()) {
            @Override
            public EDIType.Type getType() {
                return EDIType.Type.TRANSACTION;
            }
        };
        impl.setStandard(standard);
        return impl;
    }

    LoopImplementation readLoopImplementation(XMLStreamReader reader,
                                              QName complexType,
                                              boolean transactionLoop) throws XMLStreamException {

        List<EDITypeImplementation<EDIComplexType>> sequence = new ArrayList<>();
        String id;
        String typeId;
        int minOccurs = 0;
        int maxOccurs = 0;
        @SuppressWarnings("unused")
        String descriminatorAttr = null;

        if (transactionLoop) {
            id = qnImplementation.toString();
            typeId = null;
        } else {
            id = parseAttribute(reader, "code", String::valueOf);
            typeId = parseAttribute(reader, "type", String::valueOf);
            minOccurs = parseAttribute(reader, "minOccurs", Integer::parseInt, 0);
            maxOccurs = parseAttribute(reader, "maxOccurs", Integer::parseInt, 0);
            descriminatorAttr = parseAttribute(reader, "descriminator", String::valueOf, "");
        }

        //TODO: Read title attribute and description element

        if (reader.nextTag() == XMLStreamConstants.START_ELEMENT) {
            readSequence(reader, e -> {
                try {
                    if (e.equals(QN_LOOP)) {
                        sequence.add(readLoopImplementation(reader, e, false));
                    } else if (e.equals(QN_SEGMENT)) {
                        sequence.add(readSegmentImplementation(reader, e));
                    } else {
                        throw unexpectedElement(e, reader);
                    }
                } catch (XMLStreamException xse) {
                    throw schemaException("Exception reading loop sequence", reader, xse);
                }
            });
        } else {
            throw unexpectedEvent(reader);
        }

        if (reader.nextTag() == XMLStreamConstants.END_ELEMENT) {
            QName element = reader.getName();

            if (element.equals(complexType)) {
                //TODO: If discriminatorAttr was found, get values from sequence/element
                LoopImpl loop = new LoopImpl(minOccurs, maxOccurs, id, typeId, null, sequence);
                implementedTypes.add(loop);
                return loop;
            } else {
                throw unexpectedElement(element, reader);
            }
        } else {
            throw unexpectedEvent(reader);
        }
    }

    SegmentImplementation readSegmentImplementation(XMLStreamReader reader,
                                                    QName complexType) throws XMLStreamException {

        List<EDITypeImplementation<? extends EDIType>> sequence = new ArrayList<>();
        String typeId = parseAttribute(reader, "type", String::valueOf);
        int minOccurs = parseAttribute(reader, "minOccurs", Integer::parseInt, 0);
        int maxOccurs = parseAttribute(reader, "maxOccurs", Integer::parseInt, 0);
        @SuppressWarnings("unused")
        String descriminatorAttr = parseAttribute(reader, "descriminator", String::valueOf, "");

        //TODO: Read title attribute and description element
        int event = reader.nextTag();

        if (event == XMLStreamConstants.START_ELEMENT) {
            readSequence(reader, e -> {
                try {
                    if (e.equals(QN_ELEMENT)) {
                        ElementImpl element = readElementImplementation(reader, e);
                        implementedTypes.add(element);
                        sequence.add(element.getPosition() - 1, element);
                    } else if (e.equals(QN_COMPOSITE)) {
                        CompositeImpl element = readCompositeImplementation(reader, e);
                        implementedTypes.add(element);
                        sequence.add(element.getPosition() - 1, element);
                    } else {
                        throw unexpectedElement(e, reader);
                    }
                } catch (XMLStreamException xse) {
                    throw schemaException("Exception reading segment sequence", reader, xse);
                }
            });
        } else if (event == XMLStreamConstants.END_ELEMENT) {
            QName element = reader.getName();

            if (element.equals(complexType)) {
                //TODO: If discriminatorAttr was found, get values from sequence/element
                SegmentImpl segment = new SegmentImpl(minOccurs, maxOccurs, typeId, typeId, null, sequence);
                implementedTypes.add(segment);
                return segment;
            } else {
                throw unexpectedElement(element, reader);
            }
        } else {
            throw unexpectedEvent(reader);
        }

        if (reader.nextTag() == XMLStreamConstants.END_ELEMENT) {
            QName element = reader.getName();

            if (element.equals(complexType)) {
                //TODO: If discriminatorAttr was found, get values from sequence/element
                SegmentImpl segment = new SegmentImpl(minOccurs, maxOccurs, typeId, typeId, null, sequence);
                implementedTypes.add(segment);
                return segment;
            } else {
                throw unexpectedElement(element, reader);
            }
        } else {
            throw unexpectedEvent(reader);
        }
    }

    CompositeImpl readCompositeImplementation(XMLStreamReader reader,
                                              QName complexType) throws XMLStreamException {

        List<EDITypeImplementation<EDISimpleType>> sequence = new ArrayList<>();
        int position = parseAttribute(reader, "position", Integer::parseInt, 0);
        int minOccurs = parseAttribute(reader, "minOccurs", Integer::parseInt, 0);
        int maxOccurs = parseAttribute(reader, "maxOccurs", Integer::parseInt, 0);

        //TODO: Read title attribute and description element
        int event = reader.nextTag();

        if (event == XMLStreamConstants.START_ELEMENT) {
            readSequence(reader, e -> {
                try {
                    if (e.equals(QN_ELEMENT)) {
                        ElementImpl element = readElementImplementation(reader, e);
                        implementedTypes.add(element);
                        sequence.add(element.getPosition() - 1, element);
                    } else {
                        throw unexpectedElement(e, reader);
                    }
                } catch (XMLStreamException xse) {
                    throw schemaException("Exception reading composite sequence", reader, xse);
                }
            });
        } else if (event == XMLStreamConstants.END_ELEMENT) {
            QName element = reader.getName();

            if (element.equals(complexType)) {
                return new CompositeImpl(minOccurs, maxOccurs, null, position, sequence);
            } else {
                throw unexpectedElement(element, reader);
            }
        } else {
            throw unexpectedEvent(reader);
        }

        if (reader.nextTag() == XMLStreamConstants.END_ELEMENT) {
            QName element = reader.getName();

            if (element.equals(complexType)) {
                return new CompositeImpl(minOccurs, maxOccurs, null, position, sequence);
            } else {
                throw unexpectedElement(element, reader);
            }
        } else {
            throw unexpectedEvent(reader);
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

    ElementImpl readElementImplementation(XMLStreamReader reader,
                                          QName complexType) throws XMLStreamException {

        //TODO: Read title attribute and description element

        Set<String> valueSet = new HashSet<>();
        int position = parseAttribute(reader, "position", Integer::parseInt, 0);
        int minOccurs = parseAttribute(reader, "minOccurs", Integer::parseInt, 0);
        int maxOccurs = parseAttribute(reader, "maxOccurs", Integer::parseInt, 0);
        int event = reader.nextTag();

        if (event == XMLStreamConstants.START_ELEMENT) {
            QName element = reader.getName();

            if (!element.equals(QN_ENUMERATION)) {
                throw unexpectedElement(element, reader);
            }

            valueSet = readEnumerationValues(reader);
        } else if (event == XMLStreamConstants.END_ELEMENT) {
            QName element = reader.getName();

            if (element.equals(complexType)) {
                return new ElementImpl(minOccurs, maxOccurs, (String) null, position, valueSet);
            } else {
                throw unexpectedElement(element, reader);
            }
        } else {
            throw unexpectedEvent(reader);
        }

        if (reader.nextTag() == XMLStreamConstants.END_ELEMENT) {
            QName element = reader.getName();

            if (element.equals(complexType)) {
                return new ElementImpl(minOccurs, maxOccurs, (String) null, position, valueSet);
            } else {
                throw unexpectedElement(element, reader);
            }
        } else {
            throw unexpectedEvent(reader);
        }
    }

    EDIType getStandard(XMLStreamReader reader, EDIComplexType standardParent, int position) {
        final List<EDIReference> references = standardParent.getReferences();
        final int offset = position - 1;

        if (offset < references.size()) {
            return references.get(offset).getReferencedType();
        }

        throw schemaException("Position " + position + " does not correspond to an entry in " + standardParent.getId(),
                              reader);
    }
}
