package io.xlate.edi.internal.schema;

import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import io.xlate.edi.schema.EDIType;

class SchemaReaderV3 extends SchemaReaderBase implements SchemaReader {

    private static final Logger LOGGER = Logger.getLogger(SchemaReaderV3.class.getName());

    public SchemaReaderV3(XMLStreamReader reader) {
        super(StaEDISchemaFactory.XMLNS_V3, reader);
    }

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
        //TODO: read implementation
    }
}
