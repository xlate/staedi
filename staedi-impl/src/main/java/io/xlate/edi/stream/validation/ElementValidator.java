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

import java.util.List;

import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.internal.EDIException;

abstract class ElementValidator {

    static ElementValidator getInstance(EDISimpleType.Base type) {
        switch (type) {
        case IDENTIFIER:
        case STRING:
            return AlphaNumericValidator.getInstance();
        case INTEGER:
            return NumericValidator.getInstance();
        case DECIMAL:
            return DecimalValidator.getInstance();
        case DATE:
            return DateValidator.getInstance();
        case TIME:
            return TimeValidator.getInstance();
        case BINARY:
            return null;
        default:
            throw new IllegalArgumentException("Illegal type " + type);
        }
    }

    protected static boolean validateLength(EDISimpleType element,
                                         int length,
                                         List<EDIStreamValidationError> errors) {

        if (length > element.getMaxLength()) {
            errors.add(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG);
            return false;
        } else if (length < element.getMinLength()) {
            errors.add(EDIStreamValidationError.DATA_ELEMENT_TOO_SHORT);
            return false;
        }

        return true;
    }

    protected static void assertMinLength(EDISimpleType element, int length) throws EDIException {
        if (length < element.getMinLength()) {
            throw new EDIException(EDIException.DATA_ELEMENT_TOO_SHORT);
        }
    }

    protected static void assertMaxLength(EDISimpleType element, int length) throws EDIException {
        if (length > element.getMaxLength()) {
            throw new EDIException(EDIException.DATA_ELEMENT_TOO_LONG);
        }
    }

    abstract void validate(EDISimpleType element,
                           CharSequence value,
                           List<EDIStreamValidationError> errors);

    abstract void format(EDISimpleType element,
                         CharSequence value,
                         Appendable result) throws EDIException;
}
