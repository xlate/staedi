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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import io.xlate.edi.internal.stream.tokenization.Dialect;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.stream.EDIStreamValidationError;

abstract class ElementValidator {

    static class ValidatorInstances {
        static final Map<EDISimpleType.Base, ElementValidator> instances = new EnumMap<>(EDISimpleType.Base.class);

        static {
            instances.put(EDISimpleType.Base.IDENTIFIER, new AlphaNumericValidator());
            instances.put(EDISimpleType.Base.STRING, new AlphaNumericValidator());
            instances.put(EDISimpleType.Base.NUMERIC, new NumericValidator());
            instances.put(EDISimpleType.Base.DECIMAL, new DecimalValidator());
            instances.put(EDISimpleType.Base.DATE, new DateValidator());
            instances.put(EDISimpleType.Base.TIME, new TimeValidator());
        }

        private ValidatorInstances() {
        }
    }

    static ElementValidator getInstance(EDISimpleType.Base type) {
        return ValidatorInstances.instances.get(type);
    }

    protected static boolean validateLength(Dialect dialect,
                                            EDISimpleType element,
                                            int length,
                                            List<EDIStreamValidationError> errors) {

        final String version = dialect.getTransactionVersionString();

        if (length > element.getMaxLength(version)) {
            errors.add(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG);
            return false;
        } else if (length < element.getMinLength(version)) {
            errors.add(EDIStreamValidationError.DATA_ELEMENT_TOO_SHORT);
            return false;
        }

        return true;
    }

    abstract void validate(Dialect dialect,
                           EDISimpleType element,
                           CharSequence value,
                           List<EDIStreamValidationError> errors);

    abstract void format(Dialect dialect,
                         EDISimpleType element,
                         CharSequence value,
                         StringBuilder result);
}
