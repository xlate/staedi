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
import io.xlate.edi.stream.internal.EDIException;

import java.io.IOException;
import java.util.List;

class TimeValidator extends ElementValidator {

	private static final TimeValidator singleton = new TimeValidator();

	private TimeValidator() {}

	static TimeValidator getInstance() {
		return singleton;
	}

	@Override
	void validate(
			EDISimpleType element,
			CharSequence value,
			List<Integer> errors) {
		final int length = value.length();

		if (!validLength(element, length, errors)) {
			errors.add(INVALID_TIME);
		} else if (!validValue(value)) {
			errors.add(INVALID_TIME);
		}
	}

	@Override
	void format(EDISimpleType element, CharSequence value, Appendable result)
			throws EDIException {
		int length = value.length();
		checkLength(element, length);

		if (validValue(value)) {
			try {
				result.append(value);
			} catch (IOException e) {
				throw new EDIException(e);
			}

			for (int i = length, min = element.getMinLength(); i < min; i++) {
				try {
					result.append('0');
				} catch (IOException e) {
					throw new EDIException(e);
				}
			}
		} else {
			// TODO: INVALID_TIME
			throw new EDIException();
		}
	}

	private static boolean validValue(CharSequence value) {
		final int length = value.length();
		int hr = 0;
		int mi = 0;
		int se = 0;
		int ds = 0;
		int index = 0;

		for (int i = length - length; i < length; i++) {
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
