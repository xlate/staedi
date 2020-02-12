package io.xlate.edi.internal.schema;

import static io.xlate.edi.internal.schema.StaEDISchemaFactory.schemaException;
import static io.xlate.edi.internal.schema.StaEDISchemaFactory.unexpectedElement;
import static io.xlate.edi.internal.schema.StaEDISchemaFactory.unexpectedEvent;

import java.util.ArrayList;
import java.util.HashSet;
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
import io.xlate.edi.internal.schema.implementation.SegmentImpl;
import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.implementation.CompositeImplementation;
import io.xlate.edi.schema.implementation.EDITypeImplementation;
import io.xlate.edi.schema.implementation.ElementImplementation;
import io.xlate.edi.schema.implementation.LoopImplementation;
import io.xlate.edi.schema.implementation.SegmentImplementation;

class SchemaReaderV3 extends SchemaReaderBase implements SchemaReader {

    private static final Logger LOGGER = Logger.getLogger(SchemaReaderV3.class.getName());
    final QName qnImplementation;

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
        QName element = reader.getName();
        types.put(QN_TRANSACTION.toString(), readComplexType(reader, element, types));

        reader.nextTag();
        element = reader.getName();

        LoopImplementation impl = readImplementation(reader, element, types);
        if (impl != null) {
            types.put(qnImplementation.toString(), impl);
        }
    }

    LoopImplementation readImplementation(XMLStreamReader reader,
                                          QName complexType,
                                          Map<String, EDIType> types) throws XMLStreamException {

        if (!qnImplementation.equals(complexType)) {
            return null;
        }

        LoopImplementation loop = readLoopImplementation(reader, complexType, types, true);
        EDIComplexType standard = (EDIComplexType) types.get(QN_TRANSACTION.toString());
        return new LoopImpl(standard, 0, 0, qnImplementation.toString(), null, loop.getSequence());
    }

    LoopImplementation readLoopImplementation(XMLStreamReader reader,
                                              QName complexType,
                                              Map<String, EDIType> types,
                                              boolean transactionLoop) throws XMLStreamException {

        List<EDITypeImplementation<EDIComplexType>> sequence = new ArrayList<>();
        EDIComplexType standard;
        String id;
        int minOccurs = 0;
        int maxOccurs = 0;

        if (transactionLoop) {
            id = qnImplementation.toString();
            standard = (EDIComplexType) types.get(id);
        } else {
            id = parseAttribute(reader, "code", String::valueOf);
            standard = (EDIComplexType) types.get(parseAttribute(reader, "type", String::valueOf));
            minOccurs = parseAttribute(reader, "minOccurs", Integer::parseInt, 0);
            maxOccurs = parseAttribute(reader, "maxOccurs", Integer::parseInt, 0);
        }

        //TODO: Read title attribute and description element
        //TODO: Read discriminator

        if (reader.nextTag() == XMLStreamConstants.START_ELEMENT) {
            readSequence(reader, e -> {
                try {
                    if (e.equals(QN_LOOP)) {
                        sequence.add(readLoopImplementation(reader, e, types, false));
                    } else if (e.equals(QN_SEGMENT)) {
                        sequence.add(readSegmentImplementation(reader, e, types));
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
                return new LoopImpl(standard, minOccurs, maxOccurs, id, null, sequence);
            } else {
                throw unexpectedElement(element, reader);
            }
        } else {
            throw unexpectedEvent(reader);
        }
    }

    SegmentImplementation readSegmentImplementation(XMLStreamReader reader,
                                                    QName complexType,
                                                    Map<String, EDIType> types) throws XMLStreamException {

        List<EDITypeImplementation<? extends EDIType>> sequence = new ArrayList<>();
        String id = parseAttribute(reader, "type", String::valueOf);
        EDIComplexType standard = (EDIComplexType) types.get(id);
        int minOccurs = parseAttribute(reader, "minOccurs", Integer::parseInt, 0);
        int maxOccurs = parseAttribute(reader, "maxOccurs", Integer::parseInt, 0);

        //TODO: Read title attribute and description element
        //TODO: Read discriminator

        if (reader.nextTag() == XMLStreamConstants.START_ELEMENT) {
            readSequence(reader, e -> {
                try {
                    if (e.equals(QN_ELEMENT)) {
                        sequence.add(readElementImplementation(reader, e, types));
                    } else if (e.equals(QN_COMPOSITE)) {
                        sequence.add(readCompositeImplementation(reader, e, types));
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
                return new SegmentImpl(standard, minOccurs, maxOccurs, id, null, sequence);
            } else {
                throw unexpectedElement(element, reader);
            }
        } else {
            throw unexpectedEvent(reader);
        }
    }

    CompositeImplementation readCompositeImplementation(XMLStreamReader reader,
                                                        QName complexType,
                                                        Map<String, EDIType> types) throws XMLStreamException {

        List<EDITypeImplementation<EDISimpleType>> sequence = new ArrayList<>();
        String id = parseAttribute(reader, "type", String::valueOf);
        EDIComplexType standard = (EDIComplexType) types.get(id);
        int minOccurs = parseAttribute(reader, "minOccurs", Integer::parseInt, 0);
        int maxOccurs = parseAttribute(reader, "maxOccurs", Integer::parseInt, 0);

        //TODO: Read position
        //TODO: Get standard from parent based on position
        //TODO: Read title attribute and description element

        if (reader.nextTag() == XMLStreamConstants.START_ELEMENT) {
            readSequence(reader, e -> {
                try {
                    if (e.equals(QN_ELEMENT)) {
                        sequence.add(readElementImplementation(reader, complexType, types));
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

        if (reader.nextTag() == XMLStreamConstants.START_ELEMENT) {
            QName element = reader.getName();

            if (element.equals(complexType)) {
                return new CompositeImpl(standard, minOccurs, maxOccurs, id, sequence);
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

    ElementImplementation readElementImplementation(XMLStreamReader reader,
                                                    QName complexType,
                                                    Map<String, EDIType> types) throws XMLStreamException {

        //TODO: Read position
        //TODO: Get standard from parent based on position
        //TODO: Read the value enumeration (may be used for discriminator of parent types)
        //TODO: Read title attribute and description element

        Set<String> valueSet = new HashSet<>();
        EDISimpleType standard = (EDISimpleType) types.get(null);
        int minOccurs = parseAttribute(reader, "minOccurs", Integer::parseInt, 0);
        int maxOccurs = parseAttribute(reader, "maxOccurs", Integer::parseInt, 0);

        if (reader.nextTag() == XMLStreamConstants.START_ELEMENT) {

        } else {
            throw unexpectedEvent(reader);
        }

        if (reader.nextTag() == XMLStreamConstants.END_ELEMENT) {
            QName element = reader.getName();

            if (element.equals(complexType)) {
                return new ElementImpl(standard, minOccurs, maxOccurs, (String) null, valueSet);
            } else {
                throw unexpectedElement(element, reader);
            }
        } else {
            throw unexpectedEvent(reader);
        }
    }
}
