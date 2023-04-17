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
package io.xlate.edi.internal.stream.json;

import java.util.Map;

import io.xlate.edi.stream.EDIStreamReader;

final class StaEDIJakartaJsonParser extends StaEDIJsonParser<jakarta.json.JsonException>
        implements jakarta.json.stream.JsonParser, jakarta.json.stream.JsonLocation {

    static final jakarta.json.stream.JsonParser.Event[] eventMap = mapEvents(jakarta.json.stream.JsonParser.Event.class);
    static final jakarta.json.spi.JsonProvider jsonProvider = jakarta.json.spi.JsonProvider.provider();

    StaEDIJakartaJsonParser(EDIStreamReader ediReader, Map<String, Object> properties) {
        super(ediReader, properties);
    }

    @Override
    protected jakarta.json.JsonException newJsonException(String message, Throwable cause) {
        return new jakarta.json.JsonException(message, cause);
    }

    @Override
    protected jakarta.json.JsonException newJsonParsingException(String message, Throwable cause) {
        return new jakarta.json.stream.JsonParsingException(message, cause, this);
    }

    @Override
    public jakarta.json.stream.JsonLocation getLocation() {
        return this;
    }

    @Override
    public jakarta.json.stream.JsonParser.Event next() {
        return eventMap[nextEvent().ordinal()];
    }

    @Override
    public jakarta.json.JsonValue getValue() {
        assertEventSet("getValue illegal when data stream has not yet been read");

        switch (currentEvent) {
        case START_OBJECT:
            return getObject();
        case START_ARRAY:
            return getArray();
        case VALUE_NULL:
            return jakarta.json.JsonValue.NULL;
        case VALUE_NUMBER:
            return isIntegralNumber() ?
                    jsonProvider.createValue(getLong()) :
                    jsonProvider.createValue(getBigDecimal());
        case VALUE_STRING:
        case KEY_NAME:
            return jsonProvider.createValue(getString());
        default:
            throw new IllegalStateException("getValue illegal when at current position");
        }
    }

    @Override
    public jakarta.json.JsonArray getArray() {
        assertEvent(StaEDIJsonParser.Event.START_ARRAY, current -> "getArray illegal when not at start of array");

        jakarta.json.JsonArrayBuilder builder = jsonProvider.createArrayBuilder();

        while (nextEvent() != StaEDIJsonParser.Event.END_ARRAY) {
            builder.add(getValue());
        }

        return builder.build();
    }

    @Override
    public jakarta.json.JsonObject getObject() {
        assertEvent(StaEDIJsonParser.Event.START_OBJECT, current -> "getObject illegal when not at start of object");

        jakarta.json.JsonObjectBuilder builder = jsonProvider.createObjectBuilder();

        StaEDIJsonParser.Event next;
        String key = null;

        while ((next = nextEvent()) != StaEDIJsonParser.Event.END_OBJECT) {
            if (next == StaEDIJsonParser.Event.KEY_NAME) {
                key = getString();
            } else {
                builder.add(key, getValue());
            }
        }

        return builder.build();
    }
}
