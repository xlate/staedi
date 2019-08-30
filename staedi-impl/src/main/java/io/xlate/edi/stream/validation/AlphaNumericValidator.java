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
import java.util.Set;

import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.internal.CharacterSet;
import io.xlate.edi.stream.internal.EDIException;

class AlphaNumericValidator extends ElementValidator {

    private static final AlphaNumericValidator singleton = new AlphaNumericValidator();

    private AlphaNumericValidator() {
    }

    static AlphaNumericValidator getInstance() {
        return singleton;
    }

    @Override
    void validate(EDISimpleType element,
                  CharSequence value,
                  List<EDIStreamValidationError> errors) {

        int length = value.length();
        validLength(element, length, errors);

        Set<String> valueSet = element.getValueSet();

        if (!valueSet.isEmpty() && !valueSet.contains(value.toString())) {
            errors.add(EDIStreamValidationError.INVALID_CODE_VALUE);
        } else {
            for (int i = 0; i < length; i++) {
                char character = value.charAt(i);

                if (!CharacterSet.isValid(character)) {
                    errors.add(EDIStreamValidationError.INVALID_CHARACTER_DATA);
                    break;
                }
            }
        }
    }

    @Override
    void format(EDISimpleType element, CharSequence value, Appendable result) throws EDIException {

        int length = value.length();
        checkLength(element, length);

        Set<String> valueSet = element.getValueSet();

        if (!valueSet.isEmpty() && !valueSet.contains(value.toString())) {
            // TODO: INVALID_CODE_VALUE
            throw new EDIException();
        }

        for (int i = 0; i < length; i++) {
            char character = value.charAt(i);

            if (!CharacterSet.isValid(character)) {
                throw new EDIException(EDIException.INVALID_CHARACTER,
                                       "Invalid character 0x" + String.format("%04X", character));
            }

            try {
                result.append(character);
            } catch (IOException e) {
                throw new EDIException(e);
            }
        }

        for (int i = length, min = element.getMinLength(); i < min; i++) {
            try {
                result.append(' ');
            } catch (IOException e) {
                throw new EDIException(e);
            }
        }
    }
}
