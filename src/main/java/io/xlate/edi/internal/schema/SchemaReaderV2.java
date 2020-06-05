package io.xlate.edi.internal.schema;

import static io.xlate.edi.internal.schema.StaEDISchemaFactory.unexpectedElement;

import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.EDIType;

class SchemaReaderV2 extends SchemaReaderBase implements SchemaReader {

    public SchemaReaderV2(XMLStreamReader reader) {
        super(StaEDISchemaFactory.XMLNS_V2, reader);
    }

    @Override
    public String getImplementationName() {
        return null;
    }

    @Override
    protected void readInclude(XMLStreamReader reader, Map<String, EDIType> types) throws EDISchemaException {
        // Included schema not supported in V2 Schema
        throw unexpectedElement(reader.getName(), reader);
    }

    @Override
    protected void readImplementation(XMLStreamReader reader, Map<String, EDIType> types) throws XMLStreamException {
        // Implementations not supported in V2 Schema
    }

    @Override
    protected String readReferencedId(XMLStreamReader reader) {
        return reader.getAttributeValue(null, "ref");
    }
}
