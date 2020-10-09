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
import java.util.Map;

import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamReader;

final class StaEDIJakartaJsonParser extends StaEDIJsonParser implements jakarta.json.stream.JsonParser {

    protected final Location location;

    private class Location extends JsonLocation implements jakarta.json.stream.JsonLocation {
    }

    StaEDIJakartaJsonParser(EDIStreamReader ediReader, Map<String, Object> properties) {
        super(ediReader, properties);
        this.location = new Location();
    }

    @Override
    public jakarta.json.stream.JsonLocation getLocation() {
        return location;
    }

    @Override
    public jakarta.json.stream.JsonParser.Event next() {
        final StaEDIJsonParser.Event next;

        try {
            next = nextEvent();
        } catch (EDIStreamException e) {
            if (e.getCause() instanceof IOException) {
                throw new jakarta.json.JsonException(MSG_EXCEPTION, e);
            } else {
                throw new jakarta.json.stream.JsonParsingException(MSG_EXCEPTION, e, location);
            }
        }

        switch (next) {
        case END_ARRAY:
            return jakarta.json.stream.JsonParser.Event.END_ARRAY;
        case END_OBJECT:
            return jakarta.json.stream.JsonParser.Event.END_OBJECT;
        case KEY_NAME:
            return jakarta.json.stream.JsonParser.Event.KEY_NAME;
        case START_ARRAY:
            return jakarta.json.stream.JsonParser.Event.START_ARRAY;
        case START_OBJECT:
            return jakarta.json.stream.JsonParser.Event.START_OBJECT;
        case VALUE_FALSE:
            return jakarta.json.stream.JsonParser.Event.VALUE_FALSE;
        case VALUE_NULL:
            return jakarta.json.stream.JsonParser.Event.VALUE_NULL;
        case VALUE_NUMBER:
            return jakarta.json.stream.JsonParser.Event.VALUE_NUMBER;
        case VALUE_STRING:
            return jakarta.json.stream.JsonParser.Event.VALUE_STRING;
        case VALUE_TRUE:
            return jakarta.json.stream.JsonParser.Event.VALUE_TRUE;
        default:
            throw new jakarta.json.stream.JsonParsingException(MSG_UNEXPECTED + next, location);
        }
    }

}
