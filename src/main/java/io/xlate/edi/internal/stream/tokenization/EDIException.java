/*******************************************************************************
 * Copyright 2017 xlate.io LLC, http://www.xlate.io
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
package io.xlate.edi.internal.stream.tokenization;

import java.util.HashMap;
import java.util.Map;

import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.Location;

public class EDIException extends EDIStreamException {

    private static final long serialVersionUID = -2724168743697298348L;

    public static final Integer MISSING_HANDLER = 1;
    public static final Integer UNSUPPORTED_DIALECT = 2;
    public static final Integer INVALID_STATE = 3;
    public static final Integer INVALID_CHARACTER = 4;
    public static final Integer INCOMPLETE_STREAM = 5;

    private static final Map<Integer, String> exceptionMessages = new HashMap<>();

    static {
        exceptionMessages.put(MISSING_HANDLER,
                              "EDIE001 - Missing required handler");
        exceptionMessages.put(UNSUPPORTED_DIALECT,
                              "EDIE002 - Unsupported EDI dialect");
        exceptionMessages.put(INVALID_STATE,
                              "EDIE003 - Invalid processing state");
        exceptionMessages.put(INVALID_CHARACTER,
                              "EDIE004 - Invalid character");
        exceptionMessages.put(INCOMPLETE_STREAM,
                              "EDIE005 - Unexpected end of stream");
    }

    public EDIException(String message) {
        super(message);
    }

    EDIException(Integer id, Location location) {
        super(exceptionMessages.get(id), location);
    }

    EDIException(Integer id, String message, Location location) {
        super(buildMessage(exceptionMessages.get(id), location) + "; " + message, location);
    }

    public EDIException(Integer id, String message) {
        super(exceptionMessages.get(id) + "; " + message);
    }

}
