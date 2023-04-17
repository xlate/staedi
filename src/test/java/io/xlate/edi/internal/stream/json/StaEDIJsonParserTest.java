package io.xlate.edi.internal.stream.json;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;

import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIValidationException;
import io.xlate.edi.test.StaEDIReaderTestBase;

class StaEDIJsonParserTest extends StaEDIReaderTestBase implements JsonParserInvoker {

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
            case "FIELD_NAME": // Jackson only
                assertTrue(invoke(jsonParser, "hasTextCharacters", Boolean.class));
                // fall-through
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
            case "VALUE_NUMBER_INT": // Jackson only
                assertFalse(invoke(jsonParser, "hasTextCharacters", Boolean.class));
                jsonGenerator.write(invoke(jsonParser, "getLongValue", Long.class));
                break;
            case "VALUE_NUMBER_FLOAT": // Jackson only
                assertFalse(invoke(jsonParser, "hasTextCharacters", Boolean.class));
                jsonGenerator.write(invoke(jsonParser, "getDecimalValue", BigDecimal.class));
                break;
            case "VALUE_STRING":
                jsonGenerator.write(invoke(jsonParser, "getString", String.class));
                break;
            default:
                fail("Unexpected event type: " + eventName);
            }

            jsonGenerator.flush();
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
        "jakarta.json.stream.JsonParser,        /x12/005010/837.xml,                            false, /x12/sample837-original.json",
        "jakarta.json.stream.JsonParser,        /x12/005010/837.xml,                            true,  /x12/sample837-original.json",
        "jakarta.json.stream.JsonParser,        /x12/005010/837-hierarchical-level-enabled.xml, true,  /x12/sample837-original-nestHL.json",
        "jakarta.json.stream.JsonParser,        /x12/005010/837-hierarchical-level-enabled.xml, false, /x12/sample837-original.json",

        "javax.json.stream.JsonParser,          /x12/005010/837.xml,                            false, /x12/sample837-original.json",
        "javax.json.stream.JsonParser,          /x12/005010/837.xml,                            true,  /x12/sample837-original.json",
        "javax.json.stream.JsonParser,          /x12/005010/837-hierarchical-level-enabled.xml, true,  /x12/sample837-original-nestHL.json",
        "javax.json.stream.JsonParser,          /x12/005010/837-hierarchical-level-enabled.xml, false, /x12/sample837-original.json",

        "com.fasterxml.jackson.core.JsonParser, /x12/005010/837.xml,                            false, /x12/sample837-original.json",
        "com.fasterxml.jackson.core.JsonParser, /x12/005010/837.xml,                            true,  /x12/sample837-original.json",
        "com.fasterxml.jackson.core.JsonParser, /x12/005010/837-hierarchical-level-enabled.xml, true,  /x12/sample837-original-nestHL.json",
        "com.fasterxml.jackson.core.JsonParser, /x12/005010/837-hierarchical-level-enabled.xml, false, /x12/sample837-original.json"
    })
    void testNullElementsAsArray(Class<?> parserInterface, String schemaPath, boolean nestHL, String expectedResource) throws Throwable {
        ediReaderConfig.put(EDIInputFactory.JSON_NULL_EMPTY_ELEMENTS, true);
        ediReaderConfig.put(EDIInputFactory.EDI_NEST_HIERARCHICAL_LOOPS, nestHL);
        setupReader("/x12/sample837-original.edi", schemaPath);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Object jsonParser = ediInputFactory.createJsonParser(ediReader, parserInterface);
        copyParserToGenerator(ediReader, jsonParser, buffer);
        List<String> expected = Files.readAllLines(Paths.get(getClass().getResource(expectedResource).toURI()));
        System.out.println(buffer.toString());
        JSONAssert.assertEquals(String.join("", expected), buffer.toString(), true);
    }

    @ParameterizedTest
    @CsvSource({
        "jakarta.json.stream.JsonParser,        /x12/005010/837.xml,                            false, /x12/sample837-original-object-elements.json",
        "jakarta.json.stream.JsonParser,        /x12/005010/837.xml,                            true,  /x12/sample837-original-object-elements.json",
        "jakarta.json.stream.JsonParser,        /x12/005010/837-hierarchical-level-enabled.xml, true,  /x12/sample837-original-object-elements-nestHL.json",
        "jakarta.json.stream.JsonParser,        /x12/005010/837-hierarchical-level-enabled.xml, false, /x12/sample837-original-object-elements.json",

        "javax.json.stream.JsonParser,          /x12/005010/837.xml,                            false, /x12/sample837-original-object-elements.json",
        "javax.json.stream.JsonParser,          /x12/005010/837.xml,                            true,  /x12/sample837-original-object-elements.json",
        "javax.json.stream.JsonParser,          /x12/005010/837-hierarchical-level-enabled.xml, true,  /x12/sample837-original-object-elements-nestHL.json",
        "javax.json.stream.JsonParser,          /x12/005010/837-hierarchical-level-enabled.xml, false, /x12/sample837-original-object-elements.json",

        "com.fasterxml.jackson.core.JsonParser, /x12/005010/837.xml,                            false, /x12/sample837-original-object-elements.json",
        "com.fasterxml.jackson.core.JsonParser, /x12/005010/837.xml,                            true,  /x12/sample837-original-object-elements.json",
        "com.fasterxml.jackson.core.JsonParser, /x12/005010/837-hierarchical-level-enabled.xml, true,  /x12/sample837-original-object-elements-nestHL.json",
        "com.fasterxml.jackson.core.JsonParser, /x12/005010/837-hierarchical-level-enabled.xml, false, /x12/sample837-original-object-elements.json"
    })
    void testElementsAsObjects(Class<?> parserInterface, String schemaPath, boolean nestHL, String expectedResource) throws Throwable {
        ediReaderConfig.put(EDIInputFactory.JSON_OBJECT_ELEMENTS, true);
        ediReaderConfig.put(EDIInputFactory.EDI_NEST_HIERARCHICAL_LOOPS, nestHL);
        setupReader("/x12/sample837-original.edi", schemaPath);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Object jsonParser = JsonParserFactory.createJsonParser(ediReader, parserInterface, ediReaderConfig);
        copyParserToGenerator(ediReader, jsonParser, buffer);
        List<String> expected = Files.readAllLines(Paths.get(getClass().getResource(expectedResource).toURI()));
        System.out.println(buffer.toString());
        JSONAssert.assertEquals(String.join("", expected), buffer.toString(), true);
    }

    @ParameterizedTest
    @CsvSource({
        "jakarta.json.stream.JsonParser, jakarta.json.JsonObject, /x12/005010/837.xml,                            false, true,  /x12/sample837-original-object-elements.json",
        "jakarta.json.stream.JsonParser, jakarta.json.JsonObject, /x12/005010/837.xml,                            true,  true,  /x12/sample837-original-object-elements.json",
        "jakarta.json.stream.JsonParser, jakarta.json.JsonObject, /x12/005010/837-hierarchical-level-enabled.xml, true,  true,  /x12/sample837-original-object-elements-nestHL.json",
        "jakarta.json.stream.JsonParser, jakarta.json.JsonObject, /x12/005010/837-hierarchical-level-enabled.xml, false, true,  /x12/sample837-original-object-elements.json",
        "jakarta.json.stream.JsonParser, jakarta.json.JsonObject, /x12/005010/837-hierarchical-level-enabled.xml, true,  false, /x12/sample837-original-nestHL.json",
        "jakarta.json.stream.JsonParser, jakarta.json.JsonObject, /x12/005010/837-hierarchical-level-enabled.xml, false, false, /x12/sample837-original.json",

        "javax.json.stream.JsonParser,   javax.json.JsonObject,   /x12/005010/837.xml,                            false, true,  /x12/sample837-original-object-elements.json",
        "javax.json.stream.JsonParser,   javax.json.JsonObject,   /x12/005010/837.xml,                            true,  true,  /x12/sample837-original-object-elements.json",
        "javax.json.stream.JsonParser,   javax.json.JsonObject,   /x12/005010/837-hierarchical-level-enabled.xml, true,  true,  /x12/sample837-original-object-elements-nestHL.json",
        "javax.json.stream.JsonParser,   javax.json.JsonObject,   /x12/005010/837-hierarchical-level-enabled.xml, false, true,  /x12/sample837-original-object-elements.json",
        "javax.json.stream.JsonParser,   javax.json.JsonObject,   /x12/005010/837-hierarchical-level-enabled.xml, true,  false, /x12/sample837-original-nestHL.json",
        "javax.json.stream.JsonParser,   javax.json.JsonObject,   /x12/005010/837-hierarchical-level-enabled.xml, false, false, /x12/sample837-original.json"
    })
    <P, O> void testInputAsJsonObject(Class<P> parserInterface, Class<O> resultInterface, String schemaPath, boolean nestHL, boolean objectElements, String expectedResource) throws Throwable {
        ediReaderConfig.put(EDIInputFactory.EDI_NEST_HIERARCHICAL_LOOPS, nestHL);
        ediReaderConfig.put(EDIInputFactory.JSON_OBJECT_ELEMENTS, objectElements);
        ediReaderConfig.put(EDIInputFactory.JSON_NULL_EMPTY_ELEMENTS, !objectElements);

        setupReader("/x12/sample837-original.edi", schemaPath);
        P jsonParser = JsonParserFactory.createJsonParser(ediReader, parserInterface, ediReaderConfig);

        String eventName = invoke(jsonParser, "next", Object.class).toString();
        assertEquals("START_OBJECT", eventName);

        O result = invoke(jsonParser, "getObject", resultInterface);

        JsonParser parser = Json.createParser(getClass().getResourceAsStream(expectedResource));
        parser.next();

        assertEquals(parser.getObject().toString(), result.toString());
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> invoke(jsonParser, "getValue", resultInterface));
        assertEquals("getValue illegal when at current position", thrown.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
        "jakarta.json.stream.JsonParser, getArray,  getArray illegal when not at start of array",
        "jakarta.json.stream.JsonParser, getObject, getObject illegal when not at start of object",
        "jakarta.json.stream.JsonParser, getValue,  getValue illegal when data stream has not yet been read",
        "javax.json.stream.JsonParser,   getArray,  getArray illegal when not at start of array",
        "javax.json.stream.JsonParser,   getObject, getObject illegal when not at start of object",
        "javax.json.stream.JsonParser,   getValue,  getValue illegal when data stream has not yet been read"
    })
    void testGetStructureIllegalAtStart(Class<?> parserInterface, String structureMethod, String message) throws Throwable {
        setupReader("/x12/simple810.edi", "/x12/EDISchema810.xml");
        Object jsonParser = JsonParserFactory.createJsonParser(ediReader, parserInterface, ediReaderConfig);
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> invoke(jsonParser, structureMethod, Object.class));
        assertEquals(message, thrown.getMessage());
    }

    @ParameterizedTest
    @ValueSource(
            classes = {
                        jakarta.json.stream.JsonParser.class,
                        javax.json.stream.JsonParser.class
            })
    void testNumbersAllComparable(Class<?> parserInterface) throws Throwable {
        setupReader("/x12/simple810.edi", "/x12/EDISchema810.xml");
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
                        javax.json.stream.JsonParser.class,
                        com.fasterxml.jackson.core.JsonParser.class
            })
    void testTextStatesNotIllegal(Class<?> parserInterface) throws Throwable {
        setupReader("/x12/sample837-original.edi", "/x12/005010/837.xml");
        Object jsonParser = JsonParserFactory.createJsonParser(ediReader, parserInterface, ediReaderConfig);

        while (invoke(jsonParser, "hasNext", Boolean.class)) {
            String eventName = invoke(jsonParser, "next", Object.class).toString();

            switch (eventName) {
            case "KEY_NAME":
            case "FIELD_NAME": // Jackson only
            case "VALUE_STRING":
            case "VALUE_NUMBER":
            case "VALUE_NUMBER_INT": // Jackson only
            case "VALUE_NUMBER_FLOAT": // Jackson only
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
                        javax.json.stream.JsonParser.class,
                        com.fasterxml.jackson.core.JsonParser.class
            })
    void testTextStatesIllegalForNumber(Class<?> parserInterface) throws Throwable {
        setupReader("/x12/sample837-original.edi", "/x12/005010/837.xml");
        Object jsonParser = JsonParserFactory.createJsonParser(ediReader, parserInterface, ediReaderConfig);

        while (invoke(jsonParser, "hasNext", Boolean.class)) {
            String eventName = invoke(jsonParser, "next", Object.class).toString();

            switch (eventName) {
            case "VALUE_NUMBER":
            case "VALUE_NUMBER_INT": // Jackson only
            case "VALUE_NUMBER_FLOAT": // Jackson only
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
                        javax.json.stream.JsonParser.class,
                        com.fasterxml.jackson.core.JsonParser.class
            })
    void testInvalidNumberThrowsException(Class<?> parserInterface) throws Throwable {
        setupReader("/x12/invalid997_min.edi", null);
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
        assertEquals(5, errors.size());
        /*
         * 1 Invalid GS time
         * 2 invalid GS date
         * 3 GE02 too long
         * 4 GE02 invalid chars
         * 5 GE02 does not match GS06
         */
        // GS date reported after time due to version of GS date element selected
        EDIValidationException cause = (EDIValidationException) errors.get(3).getCause();
        assertEquals("GE", cause.getLocation().getSegmentTag());
        assertEquals(2, cause.getLocation().getElementPosition());
    }

    @ParameterizedTest
    @CsvSource({
        "jakarta.json.stream.JsonParser       , jakarta.json.JsonException",
        "javax.json.stream.JsonParser         , javax.json.JsonException",
        "com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.core.JsonParseException"
    })
    void testIOExceptionThrowsCorrectJsonException(String parserName, String exceptionName) throws Throwable {
        InputStream stream = getClass().getResourceAsStream("/x12/sample837-original.edi");
        ediReaderConfig.forEach(ediInputFactory::setProperty);
        ediReader = ediInputFactory.createEDIStreamReader(new FilterInputStream(stream) {
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

    @ParameterizedTest
    @ValueSource(
            classes = {
                        jakarta.json.stream.JsonParser.class,
                        javax.json.stream.JsonParser.class,
                        com.fasterxml.jackson.core.JsonParser.class
            })
    <T> void testBinaryDataReadAsEncodedString(Class<T> parserInterface) throws Throwable {
        ediReaderConfig.forEach(ediInputFactory::setProperty);
        setupReader("/x12/simple_with_binary_segment.edi", "/x12/EDISchemaBinarySegment.xml");
        T jsonParser = JsonParserFactory.createJsonParser(ediReader, parserInterface, ediReaderConfig);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        copyParserToGenerator(ediReader, jsonParser, buffer);
        List<String> expected = Files.readAllLines(Paths.get(getClass().getResource("/x12/simple_with_binary_segment.json").toURI()));
        System.out.println(buffer.toString());
        JSONAssert.assertEquals(String.join("", expected), buffer.toString(), true);
    }
}
