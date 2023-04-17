package io.xlate.edi.internal.stream.json;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.base.ParserMinimalBase;
import com.fasterxml.jackson.core.io.ContentReference;

import io.xlate.edi.internal.stream.json.StaEDIJsonParser.Event;
import io.xlate.edi.stream.EDIStreamReader;

public class StaEDIJacksonJsonParser extends ParserMinimalBase {

    private static final Version VERSION;
    private static final BigDecimal MIN_LONG = BigDecimal.valueOf(Long.MIN_VALUE);
    private static final BigDecimal MAX_LONG = BigDecimal.valueOf(Long.MAX_VALUE);

    private static final BigDecimal MIN_INTEGER = BigDecimal.valueOf(Integer.MIN_VALUE);
    private static final BigDecimal MAX_INTEGER = BigDecimal.valueOf(Integer.MAX_VALUE);

    private static final BigDecimal MIN_DOUBLE = BigDecimal.valueOf(-Double.MAX_VALUE);
    private static final BigDecimal MAX_DOUBLE = BigDecimal.valueOf(Double.MAX_VALUE);

    private static final BigDecimal MIN_FLOAT = BigDecimal.valueOf(-Float.MAX_VALUE);
    private static final BigDecimal MAX_FLOAT = BigDecimal.valueOf(Float.MAX_VALUE);

    private StaEDIJsonParser<JsonParseException> parser;
    private ObjectCodec codec;
    private final Deque<StaEDIJsonStreamContext> context = new ArrayDeque<>();
    private StaEDIJsonStreamContext pendingContext = null;

    StaEDIJacksonJsonParser(EDIStreamReader ediReader, Map<String, Object> properties) {
        parser = new JacksonJsonParser(ediReader, properties);
        context.addLast(new StaEDIJsonStreamContext(JsonStreamContext.TYPE_ROOT, null));
    }

    /* test */ boolean hasNext() throws IOException {
        return parser.hasNext();
    }

    /* test */ JsonToken next() throws IOException {
        return nextToken();
    }

    /* test */ String getString() throws IOException {
        return getText();
    }

    /* test */ int getInt() {
        return getIntValue();
    }

    /* test */ JsonLocation getLocation() {
        return getTokenLocation();
    }

    @Override
    public JsonToken nextToken() throws IOException {
        if (!parser.hasNext()) {
            return null;
        }

        if (pendingContext != null) {
            context.add(pendingContext);
            pendingContext = null;
        }

        final Event next = parser.nextEvent();
        final JsonToken token;

        switch (next) {
        case END_ARRAY:
            context.removeLast();
            token = JsonToken.END_ARRAY;
            break;
        case END_OBJECT:
            context.removeLast();
            token = JsonToken.END_OBJECT;
            break;
        case KEY_NAME:
            context.getLast().incrementIndex();
            context.getLast().setCurrentName(getText());
            token = JsonToken.FIELD_NAME;
            break;
        case START_ARRAY:
            context.getLast().incrementIndexIfArray();
            pendingContext = new StaEDIJsonStreamContext(JsonStreamContext.TYPE_ARRAY, context.getLast());
            token = JsonToken.START_ARRAY;
            break;
        case START_OBJECT:
            context.getLast().incrementIndexIfArray();
            pendingContext = new StaEDIJsonStreamContext(JsonStreamContext.TYPE_OBJECT, context.getLast());
            token = JsonToken.START_OBJECT;
            break;
        case VALUE_NULL:
            context.getLast().incrementIndexIfArray();
            token = JsonToken.VALUE_NULL;
            break;
        case VALUE_NUMBER:
            context.getLast().incrementIndexIfArray();
            if (parser.isIntegralNumber()) {
                token = JsonToken.VALUE_NUMBER_INT;
            } else {
                token = JsonToken.VALUE_NUMBER_FLOAT;
            }
            break;
        case VALUE_STRING:
            context.getLast().incrementIndexIfArray();
            token = JsonToken.VALUE_STRING;
            break;
        default:
            throw new JsonParseException(this, StaEDIJsonParser.MSG_UNEXPECTED + next);
        }

        super._currToken = token;

        return token;
    }

    @Override
    public void close() throws IOException {
        parser.close();
    }

    @Override
    public String getText() throws IOException {
        return parser.getString();
    }

    @Override
    public char[] getTextCharacters() throws IOException {
        return parser.ediReader.getTextCharacters();
    }

    @Override
    public int getTextLength() throws IOException {
        return parser.ediReader.getTextLength();
    }

    @Override
    public int getTextOffset() throws IOException {
        return parser.ediReader.getTextStart();
    }

    @Override
    public BigInteger getBigIntegerValue() throws IOException {
        return parser.getBigDecimal().toBigInteger();
    }

    @Override
    public ObjectCodec getCodec() {
        return this.codec;
    }

    @Override
    public void setCodec(ObjectCodec c) {
        this.codec = c;
    }

    @Override
    public JsonLocation getCurrentLocation() {
        return getTokenLocation();
    }

    @Override
    public JsonLocation getTokenLocation() {
        return new JacksonJsonLocation(parser.getStreamOffset(), parser.getLineNumber(), parser.getColumnNumber());
    }

    @Override
    protected void _handleEOF() throws JsonParseException {
        // Not supported, do nothing
    }

    @Override
    public boolean isClosed() {
        return parser.closed;
    }

    @Override
    public JsonStreamContext getParsingContext() {
        return context.getLast().deepCopy();
    }

    @Override
    public String getCurrentName() throws IOException {
        return context.getLast().getCurrentName();
    }

    @Override
    public void overrideCurrentName(String name) {
        // Not supported, do nothing
    }

    @Override
    public boolean hasTextCharacters() {
        switch (parser.currentEvent) {
        case KEY_NAME:
        case VALUE_STRING:
            return true;
        default:
            return false;
        }
    }

    @Override
    public byte[] getBinaryValue(Base64Variant b64variant) throws IOException {
        return parser.currentBinaryValue.toByteArray();
    }

    @Override
    public Version version() {
        return VERSION;
    }

    static <T extends Number & Comparable<T>> boolean between(T value, T min, T max) {
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }

    @Override
    public Number getNumberValue() throws IOException {
        if (parser.currentEvent != Event.VALUE_NUMBER) {
            throw new JsonParseException(this, "Current token is not a number");
        }

        BigDecimal value = parser.getBigDecimal();
        Number result;

        switch (getNumberType()) {
        case BIG_INTEGER:
            result = value.toBigInteger();
            break;
        case DOUBLE:
            result = value.doubleValue();
            break;
        case FLOAT:
            result = value.floatValue();
            break;
        case INT:
            result = value.intValue();
            break;
        case LONG:
            result = value.longValue();
            break;
        default:
            result = value;
            break;
        }

        return result;
    }

    @Override
    public NumberType getNumberType() throws IOException {
        if (parser.currentEvent != Event.VALUE_NUMBER) {
            return null;
        }

        BigDecimal value = parser.getBigDecimal();
        NumberType type;

        if (value.scale() == 0) {
            if (between(value, MIN_INTEGER, MAX_INTEGER)) {
                type = NumberType.INT;
            } else if (between(value, MIN_LONG, MAX_LONG)) {
                type = NumberType.LONG;
            } else {
                type = NumberType.BIG_INTEGER;
            }
        } else {
            int exp = value.precision() - value.scale() - 1;

            if (between(value, MIN_FLOAT, MAX_FLOAT)
                    && between(exp, Float.MIN_EXPONENT, Float.MAX_EXPONENT)) {
                type = NumberType.FLOAT;
            } else if (between(value, MIN_DOUBLE, MAX_DOUBLE)
                    && between(exp, Double.MIN_EXPONENT, Double.MAX_EXPONENT)) {
                type = NumberType.DOUBLE;
            } else {
                type = NumberType.BIG_DECIMAL;
            }
        }

        return type;
    }

    @Override
    public int getIntValue() {
        return parser.getInt();
    }

    @Override
    public long getLongValue() {
        return parser.getLong();
    }

    @Override
    public float getFloatValue() {
        return parser.getBigDecimal().floatValue();
    }

    @Override
    public double getDoubleValue() {
        return parser.getBigDecimal().doubleValue();
    }

    @Override
    public BigDecimal getDecimalValue() throws IOException {
        return parser.getBigDecimal();
    }

    class JacksonJsonParser extends StaEDIJsonParser<JsonParseException> {

        JacksonJsonParser(EDIStreamReader ediReader, Map<String, Object> properties) {
            super(ediReader, properties);
        }

        @Override
        protected JsonParseException newJsonException(String message, Throwable cause) {
            return new JsonParseException(StaEDIJacksonJsonParser.this, message, cause);
        }

        @Override
        protected JsonParseException newJsonParsingException(String message, Throwable cause) {
            return newJsonException(message, cause);
        }
    }

    static class JacksonJsonLocation extends JsonLocation {
        private static final long serialVersionUID = 1L;

        public JacksonJsonLocation(long streamOffset, long lineNumber, long columnNumber) {
            super(ContentReference.unknown(), streamOffset, (int) lineNumber, (int) columnNumber);
        }

        public long getStreamOffset() {
            return _totalChars;
        }

        public long getLineNumber() {
            return _lineNr;
        }

        public long getColumnNumber() {
            return _columnNr;
        }
    }

    static class StaEDIJsonStreamContext extends JsonStreamContext {

        StaEDIJsonStreamContext parent;
        String name;

        StaEDIJsonStreamContext(int type, StaEDIJsonStreamContext parent) {
            super(type, -1);
            this.parent = parent;
        }

        StaEDIJsonStreamContext(StaEDIJsonStreamContext source) {
            super(source);
            this.parent = source.parent != null ? source.parent.deepCopy() : null;
            this.name = source.name;
        }

        void incrementIndexIfArray() {
            if (inArray()) {
                incrementIndex();
            }
        }

        void incrementIndex() {
            this._index++;
        }

        @Override
        public JsonStreamContext getParent() {
            return parent;
        }

        @Override
        public String getCurrentName() {
            return name;
        }

        public void setCurrentName(String name) {
            this.name = name;
        }

        public StaEDIJsonStreamContext deepCopy() {
            return new StaEDIJsonStreamContext(this);
        }
    }

    static {
        VERSION = Optional.ofNullable(StaEDIJacksonJsonParser.class.getResourceAsStream("/io/xlate/edi/internal/project.properties"))
                .map(stream -> {
                    try (InputStream source = stream) {
                        Properties projectProperties = new Properties();
                        projectProperties.load(source);
                        return projectProperties;
                    } catch (IOException e) {
                        return null;
                    }
                })
                .map(StaEDIJacksonJsonParser::toVersion)
                .orElseGet(Version::unknownVersion);
    }

    static Version toVersion(Properties projectProperties) {
        String[] version = projectProperties.getProperty("version").split("[.-]");

        return new Version(
                Integer.parseInt(version[0]),
                Integer.parseInt(version[1]),
                Integer.parseInt(version[2]),
                version.length > 3 ? version[3] : null,
                projectProperties.getProperty("groupId"),
                projectProperties.getProperty("artifactId"));
    }
}
