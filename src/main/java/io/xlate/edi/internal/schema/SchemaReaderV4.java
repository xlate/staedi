package io.xlate.edi.internal.schema;

import static io.xlate.edi.internal.schema.StaEDISchemaFactory.schemaException;

import java.net.URL;
import java.util.Map;

import javax.xml.stream.XMLStreamReader;

import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.SchemaFactory;

public class SchemaReaderV4 extends SchemaReaderV3 {

    public SchemaReaderV4(XMLStreamReader reader, Map<String, Object> properties) {
        super(StaEDISchemaFactory.XMLNS_V4, reader, properties);
    }

    @Override
    protected String readReferencedId(XMLStreamReader reader) {
        return parseAttribute(reader, "type", String::valueOf);
    }

    @Override
    protected void readInclude(XMLStreamReader reader, Map<String, EDIType> types) throws EDISchemaException {
        String location = parseAttribute(reader, "schemaLocation", String::valueOf);
        URL context = null;

        try {
            if (properties.containsKey(SchemaFactory.SCHEMA_LOCATION_URL_CONTEXT)) {
                Object ctx = properties.get(SchemaFactory.SCHEMA_LOCATION_URL_CONTEXT);

                if (ctx instanceof URL) {
                    context = (URL) ctx;
                } else {
                    context = new URL(String.valueOf(ctx));
                }
            }

            URL schemaLocation = context != null ? new URL(context, location) : new URL(location);
            types.putAll(StaEDISchemaFactory.readSchemaTypes(schemaLocation, super.properties));
            reader.nextTag(); // End of include
        } catch (Exception e) {
            throw schemaException("Exception reading included schema", reader, e);
        }
    }
}
