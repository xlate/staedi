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
package io.xlate.edi.internal.stream.validation;

import java.io.IOException;
import java.util.List;

import io.xlate.edi.internal.stream.tokenization.Dialect;
import io.xlate.edi.internal.stream.tokenization.EDIException;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.stream.EDIStreamValidationError;

class NumericValidator extends ElementValidator {

    private static final NumericValidator singleton = new NumericValidator();

    protected NumericValidator() {
    }

    static NumericValidator getInstance() {
        return singleton;
    }

    @Override
    void validate(Dialect dialect, EDISimpleType element, CharSequence value, List<EDIStreamValidationError> errors) {
        int length = validate(dialect, value);
        validateLength(element, Math.abs(length), errors);

        if (length < 0) {
            errors.add(EDIStreamValidationError.INVALID_CHARACTER_DATA);
        }
    }

    @Override
    void format(Dialect dialect, EDISimpleType element, CharSequence value, Appendable result) throws EDIException {
        int length = validate(dialect, value);
        assertMaxLength(element, Math.abs(length));

        if (length < 0) {
            throw new EDIException(EDIException.INVALID_CHARACTER);
        }

        try {
            for (long i = length, min = element.getMinLength(); i < min; i++) {
                result.append('0');
            }

            result.append(value);
        } catch (IOException e) {
            throw new EDIException(e);
        }
    }

    /**
     * Validate that the value contains only characters the represent an
     * integer.
     *
     * @param dialect the dialect currently be parsed
     * @param value the sequence of characters to validate
     * @return true of the value is a valid integer representation, otherwise false
     */
    int validate(Dialect dialect, CharSequence value) {
        int length = value.length();
        boolean invalid = false;

        for (int i = 0, m = length; i < m; i++) {
            switch (value.charAt(i)) {
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
            case '-':
                length--;

                if (i > 0) {
                    invalid = true;
                }
                break;

            default:
                invalid = true;
                break;
            }
        }

        return invalid ? -length : length;
    }
}
