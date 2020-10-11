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

import java.util.Map;

import io.xlate.edi.stream.EDIStreamReader;

final class StaEDIJavaxJsonParser extends StaEDIJsonParser
implements javax.json.stream.JsonParser, javax.json.stream.JsonLocation {

    StaEDIJavaxJsonParser(EDIStreamReader ediReader, Map<String, Object> properties) {
        super(ediReader, properties);
    }

    @Override
    protected RuntimeException newJsonException(String message, Throwable cause) {
        return new javax.json.JsonException(message, cause);
    }

    @Override
    protected RuntimeException newJsonParsingException(String message, Throwable cause) {
        return new javax.json.stream.JsonParsingException(message, cause, this);
    }

    @Override
    public javax.json.stream.JsonLocation getLocation() {
        return this;
    }

    @Override
    public javax.json.stream.JsonParser.Event next() {
        final StaEDIJsonParser.Event next = nextEvent();

        switch (next) {
        case END_ARRAY:
            return javax.json.stream.JsonParser.Event.END_ARRAY;
        case END_OBJECT:
            return javax.json.stream.JsonParser.Event.END_OBJECT;
        case KEY_NAME:
            return javax.json.stream.JsonParser.Event.KEY_NAME;
        case START_ARRAY:
            return javax.json.stream.JsonParser.Event.START_ARRAY;
        case START_OBJECT:
            return javax.json.stream.JsonParser.Event.START_OBJECT;
        case VALUE_NULL:
            return javax.json.stream.JsonParser.Event.VALUE_NULL;
        case VALUE_NUMBER:
            return javax.json.stream.JsonParser.Event.VALUE_NUMBER;
        case VALUE_STRING:
            return javax.json.stream.JsonParser.Event.VALUE_STRING;
        default:
            // JSON 'true' and 'false' are not expected in the EDI stream
            throw new javax.json.stream.JsonParsingException(MSG_UNEXPECTED + next, this);
        }
    }

}
