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

import java.util.List;
import java.util.Set;

import io.xlate.edi.internal.stream.tokenization.CharacterSet;
import io.xlate.edi.internal.stream.tokenization.Dialect;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.stream.EDIStreamValidationError;

class AlphaNumericValidator extends ElementValidator {

    private static final AlphaNumericValidator singleton = new AlphaNumericValidator();

    private AlphaNumericValidator() {
    }

    static AlphaNumericValidator getInstance() {
        return singleton;
    }

    @Override
    void validate(Dialect dialect,
                  EDISimpleType element,
                  CharSequence value,
                  List<EDIStreamValidationError> errors) {

        int length = value.length();
        validateLength(dialect, element, length, errors);

        Set<String> valueSet = element.getValueSet(dialect.getTransactionVersionString());

        if (valueSet.isEmpty() || valueSet.contains(value.toString())) {
            if (valueSet.isEmpty()) {
                // Only validate the characters if not explicitly listed in the set of allowed values
                validateCharacters(value, length, errors);
            }
        } else {
            validateCharacters(value, length, errors);
            errors.add(EDIStreamValidationError.INVALID_CODE_VALUE);
        }
    }

    static void validateCharacters(CharSequence value, int length, List<EDIStreamValidationError> errors) {
        if (!validCharacters(value, length)) {
            errors.add(EDIStreamValidationError.INVALID_CHARACTER_DATA);
        }
    }

    static boolean validCharacters(CharSequence value, int length) {
        for (int i = 0; i < length; i++) {
            if (!CharacterSet.isValid(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    void format(Dialect dialect, EDISimpleType element, CharSequence value, StringBuilder result) {
        int length = value.length();

        for (int i = 0; i < length; i++) {
            result.append(value.charAt(i));
        }

        for (long i = length, min = element.getMinLength(); i < min; i++) {
            result.append(' ');
        }
    }
}
