package io.xlate.edi.internal.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.stream.JsonGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamReader;

class StaEDIJakartaJsonParserTest {

    EDIStreamReader ediReader;

    @BeforeEach
    void setUp() throws Exception {
    }

    @Test
    void testSimple997WriteAsJson() throws Exception {
        Map<String, Object> ediReaderConfig = new HashMap<>();
        ediReaderConfig.put(EDIInputFactory.JSON_NULL_EMPTY_ELEMENTS, true);
        ediReaderConfig.put(EDIInputFactory.JSON_OBJECT_ELEMENTS, false);

        EDIInputFactory factory = EDIInputFactory.newFactory();
        ediReaderConfig.forEach(factory::setProperty);

        InputStream stream = getClass().getResourceAsStream("/x12/sample837-original.edi");
        EDIStreamReader ediReader = factory.createEDIStreamReader(stream);
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema transactionSchema = schemaFactory.createSchema(getClass().getResource("/x12/005010/837.xml"));
        ediReader = factory.createFilteredReader(ediReader, (reader) -> {
            if (reader.getEventType() == EDIStreamEvent.START_TRANSACTION) {
                reader.setTransactionSchema(transactionSchema);
            }
            return true;
        });

        // Parser is in jakarta namespace, generator is javax
        jakarta.json.stream.JsonParser jsonParser = new StaEDIJakartaJsonParser(ediReader, ediReaderConfig);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Map<String, Object> jsonConfig = new HashMap<>();
        jsonConfig.put(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE);
        JsonGenerator jsonGenerator = Json.createGeneratorFactory(jsonConfig).createGenerator(buffer);

        while (jsonParser.hasNext()) {
            switch (jsonParser.next()) {
            case END_ARRAY:
                jsonGenerator.writeEnd();
                break;
            case END_OBJECT:
                jsonGenerator.writeEnd();
                break;
            case KEY_NAME:
                jsonGenerator.writeKey(jsonParser.getString());
                break;
            case START_ARRAY:
                jsonGenerator.writeStartArray();
                break;
            case START_OBJECT:
                jsonGenerator.writeStartObject();
                break;
            case VALUE_NULL:
                jsonGenerator.writeNull();
                break;
            case VALUE_NUMBER:
                if (jsonParser.isIntegralNumber()) {
                    jsonGenerator.write(jsonParser.getLong());
                } else {
                    jsonGenerator.write(jsonParser.getBigDecimal());
                }
                break;
            case VALUE_STRING:
                jsonGenerator.write(jsonParser.getString());
                break;
            case VALUE_FALSE:
            case VALUE_TRUE:
            default:
                fail("Unexpected event type");
            }

            assertEquals(ediReader.getLocation().getCharacterOffset(), jsonParser.getLocation().getStreamOffset());
            assertEquals(ediReader.getLocation().getLineNumber(), jsonParser.getLocation().getLineNumber());
            assertEquals(ediReader.getLocation().getColumnNumber(), jsonParser.getLocation().getColumnNumber());
        }

        jsonParser.close();
        jsonGenerator.close();
        System.out.println(buffer.toString());
    }

}
