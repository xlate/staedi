package io.xlate.edi.internal.stream.json;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.stream.JsonGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;

import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIValidationException;

class StaEDIJsonParserTest {

    Map<String, Object> ediReaderConfig;
    EDIInputFactory factory;
    EDIStreamReader ediReader;

    @BeforeEach
    void setup() throws Exception {
        ediReaderConfig = new HashMap<>();
        factory = EDIInputFactory.newFactory();
    }

    void setupReader(EDIInputFactory factory, String ediResource, String schemaResource) throws Exception {
        InputStream stream = getClass().getResourceAsStream(ediResource);
        ediReaderConfig.forEach(factory::setProperty);
        ediReader = factory.createEDIStreamReader(stream);

        if (schemaResource != null) {
            SchemaFactory schemaFactory = SchemaFactory.newFactory();
            Schema transactionSchema = schemaFactory.createSchema(getClass().getResource(schemaResource));
            ediReader = factory.createFilteredReader(ediReader, (reader) -> {
                if (reader.getEventType() == EDIStreamEvent.START_TRANSACTION) {
                    reader.setTransactionSchema(transactionSchema);
                }
                return true;
            });
        }
    }

    @SuppressWarnings("unchecked")
    <T> T invoke(Object instance, String methodName, Class<T> returnType) throws Throwable {
        try {
            Method method = instance.getClass().getMethod(methodName);
            return (T) method.invoke(instance);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void copyParserToGenerator(EDIStreamReader ediReader, Object jsonParser, OutputStream buffer) throws Throwable {
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
    @CsvSource({
        "jakarta.json.stream.JsonParser, false, /x12/sample837-original.json",
        "jakarta.json.stream.JsonParser, true,  /x12/sample837-original-nestHL.json",
        "javax.json.stream.JsonParser,   false, /x12/sample837-original.json",
        "javax.json.stream.JsonParser,   true,  /x12/sample837-original-nestHL.json"
    })
    void testNullElementsAsArray(Class<?> parserInterface, boolean nestHL, String expectedResource) throws Throwable {
        ediReaderConfig.put(EDIInputFactory.EDI_NEST_HIERARCHICAL_LOOPS, nestHL);
        ediReaderConfig.put(EDIInputFactory.JSON_NULL_EMPTY_ELEMENTS, true);
        setupReader(factory, "/x12/sample837-original.edi", "/x12/005010/837.xml");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Object jsonParser = factory.createJsonParser(ediReader, parserInterface);
        copyParserToGenerator(ediReader, jsonParser, buffer);
        List<String> expected = Files.readAllLines(Paths.get(getClass().getResource(expectedResource).toURI()));
        System.out.println(buffer.toString());
        JSONAssert.assertEquals(String.join("", expected), buffer.toString(), true);
    }

    @ParameterizedTest
    @CsvSource({
        "jakarta.json.stream.JsonParser, false, /x12/sample837-original-object-elements.json",
        "jakarta.json.stream.JsonParser, true,  /x12/sample837-original-object-elements-nestHL.json",
        "javax.json.stream.JsonParser,   false, /x12/sample837-original-object-elements.json",
        "javax.json.stream.JsonParser,   true,  /x12/sample837-original-object-elements-nestHL.json"
    })
    void testElementsAsObjects(Class<?> parserInterface, boolean nestHL, String expectedResource) throws Throwable {
        ediReaderConfig.put(EDIInputFactory.EDI_NEST_HIERARCHICAL_LOOPS, nestHL);
        ediReaderConfig.put(EDIInputFactory.JSON_OBJECT_ELEMENTS, true);
        setupReader(factory, "/x12/sample837-original.edi", "/x12/005010/837.xml");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Object jsonParser = JsonParserFactory.createJsonParser(ediReader, parserInterface, ediReaderConfig);
        copyParserToGenerator(ediReader, jsonParser, buffer);
        List<String> expected = Files.readAllLines(Paths.get(getClass().getResource(expectedResource).toURI()));
        System.out.println(buffer.toString());
        JSONAssert.assertEquals(String.join("", expected), buffer.toString(), true);
    }

    @ParameterizedTest
    @ValueSource(
            classes = {
                        jakarta.json.stream.JsonParser.class,
                        javax.json.stream.JsonParser.class
            })
    void testNumbersAllComparable(Class<?> parserInterface) throws Throwable {
        setupReader(factory, "/x12/simple810.edi", "/x12/EDISchema810.xml");
        Object jsonParser = JsonParserFactory.createJsonParser(ediReader, parserInterface, ediReaderConfig);

        while (invoke(jsonParser, "hasNext", Boolean.class)) {
            String eventName = invoke(jsonParser, "next", Object.class).toString();

            switch (eventName) {
            case "VALUE_NUMBER":
                int intValue = invoke(jsonParser, "getInt", Integer.class);
                long longValue = invoke(jsonParser, "getLong", Long.class);
                BigDecimal decimalValue = invoke(jsonParser, "getBigDecimal", BigDecimal.class);

                assertEquals(longValue, intValue);

                if (invoke(jsonParser, "isIntegralNumber", Boolean.class)) {
                    assertEquals(0, decimalValue.compareTo(BigDecimal.valueOf(intValue)), decimalValue + " failed comparison with " + intValue);
                    assertEquals(0, decimalValue.compareTo(BigDecimal.valueOf(longValue)), decimalValue + " failed comparison with " + longValue);
                } else {
                    assertEquals(decimalValue.longValue(), intValue, decimalValue + " failed comparison with " + intValue);
                    assertEquals(decimalValue.longValue(), longValue, decimalValue + " failed comparison with " + longValue);
                }

                break;
            default:
                break;
            }
        }

        invoke(jsonParser, "close", Void.class);
    }

    @ParameterizedTest
    @ValueSource(
            classes = {
                        jakarta.json.stream.JsonParser.class,
                        javax.json.stream.JsonParser.class
            })
    void testTextStatesNotIllegal(Class<?> parserInterface) throws Throwable {
        setupReader(factory, "/x12/sample837-original.edi", "/x12/005010/837.xml");
        Object jsonParser = JsonParserFactory.createJsonParser(ediReader, parserInterface, ediReaderConfig);

        while (invoke(jsonParser, "hasNext", Boolean.class)) {
            String eventName = invoke(jsonParser, "next", Object.class).toString();

            switch (eventName) {
            case "KEY_NAME":
            case "VALUE_STRING":
            case "VALUE_NUMBER":
                assertDoesNotThrow(() -> invoke(jsonParser, "getString", String.class));
                break;
            default:
                assertThrows(IllegalStateException.class, () -> invoke(jsonParser, "getString", String.class));
                break;
            }
        }

        invoke(jsonParser, "close", Void.class);
    }

    @ParameterizedTest
    @ValueSource(
            classes = {
                        jakarta.json.stream.JsonParser.class,
                        javax.json.stream.JsonParser.class
            })
    void testTextStatesIllegalForNumber(Class<?> parserInterface) throws Throwable {
        setupReader(factory, "/x12/sample837-original.edi", "/x12/005010/837.xml");
        Object jsonParser = JsonParserFactory.createJsonParser(ediReader, parserInterface, ediReaderConfig);

        while (invoke(jsonParser, "hasNext", Boolean.class)) {
            String eventName = invoke(jsonParser, "next", Object.class).toString();

            switch (eventName) {
            case "VALUE_NUMBER":
                assertDoesNotThrow(() -> invoke(jsonParser, "getInt", Integer.class));
                break;
            default:
                assertThrows(IllegalStateException.class, () -> invoke(jsonParser, "getInt", Integer.class));
                break;
            }
        }

        invoke(jsonParser, "close", Void.class);
    }

    @ParameterizedTest
    @ValueSource(
            classes = {
                        jakarta.json.stream.JsonParser.class,
                        javax.json.stream.JsonParser.class
            })
    void testInvalidNumberThrowsException(Class<?> parserInterface) throws Throwable {
        setupReader(factory, "/x12/invalid997_min.edi", null);
        Object jsonParser = JsonParserFactory.createJsonParser(ediReader, parserInterface, ediReaderConfig);
        List<Exception> errors = new ArrayList<>();

        while (invoke(jsonParser, "hasNext", Boolean.class)) {
            try {
                invoke(jsonParser, "next", Object.class).toString();
            } catch (Exception e) {
                errors.add(e);
            }
        }

        invoke(jsonParser, "close", Void.class);
        assertEquals(4, errors.size());
        // Invalid GS time, invalid GS date, GE02 too long, GE02 invalid chars
        // GS date reported after time due to version of GS date element selected
        EDIValidationException cause = (EDIValidationException) errors.get(3).getCause();
        assertEquals("GE", cause.getLocation().getSegmentTag());
        assertEquals(2, cause.getLocation().getElementPosition());
    }

    @ParameterizedTest
    @CsvSource({
        "jakarta.json.stream.JsonParser,jakarta.json.JsonException",
        "javax.json.stream.JsonParser,javax.json.JsonException"
    })
    void testIOExceptionThrowsCorrectJsonException(String parserName, String exceptionName) throws Throwable {
        InputStream stream = getClass().getResourceAsStream("/x12/sample837-original.edi");
        ediReaderConfig.forEach(factory::setProperty);
        ediReader = factory.createEDIStreamReader(new FilterInputStream(stream) {
            @Override
            public int read() throws IOException {
                if (ediReader.getLocation().getCharacterOffset() > 50) {
                    throw new IOException("Fatal stream error");
                }
                return super.read();
            }
        });

        Class<?> parserInterface = Class.forName(parserName);
        Class<?> exceptionType = Class.forName(exceptionName);

        Object jsonParser = JsonParserFactory.createJsonParser(ediReader, parserInterface, ediReaderConfig);
        List<Exception> errors = new ArrayList<>();

        while (invoke(jsonParser, "hasNext", Boolean.class)) {
            try {
                invoke(jsonParser, "next", Object.class).toString();
            } catch (Exception e) {
                errors.add(e);
                break;
            }
        }

        assertEquals(1, errors.size());
        assertEquals(exceptionType, errors.get(0).getClass());
    }
}
