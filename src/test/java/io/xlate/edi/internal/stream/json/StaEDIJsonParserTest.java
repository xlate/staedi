package io.xlate.edi.internal.stream.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.stream.JsonGenerator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;

import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamReader;

class StaEDIJsonParserTest {

    @SuppressWarnings("unchecked")
    <T> T invoke(Object instance, String methodName, Class<T> returnType) {
        try {
            Method method = instance.getClass().getMethod(methodName);
            return (T) method.invoke(instance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void copyParserToGenerator(EDIStreamReader ediReader, Object jsonParser, OutputStream buffer) {
        Map<String, Object> jsonConfig = new HashMap<>();
        jsonConfig.put(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE);
        JsonGenerator jsonGenerator = Json.createGeneratorFactory(jsonConfig).createGenerator(buffer);

        while (invoke(jsonParser, "hasNext", Boolean.class)) {
            String eventName = invoke(jsonParser, "next", Object.class).toString();

            switch (eventName) {
            case "END_ARRAY":
                jsonGenerator.writeEnd();
                break;
            case "END_OBJECT":
                jsonGenerator.writeEnd();
                break;
            case "KEY_NAME":
                jsonGenerator.writeKey(invoke(jsonParser, "getString", String.class));
                break;
            case "START_ARRAY":
                jsonGenerator.writeStartArray();
                break;
            case "START_OBJECT":
                jsonGenerator.writeStartObject();
                break;
            case "VALUE_NULL":
                jsonGenerator.writeNull();
                break;
            case "VALUE_NUMBER":
                if (invoke(jsonParser, "isIntegralNumber", Boolean.class)) {
                    jsonGenerator.write(invoke(jsonParser, "getLong", Long.class));
                } else {
                    jsonGenerator.write(invoke(jsonParser, "getBigDecimal", BigDecimal.class));
                }
                break;
            case "VALUE_STRING":
                jsonGenerator.write(invoke(jsonParser, "getString", String.class));
                break;
            default:
                fail("Unexpected event type");
            }

            Object location = invoke(jsonParser, "getLocation", Object.class);
            assertEquals(ediReader.getLocation().getCharacterOffset(), invoke(location, "getStreamOffset", Long.class));
            assertEquals(ediReader.getLocation().getLineNumber(), invoke(location, "getLineNumber", Long.class));
            assertEquals(ediReader.getLocation().getColumnNumber(), invoke(location, "getColumnNumber", Long.class));
        }

        invoke(jsonParser, "close", Void.class);
        jsonGenerator.close();
    }

    @Test
    void testInvalidParserType() {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                                                       () -> factory.createJsonParser(null, Object.class));
        assertEquals("Unsupported JSON parser type: " + Object.class.toString(), thrown.getMessage());
    }

    @ParameterizedTest
    @ValueSource(
            classes = {
                        jakarta.json.stream.JsonParser.class,
                        javax.json.stream.JsonParser.class
            })
    void testNullElementsAsArray(Class<?> parserInterface) throws Exception {
        Map<String, Object> ediReaderConfig = new HashMap<>();
        ediReaderConfig.put(EDIInputFactory.JSON_NULL_EMPTY_ELEMENTS, true);
        //ediReaderConfig.put(EDIInputFactory.JSON_OBJECT_ELEMENTS, false);

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

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Object jsonParser = JsonParserFactory.createJsonParser(ediReader, parserInterface, ediReaderConfig);
        copyParserToGenerator(ediReader, jsonParser, buffer);
        List<String> expected = Files.readAllLines(Paths.get(getClass().getResource("/x12/sample837-original.json").toURI()));
        System.out.println(buffer.toString());
        JSONAssert.assertEquals(String.join("", expected), buffer.toString(), true);
    }

    @ParameterizedTest
    @ValueSource(
            classes = {
                        jakarta.json.stream.JsonParser.class,
                        javax.json.stream.JsonParser.class
            })
    void testElementsAsObjects(Class<?> parserInterface) throws Exception {
        Map<String, Object> ediReaderConfig = new HashMap<>();
        ediReaderConfig.put(EDIInputFactory.JSON_OBJECT_ELEMENTS, true);

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

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Object jsonParser = JsonParserFactory.createJsonParser(ediReader, parserInterface, ediReaderConfig);
        copyParserToGenerator(ediReader, jsonParser, buffer);
        List<String> expected = Files.readAllLines(Paths.get(getClass().getResource("/x12/sample837-original-object-elements.json").toURI()));
        System.out.println(buffer.toString());
        JSONAssert.assertEquals(String.join("", expected), buffer.toString(), true);
    }
}
