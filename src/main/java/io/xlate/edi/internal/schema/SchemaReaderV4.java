package io.xlate.edi.internal.schema;

import static io.xlate.edi.internal.schema.StaEDISchemaFactory.schemaException;

import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamReader;

import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.SchemaFactory;

public class SchemaReaderV4 extends SchemaReaderV3 {

    private static final Logger LOGGER = Logger.getLogger(SchemaReaderV4.class.getName());

    public SchemaReaderV4(XMLStreamReader reader, Map<String, Object> properties) {
        super(StaEDISchemaFactory.XMLNS_V4, reader, properties);
    }

    @Override
    void nameCheck(String name, Map<String, EDIType> types, XMLStreamReader reader) {
        if (types.containsKey(name)) {
            LOGGER.fine(() -> "Duplicate type name encountered: [" + name + ']');
        }
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

            URL schemaLocation;

            if (location.startsWith("classpath:")) {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                if (location.startsWith("classpath:/")) {
                    location = location.substring(0, "classpath:".length()) + location.substring("classpath:/".length());
                }
                schemaLocation = new URL(null, location, new ClasspathURLStreamHandler(loader));
            } else {
                schemaLocation = new URL(context, location);
            }

            types.putAll(StaEDISchemaFactory.readSchemaTypes(schemaLocation, super.properties));
            reader.nextTag(); // End of include
        } catch (Exception e) {
            throw schemaException("Exception reading included schema", reader, e);
        }

        nextTag(reader, "seeking next element after include");
    }
}
