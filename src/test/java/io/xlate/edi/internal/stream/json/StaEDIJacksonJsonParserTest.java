package io.xlate.edi.internal.stream.json;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.test.StaEDIReaderTestBase;

class StaEDIJacksonJsonParserTest extends StaEDIReaderTestBase implements JsonParserInvoker {

    static byte[] TINY_X12 = ("ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
            + "IEA*1*508121953~").getBytes();

    @Test
    void testJacksonParserVersion() throws Throwable {
        ediReaderConfig.put(EDIInputFactory.JSON_NULL_EMPTY_ELEMENTS, true);
        setupReader(TINY_X12, null);
        JsonParser jsonParser = JsonParserFactory.createJsonParser(ediReader, JsonParser.class, ediReaderConfig);
        Version version = jsonParser.version();
        assertNotNull(version);
        assertFalse(version.isUnknownVersion());
    }

    @Test
    void testReadValueAsTree() throws Throwable {
        ediReaderConfig.put(EDIInputFactory.JSON_NULL_EMPTY_ELEMENTS, true);
        setupReader("/x12/sample837-original.edi", "/x12/005010/837.xml");
        JsonParser jsonParser = JsonParserFactory.createJsonParser(ediReader, JsonParser.class, ediReaderConfig);
        assertFalse(jsonParser.isClosed());
        jsonParser.setCodec(new ObjectMapper());
        JsonNode tree = jsonParser.readValueAsTree();
        assertNotNull(tree);
        assertNull(jsonParser.nextToken());
        jsonParser.close();
        assertTrue(jsonParser.isClosed());

        List<String> expected = Files.readAllLines(Paths.get(getClass().getResource("/x12/sample837-original.json").toURI()));
        JSONAssert.assertEquals(String.join("", expected), tree.toString(), true);
    }

    @ParameterizedTest
    @CsvSource({
        "                    1 , INT        , java.lang.Integer   , getIntValue",
        "                   -1 , INT        , java.lang.Integer   , getIntValue",
        "           9999999999 , LONG       , java.lang.Long      , getLongValue",
        "          -9999999999 , LONG       , java.lang.Long      , getLongValue",
        " 99999999999999999999 , BIG_INTEGER, java.math.BigInteger, getBigIntegerValue",
        "-99999999999999999999 , BIG_INTEGER, java.math.BigInteger, getBigIntegerValue",
    })
    <T> void testNumericTypes(String inputValue, NumberType expectedType, Class<T> expectedClass, String directMethod) throws Throwable {
        ediReaderConfig.put(EDIInputFactory.JSON_NULL_EMPTY_ELEMENTS, true);

        setupReader(("ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "GS*AA*ReceiverDept*SenderDept*20200101*000000*1*X*005010~\n"
                + "ST*000*0001*005010~\n"
                + String.format("INT*%s~", inputValue)
                + "FLT*1~"
                + "SE*4*0001~\n"
                + "GE*1*1~\n"
                + "IEA*1*508121953~").getBytes(), "/x12/EDISchema000-numeric-types.xml");

        JsonParser jsonParser = JsonParserFactory.createJsonParser(ediReader, JsonParser.class, ediReaderConfig);
        JsonToken token;
        boolean matched = false;

        while ((token = jsonParser.nextToken()) != null) {
            String pointer = jsonParser.getParsingContext().pathAsPointer().toString();
            if ("/data/1/data/1/data/1/data/0".equals(pointer)) {
                matched = true;
                assertEquals(JsonToken.VALUE_NUMBER_INT, token);
                assertEquals(expectedType, jsonParser.getNumberType());
                Number actualValue = jsonParser.getNumberValue();
                assertEquals(expectedClass, actualValue.getClass());
                assertEquals(inputValue, String.valueOf(actualValue));
                T directValue = invoke(jsonParser, directMethod, expectedClass);
                assertEquals(actualValue, directValue);
            }
        }

        assertTrue(matched);
    }

    @ParameterizedTest
    @CsvSource({
        "1,                 1,   FLOAT,       java.lang.Float,      getFloatValue",
        "34028234663852886, 38,  DOUBLE,      java.lang.Double,     getDoubleValue",
        "18976931348623157, 308, BIG_DECIMAL, java.math.BigDecimal, getDecimalValue",
    })
    <T> void testDecimalTypesLargeValue(String prefix, int exp, NumberType expectedType, Class<T> expectedClass, String directMethod) throws Throwable {
        ediReaderConfig.put(EDIInputFactory.JSON_NULL_EMPTY_ELEMENTS, true);
        BigDecimal inputValue = new BigDecimal(prefix + String.join("", Collections.nCopies(exp, "0")) + ".1");

        setupReader(("ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "GS*AA*ReceiverDept*SenderDept*20200101*000000*1*X*005010~\n"
                + "ST*000*0001*005010~\n"
                + "INT*1~"
                + String.format("FLT*%s~", inputValue.toPlainString())
                + "SE*4*0001~\n"
                + "GE*1*1~\n"
                + "IEA*1*508121953~").getBytes(), "/x12/EDISchema000-numeric-types.xml");

        JsonParser jsonParser = JsonParserFactory.createJsonParser(ediReader, JsonParser.class, ediReaderConfig);
        JsonToken token;
        boolean matched = false;

        while ((token = jsonParser.nextToken()) != null) {
            String pointer = jsonParser.getParsingContext().pathAsPointer().toString();
            if ("/data/1/data/1/data/2/data/0".equals(pointer)) {
                matched = true;
                assertEquals(JsonToken.VALUE_NUMBER_FLOAT, token);
                assertEquals(expectedType, jsonParser.getNumberType());
                Number actualValue = jsonParser.getNumberValue();
                assertEquals(expectedClass, actualValue.getClass());
                T directValue = invoke(jsonParser, directMethod, expectedClass);
                assertEquals(actualValue, directValue);
            }
        }

        assertTrue(matched);
    }

    @ParameterizedTest
    @CsvSource({
        "1,     FLOAT,       java.lang.Float",
        "128,   DOUBLE,      java.lang.Double",
        "1024,  BIG_DECIMAL, java.math.BigDecimal",
        "-1,    FLOAT,       java.lang.Float",
        "-128,  DOUBLE,      java.lang.Double",
        "-1024, BIG_DECIMAL, java.math.BigDecimal",
    })
    void testDecimalTypesLargeExponent(int exp, NumberType expectedType, Class<?> expectedClass) throws Exception {
        ediReaderConfig.put(EDIInputFactory.JSON_NULL_EMPTY_ELEMENTS, true);
        BigDecimal inputValue;
        if (exp > 0) {
            inputValue = new BigDecimal("1" + String.join("", Collections.nCopies(exp, "0")) + ".1");
        } else {
            inputValue = new BigDecimal("0." + String.join("", Collections.nCopies(Math.abs(exp), "0")) + "1");
        }

        setupReader(("ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "GS*AA*ReceiverDept*SenderDept*20200101*000000*1*X*005010~\n"
                + "ST*000*0001*005010~\n"
                + "INT*1~"
                + String.format("FLT*%s~", inputValue.toPlainString())
                + "SE*4*0001~\n"
                + "GE*1*1~\n"
                + "IEA*1*508121953~").getBytes(), "/x12/EDISchema000-numeric-types.xml");

        JsonParser jsonParser = JsonParserFactory.createJsonParser(ediReader, JsonParser.class, ediReaderConfig);
        JsonToken token;
        boolean matched = false;

        while ((token = jsonParser.nextToken()) != null) {
            String pointer = jsonParser.getParsingContext().pathAsPointer().toString();
            if ("/data/1/data/1/data/2/data".equals(pointer)) {
                // Segment FLT
                assertNull(jsonParser.getNumberType());
                assertThrows(JsonParseException.class, () -> jsonParser.getNumberValue());
            }

            if ("/data/1/data/1/data/2/data/0".equals(pointer)) {
                // Element FLT01
                matched = true;
                assertEquals(JsonToken.VALUE_NUMBER_FLOAT, token);
                assertEquals(expectedType, jsonParser.getNumberType());
                Number actualValue = jsonParser.getNumberValue();
                assertEquals(expectedClass, actualValue.getClass());
            }
        }

        assertTrue(matched);
    }

    @Test
    void testPointersUnique() throws Exception {
        ediReaderConfig.put(EDIInputFactory.JSON_NULL_EMPTY_ELEMENTS, true);

        setupReader(("ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "GS*AA*ReceiverDept*SenderDept*20200101*000000*1*X*005010~\n"
                + "ST*000*0001*005010~\n"
                + "INT*1~"
                + "FLT*1~"
                + "SE*4*0001~\n"
                + "GE*1*1~\n"
                + "IEA*1*508121953~").getBytes(), "/x12/EDISchema000-numeric-types.xml");

        JsonParser jsonParser = JsonParserFactory.createJsonParser(ediReader, JsonParser.class, ediReaderConfig);
        JsonToken token;
        Map<String, List<JsonToken>> pointers = new LinkedHashMap<>();

        while ((token = jsonParser.nextToken()) != null) {
            JsonToken tok = token;
            String pointer = jsonParser.getParsingContext().pathAsPointer().toString();
            List<JsonToken> tokens = pointers.computeIfAbsent(pointer, k -> new ArrayList<>());
            assertFalse(tokens.contains(token), () -> "pointers already contains " + pointer + " for token " + tok);
            assertTrue(tokens.size() <= 2, () -> "pointer " + pointer +" has too many tokens:" + tokens);
            tokens.add(token);
        }

        assertFalse(pointers.isEmpty());
    }

    @Test
    void testTextRetrieval() throws Exception {
        ediReaderConfig.put(EDIInputFactory.JSON_NULL_EMPTY_ELEMENTS, true);

        setupReader(("ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "GS*AA*ReceiverDept*SenderDept*20200101*000000*1*X*005010~\n"
                + "ST*000*0001*005010~\n"
                + "INT*1~"
                + "FLT*1~"
                + "SE*4*0001~\n"
                + "GE*1*1~\n"
                + "IEA*1*508121953~").getBytes(), "/x12/EDISchema000-numeric-types.xml");

        JsonParser jsonParser = JsonParserFactory.createJsonParser(ediReader, JsonParser.class, ediReaderConfig);
        boolean matched = false;

        while (jsonParser.nextToken() != null) {
            String pointer = jsonParser.getParsingContext().pathAsPointer().toString();

            if ("/data/1/data/0/data/0".equals(pointer)) {
                matched = true;
                // Element GS01
                assertEquals("AA", jsonParser.getText());
                assertEquals(2, jsonParser.getTextLength());
                assertArrayEquals(new char[] { 'A', 'A' }, Arrays.copyOfRange(
                    jsonParser.getTextCharacters(),
                    jsonParser.getTextOffset(),
                    jsonParser.getTextLength()));
            }
        }

        assertTrue(matched);
    }

    @Test
    void testBinaryElementRetrieval() throws Exception {
        ediReaderConfig.put(EDIInputFactory.JSON_NULL_EMPTY_ELEMENTS, true);
        setupReader("/x12/simple_with_binary_segment.edi", "/x12/EDISchemaBinarySegment.xml");

        JsonParser jsonParser = JsonParserFactory.createJsonParser(ediReader, JsonParser.class, ediReaderConfig);
        int matched = 0;

        while (jsonParser.nextToken() != null) {
            String pointer = jsonParser.getParsingContext().pathAsPointer().toString();

            if ("/data/1/data/1/data/1/data/1".equals(pointer)) {
                matched++;
                assertArrayEquals("1234567890123456789012345".getBytes(), jsonParser.getBinaryValue());
            }
            if ("/data/1/data/1/data/2/data/1".equals(pointer)) {
                matched++;
                assertArrayEquals("12345678901234567890\n1234".getBytes(), jsonParser.getBinaryValue());
            }
            if ("/data/1/data/1/data/3/data/1".equals(pointer)) {
                matched++;
                assertArrayEquals("1234567890\n1234567890\n12\n".getBytes(), jsonParser.getBinaryValue());
            }
        }

        assertEquals(3, matched);
    }
}
