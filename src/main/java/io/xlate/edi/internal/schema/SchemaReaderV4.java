package io.xlate.edi.internal.schema;

import static io.xlate.edi.internal.schema.StaEDISchemaFactory.schemaException;

import java.net.URL;
import java.util.Map;

import javax.xml.stream.XMLStreamReader;

import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.EDIType;

public class SchemaReaderV4 extends SchemaReaderV3 {

    public SchemaReaderV4(XMLStreamReader reader) {
        super(StaEDISchemaFactory.XMLNS_V4, reader);
    }

    @Override
    protected String readReferencedId(XMLStreamReader reader) {
        return reader.getAttributeValue(null, "type");
    }

    @Override
    protected void readInclude(XMLStreamReader reader, Map<String, EDIType> types) throws EDISchemaException {
        String location = parseAttribute(reader, "schemaLocation", String::valueOf);

        try {
            types.putAll(StaEDISchemaFactory.readSchemaTypes(new URL(location)));
            reader.nextTag(); // End of include
        } catch (Exception e) {
            throw schemaException("Exception reading included schema", reader, e);
        }
    }
}
