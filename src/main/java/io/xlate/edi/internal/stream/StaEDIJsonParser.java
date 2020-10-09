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

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;

import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamReader;

class StaEDIJsonParser {

    static final String MSG_EXCEPTION = "Exception reading the EDI stream as JSON";
    static final String MSG_UNEXPECTED = "Unexpected event reached parsing JSON: ";

    static final String KEY_TYPE = "type";
    static final String KEY_NAME = "name";
    static final String KEY_META = "meta";
    static final String KEY_DATA = "data";

    protected final EDIStreamReader ediReader;
    protected final Map<String, Object> properties;

    final Queue<Event> events = new ArrayDeque<>();

    class JsonLocation {
        public long getLineNumber() {
            return ediReader.getLocation().getLineNumber();
        }

        public long getColumnNumber() {
            return ediReader.getLocation().getColumnNumber();
        }

        public long getStreamOffset() {
            return ediReader.getLocation().getCharacterOffset();
        }
    }

    enum Event {
        START_ARRAY,
        START_OBJECT,
        KEY_NAME,
        VALUE_STRING,
        VALUE_NUMBER,
        VALUE_TRUE,
        VALUE_FALSE,
        VALUE_NULL,
        END_OBJECT,
        END_ARRAY
    }

    StaEDIJsonParser(EDIStreamReader ediReader, Map<String, Object> properties) {
        super();
        this.ediReader = ediReader;
        this.properties = properties;
    }

    /**
     * @see jakarta.json.stream.JsonParser#hasNext()
     */
    public boolean hasNext() {
        // TODO Auto-generated method stub
        return false;
    }

    Event nextEvent() throws EDIStreamException {
        return null;
    }

    /**
     * @see jakarta.json.stream.JsonParser#close()
     */
    public void close() {
        // TODO Auto-generated method stub

    }

    /**
     * @see jakarta.json.stream.JsonParser#getBigDecimal()
     */
    public BigDecimal getBigDecimal() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see jakarta.json.stream.JsonParser#getInt()
     */
    public int getInt() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * @see jakarta.json.stream.JsonParser#getLong()
     */
    public long getLong() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * @see jakarta.json.stream.JsonParser#getString()
     */
    public String getString() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see jakarta.json.stream.JsonParser#isIntegralNumber()
     */
    public boolean isIntegralNumber() {
        // TODO Auto-generated method stub
        return false;
    }

}
