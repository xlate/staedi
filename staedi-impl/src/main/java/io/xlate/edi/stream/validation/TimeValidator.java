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
package io.xlate.edi.stream.validation;

import java.io.IOException;
import java.util.List;

import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.internal.EDIException;

class TimeValidator extends ElementValidator {

    private static final TimeValidator singleton = new TimeValidator();

    private TimeValidator() {
    }

    static TimeValidator getInstance() {
        return singleton;
    }

    @Override
    void validate(EDISimpleType element,
                  CharSequence value,
                  List<EDIStreamValidationError> errors) {
        final int length = value.length();

        if (!validateLength(element, length, errors) || !validValue(value)) {
            errors.add(EDIStreamValidationError.INVALID_TIME);
        }
    }

    @Override
    void format(EDISimpleType element, CharSequence value, Appendable result) throws EDIException {
        int length = value.length();
        assertMaxLength(element, length);

        if (!validValue(value)) {
            throw new EDIException(EDIException.INVALID_TIME);
        }

        try {
            result.append(value);

            for (int i = length, min = element.getMinLength(); i < min; i++) {
                result.append('0');
            }
        } catch (IOException e) {
            throw new EDIException(e);
        }
    }

    static boolean validValue(CharSequence value) {
        final int length = value.length();
        int hr = 0;
        int mi = 0;
        int se = 0;
        int ds = 0;
        int index = 0;

        for (int i = 0; i < length; i++) {
            char current = value.charAt(i);

            switch (current) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                break;
            default:
                return false;
            }

            int digit = Character.digit(current, 10);

            switch (++index) {
            case 1:
            case 2:
                hr = hr * 10 + digit;
                break;

            case 3:
            case 4:
                mi = mi * 10 + digit;
                break;

            case 5:
            case 6:
                se = se * 10 + digit;
                break;

            default:
                ds = ds * 10 + digit;
                break;
            }
        }

        return hr < 24 && mi < 60 && se < 60;
    }
}
