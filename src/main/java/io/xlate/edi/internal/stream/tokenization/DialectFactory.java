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
package io.xlate.edi.internal.stream.internal;

public abstract class DialectFactory {

    private enum DialectTag {
        X12("ISA"),
        EDIFACT_A("UNA"),
        EDIFACT_B("UNB");

        private String tag;

        DialectTag(String tag) {
            this.tag = tag;
        }

        public static DialectTag fromValue(String tag) {
            for (DialectTag d : DialectTag.values()) {
                if (d.tag.equals(tag)) {
                    return d;
                }
            }
            return null;
        }
    }

    public static Dialect getDialect(char[] buffer, int start, int length) throws EDIException {
        String tag = new String(buffer, start, length);
        return getDialect(tag);
    }

    public static Dialect getDialect(String tag) throws EDIException {
        DialectTag type = DialectTag.fromValue(tag);

        if (type != null) {
            Dialect dialect;

            switch (type) {
            case X12:
                dialect = new X12Dialect();
                break;
            case EDIFACT_A:
            case EDIFACT_B:
                dialect = new EDIFACTDialect();
                break;
            default:
                throw new EDIException(EDIException.UNSUPPORTED_DIALECT, tag);
            }

            dialect.setHeaderTag(tag);

            return dialect;
        }

        throw new EDIException(EDIException.UNSUPPORTED_DIALECT, tag);
    }
}
