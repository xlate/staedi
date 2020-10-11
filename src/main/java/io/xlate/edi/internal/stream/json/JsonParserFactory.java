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

public final class JsonParserFactory {

    private JsonParserFactory() {
    }

    @SuppressWarnings("unchecked")
    public static <J> J createJsonParser(EDIStreamReader reader, Class<J> type, Map<String, Object> properties) {
        final J parser;

        switch (type.getName()) {
        case "jakarta.json.stream.JsonParser":
            parser = (J) new StaEDIJakartaJsonParser(reader, properties);
            break;
        case "javax.json.stream.JsonParser":
            parser = (J) new StaEDIJavaxJsonParser(reader, properties);
            break;
        default:
            throw new IllegalArgumentException("Unsupported JSON parser type: " + type);
        }

        return parser;
    }

}
