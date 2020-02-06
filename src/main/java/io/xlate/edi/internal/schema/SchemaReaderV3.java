package io.xlate.edi.internal.schema;

import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import io.xlate.edi.schema.EDIType;

class SchemaReaderV3 extends SchemaReaderBase implements SchemaReader {

    public SchemaReaderV3(XMLStreamReader reader) {
        super(StaEDISchemaFactory.XMLNS_V3, reader);
    }

    @Override
    void readTransaction(XMLStreamReader reader, Map<String, EDIType> types) throws XMLStreamException {
        QName element = reader.getName();
        types.put(QN_TRANSACTION.toString(), readComplexType(reader, element, types));
        //TODO: read implementation
    }
}
