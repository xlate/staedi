/*******************************************************************************
 * Copyright 2020 xlate.io LLC, http://www.xlate.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package io.xlate.edi.internal.stream;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.schema.EDISimpleType.Base;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamReader;

abstract class StaEDIJsonParser implements Configurable {

    private static final Logger LOGGER = Logger.getLogger(StaEDIJsonParser.class.getName());

    static final String MSG_EXCEPTION = "Exception reading the EDI stream as JSON";
    static final String MSG_UNEXPECTED = "Unexpected event reached parsing JSON: ";

    static final String KEY_TYPE = "type";
    static final String KEY_NAME = "name";
    static final String KEY_META = "meta";
    static final String KEY_DATA = "data";

    static final EDIType TYPE_DUMMY = new EDIType() {
        @Override
        public String getId() {
            return null;
        }
        @Override
        public String getCode() {
            return null;
        }
        @Override
        public Type getType() {
            return null;
        }
        @Override
        public String getTitle() {
            return null;
        }
        @Override
        public String getDescription() {
            return null;
        }
    };

    protected final EDIStreamReader ediReader;
    protected final Map<String, Object> properties;
    protected final boolean emptyElementsNull;
    protected final boolean elementsAsObject;

    final Queue<Event> eventQueue = new ArrayDeque<>();
    final Queue<EDIType> typeQueue = new ArrayDeque<>();
    final Queue<String> valueQueue = new ArrayDeque<>();

    Event currentEvent;
    EDIType currentType;
    String currentValue;
    Number currentNumberValue;

    private final StringBuilder cdataBuilder = new StringBuilder();
    /*private final OutputStream cdataStream = new OutputStream() {
        @Override
        public void write(int b) throws IOException {
            cdataBuilder.append((char) b);
        }
    };*/

    enum Event {
        /**
         * @see jakarta.json.stream.JsonParser.Event#START_ARRAY
         */
        START_ARRAY,
        /**
         * @see jakarta.json.stream.JsonParser.Event#START_OBJECT
         */
        START_OBJECT,
        /**
         * @see jakarta.json.stream.JsonParser.Event#KEY_NAME
         */
        KEY_NAME,
        /**
         * @see jakarta.json.stream.JsonParser.Event#VALUE_STRING
         */
        VALUE_STRING,
        /**
         * @see jakarta.json.stream.JsonParser.Event#VALUE_NUMBER
         */
        VALUE_NUMBER,
        /**
         * @see jakarta.json.stream.JsonParser.Event#VALUE_TRUE
         */
        VALUE_TRUE,
        /**
         * @see jakarta.json.stream.JsonParser.Event#VALUE_FALSE
         */
        VALUE_FALSE,
        /**
         * @see jakarta.json.stream.JsonParser.Event#VALUE_NULL
         */
        VALUE_NULL,
        /**
         * @see jakarta.json.stream.JsonParser.Event#END_OBJECT
         */
        END_OBJECT,
        /**
         * @see jakarta.json.stream.JsonParser.Event#END_ARRAY
         */
        END_ARRAY
    }

    StaEDIJsonParser(EDIStreamReader ediReader, Map<String, Object> properties) {
        super();
        this.ediReader = ediReader;
        this.properties = properties;
        this.emptyElementsNull = getProperty(EDIInputFactory.JSON_NULL_EMPTY_ELEMENTS, Boolean::parseBoolean, false);
        this.elementsAsObject = getProperty(EDIInputFactory.JSON_OBJECT_ELEMENTS, Boolean::parseBoolean, false);
    }

    protected abstract RuntimeException newJsonException(String message, Throwable cause);

    protected abstract RuntimeException newJsonParsingException(String message, Throwable cause);

    @Override
    public Object getProperty(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name must not be null");
        }
        return properties.get(name);
    }

    void advanceEvent() {
        currentEvent = eventQueue.remove();
        currentType = typeQueue.remove();
        currentValue = valueQueue.remove();
        currentNumberValue = null;
    }

    void enqueue(Event event, EDIType type, String value) {
        eventQueue.add(event);
        typeQueue.add(type != null ? type : TYPE_DUMMY);
        valueQueue.add(value != null ? value : "");
    }

    void enqueueStructureBegin(String typeName, String structureName) {
        enqueue(Event.START_OBJECT, null, null);

        enqueue(Event.KEY_NAME, null, KEY_NAME);
        enqueue(Event.VALUE_STRING, null, structureName);

        enqueue(Event.KEY_NAME, null, KEY_TYPE);
        enqueue(Event.VALUE_STRING, null, typeName);

        enqueue(Event.KEY_NAME, null, KEY_DATA);
        enqueue(Event.START_ARRAY, null, null);
    }

    void enqueueEvent(EDIStreamEvent ediEvent) {
        LOGGER.finer(() -> "Enqueue EDI event: " + ediEvent);
        cdataBuilder.setLength(0);

        switch (ediEvent) {
        case ELEMENT_DATA:
            EDISimpleType elementType = (EDISimpleType) ediReader.getSchemaTypeReference().getReferencedType();

            if (elementsAsObject) {
                enqueue(Event.START_OBJECT, null, null);
                enqueue(Event.KEY_NAME, null, KEY_TYPE);
                enqueue(Event.VALUE_STRING, null, "element");
                enqueue(Event.KEY_NAME, null, KEY_DATA);
            }

            final Event dataEvent;
            final String dataText = ediReader.getText();

            if (dataText.isEmpty()) {
                dataEvent = this.emptyElementsNull ? Event.VALUE_NULL : Event.VALUE_STRING;
            } else if (elementType.getBase() == Base.DECIMAL || elementType.getBase() == Base.NUMERIC) {
                dataEvent = Event.VALUE_NUMBER;
            } else {
                dataEvent = Event.VALUE_STRING;
            }

            enqueue(dataEvent, elementType, dataText);

            if (elementsAsObject) {
                enqueue(Event.END_OBJECT, null, null);
            }

            break;

        //        case ELEMENT_DATA_BINARY:
        //            /*
        //             * This section will read the binary data and Base64 the stream
        //             * into an XML CDATA section.
        //             * */
        //            name = buildName(elementStack.getFirst(), EDINamespaces.ELEMENTS);
        //            enqueueEvent(START_ELEMENT, name, false);
        //            enqueueEvent(CDATA, DUMMY_QNAME, false);
        //
        //            // This only will work if using a validation filter!
        //            InputStream input = ediReader.getBinaryData();
        //            byte[] buffer = new byte[4096];
        //            int amount;
        //
        //            try (OutputStream output = Base64.getEncoder().wrap(cdataStream)) {
        //                while ((amount = input.read(buffer)) > -1) {
        //                    output.write(buffer, 0, amount);
        //                }
        //            } catch (IOException e) {
        //                throw new XMLStreamException(e);
        //            }
        //
        //            enqueueEvent(END_ELEMENT, name, false);
        //            break;
        //
        case START_INTERCHANGE:
            enqueueStructureBegin("loop", "INTERCHANGE");
            break;
        case START_GROUP:
        case START_TRANSACTION:
        case START_LOOP:
            enqueueStructureBegin("loop", ediReader.getText());
            break;
        case START_SEGMENT:
            enqueueStructureBegin("segment", ediReader.getText());
            break;
        case START_COMPOSITE:
            enqueueStructureBegin("composite", ediReader.getReferenceCode());
            break;

        case END_INTERCHANGE:
        case END_GROUP:
        case END_TRANSACTION:
        case END_LOOP:
        case END_SEGMENT:
        case END_COMPOSITE:
            enqueue(Event.END_ARRAY, null, null);
            enqueue(Event.END_OBJECT, null, null);
            break;

        //        case SEGMENT_ERROR:
        //            throw new XMLStreamException(String.format("Segment %s has error %s",
        //                                                       ediReader.getText(),
        //                                                       ediReader.getErrorType()),
        //                                         this.location);
        //
        //        case ELEMENT_OCCURRENCE_ERROR:
        //        case ELEMENT_DATA_ERROR:
        //            throw new XMLStreamException(String.format("Element %s has error %s",
        //                                                       ediReader.getText(),
        //                                                       ediReader.getErrorType()),
        //                                         this.location);

        default:
            throw new IllegalStateException("Unknown state: " + ediEvent);
        }
    }

    Event nextEvent() {
        if (eventQueue.isEmpty()) {
            LOGGER.finer(() -> "eventQueue is empty, calling ediReader.next()");
            try {
                enqueueEvent(ediReader.next());
            } catch (EDIStreamException e) {
                if (e.getCause() instanceof IOException) {
                    throw newJsonException(MSG_EXCEPTION, e);
                } else {
                    throw newJsonParsingException(MSG_EXCEPTION, e);
                }
            }
        }

        advanceEvent();

        return currentEvent;
    }

    /**
     * @see jakarta.json.stream.JsonLocation#getLineNumber()
     */
    public long getLineNumber() {
        return ediReader.getLocation().getLineNumber();
    }

    /**
     * @see jakarta.json.stream.JsonLocation#getColumnNumber()
     */
    public long getColumnNumber() {
        return ediReader.getLocation().getColumnNumber();
    }

    /**
     * @see jakarta.json.stream.JsonLocation#getStreamOffset()
     */
    public long getStreamOffset() {
        return ediReader.getLocation().getCharacterOffset();
    }

    public void close() {
        try {
            ediReader.close();
        } catch (IOException e) {
            throw newJsonException(MSG_EXCEPTION, e);
        }
    }

    void assertEventValueNumber() {
        final Event current = this.currentEvent;

        if (current != Event.VALUE_NUMBER) {
            throw new IllegalStateException("Unable to get number value for event [" + current + ']');
        }
    }

    void assertEventValueString() {
        final Event current = this.currentEvent;

        switch (current) {
        case KEY_NAME:
        case VALUE_STRING:
        case VALUE_NUMBER:
            break;
        default:
            throw new IllegalStateException("Unable to get string value for event [" + current + ']');
        }
    }

    long pow(long base, int exponent) {
        long result = 1;

        for (int i = 1; i <= exponent; i++) {
            result *= base;
        }

        return result;
    }

    /**
     * @see jakarta.json.stream.JsonParser#hasNext()
     * @see javax.json.stream.JsonParser#hasNext()
     */
    public boolean hasNext() {
        try {
            return !eventQueue.isEmpty() || ediReader.hasNext();
        } catch (EDIStreamException e) {
            if (e.getCause() instanceof IOException) {
                throw newJsonException(MSG_EXCEPTION, e);
            } else {
                throw newJsonParsingException(MSG_EXCEPTION, e);
            }
        }
    }

    /**
     * @see jakarta.json.stream.JsonParser#getBigDecimal()
     */
    public BigDecimal getBigDecimal() {
        assertEventValueNumber();

        if (currentNumberValue instanceof BigDecimal) {
            return (BigDecimal) currentNumberValue;
        }

        final EDISimpleType type = (EDISimpleType) this.currentType;
        final String value = this.currentValue;
        final BigDecimal decimalValue;

        try {
            if (type.getBase() == Base.NUMERIC) {
                final Integer scale = type.getScale();
                final long unscaled = Long.parseLong(value);
                decimalValue = BigDecimal.valueOf(unscaled, scale);
            } else {
                decimalValue = new BigDecimal(value);
            }
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Value not a number: " + value, e);
        }

        currentNumberValue = decimalValue;

        return decimalValue;
    }

    /**
     * @see jakarta.json.stream.JsonParser#getInt()
     */
    public int getInt() {
        return (int) getLong();
    }

    /**
     * @see jakarta.json.stream.JsonParser#getLong()
     */
    public long getLong() {
        assertEventValueNumber();

        if (currentNumberValue != null) {
            return currentNumberValue.longValue();
        }

        final EDISimpleType type = (EDISimpleType) this.currentType;
        final String value = this.currentValue;
        final Number parsedValue;

        try {
            if (type.getBase() == Base.NUMERIC) {
                final Integer scale = type.getScale();
                final long unscaled = Long.parseLong(value);
                parsedValue = unscaled / this.pow(10, scale);
            } else {
                // Optimize?
                parsedValue = new BigDecimal(value);
            }
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Value not a number: " + value, e);
        }

        currentNumberValue = parsedValue;

        return parsedValue.longValue();
    }

    /**
     * @see jakarta.json.stream.JsonParser#getString()
     */
    public String getString() {
        assertEventValueString();
        return this.currentValue;
    }

    /**
     * @see jakarta.json.stream.JsonParser#isIntegralNumber()
     */
    public boolean isIntegralNumber() {
        assertEventValueNumber();
        return this.currentValue.indexOf('.') < 0;
    }

}
