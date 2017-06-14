/*******************************************************************************
 * Copyright 2017 xlate.io LLC, http://www.xlate.io
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package io.xlate.edi.stream.validation;

import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.stream.EDIStreamConstants.ElementDataErrors;
import io.xlate.edi.stream.internal.EDIException;

import java.util.List;

abstract class ElementValidator implements ElementDataErrors {

	static ElementValidator getInstance(int type) {
		switch (type) {
		case EDISimpleType.BASE_IDENTIFIER:
		case EDISimpleType.BASE_STRING:
			return AlphaNumericValidator.getInstance();
		case EDISimpleType.BASE_INTEGER:
			return NumericValidator.getInstance();
		case EDISimpleType.BASE_DECIMAL:
			return DecimalValidator.getInstance();
		case EDISimpleType.BASE_DATE:
			return DateValidator.getInstance();
		case EDISimpleType.BASE_TIME:
			return TimeValidator.getInstance();
		case EDISimpleType.BASE_BINARY:
			return null;
		default:
			throw new IllegalArgumentException("Illegal type " + type);
		}
	}

	protected static boolean validLength(
			EDISimpleType element,
			int length,
			List<Integer> errors) {

		if (length > element.getMaxLength()) {
			errors.add(DATA_ELEMENT_TOO_LONG);
			return false;
		} else if (length < element.getMinLength()) {
			errors.add(DATA_ELEMENT_TOO_SHORT);
			return false;
		}

		return true;
	}

	protected static void checkLength(EDISimpleType element, int length)
			throws EDIException {

		if (length > element.getMaxLength()) {
			// DATA_ELEMENT_TOO_LONG
			throw new EDIException();
		}
	}

	abstract void validate(
			EDISimpleType element,
			CharSequence value,
			List<Integer> errors);

	abstract void format(
			EDISimpleType element,
			CharSequence value,
			Appendable result) throws EDIException;
}
