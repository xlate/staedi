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
import java.util.Calendar;
import java.util.List;

import io.xlate.edi.internal.stream.tokenization.Dialect;
import io.xlate.edi.internal.stream.tokenization.EDIException;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.EDIValidationException;

class DateValidator extends ElementValidator {

    private static final DateValidator singleton = new DateValidator();

    private DateValidator() {
    }

    static DateValidator getInstance() {
        return singleton;
    }

    @Override
    void validate(Dialect dialect,
                  EDISimpleType element,
                  CharSequence value,
                  List<EDIStreamValidationError> errors) {

        int length = value.length();

        if (!validateLength(element, length, errors) || length % 2 != 0 || !validValue(value)) {
            errors.add(EDIStreamValidationError.INVALID_DATE);
        }
    }

    @Override
    void format(Dialect dialect, EDISimpleType element, CharSequence value, Appendable result) throws EDIException {
        assertMinLength(element, value);
        assertMaxLength(element, value);

        if (!validValue(value)) {
            throw new EDIValidationException(EDIStreamEvent.ELEMENT_DATA, EDIStreamValidationError.INVALID_DATE, null, value);
        }

        try {
            result.append(value);
        } catch (IOException e) {
            throw new EDIException(e);
        }
    }

    static boolean validValue(CharSequence value) {
        int length = value.length();
        int dateValue = 0;

        for (int i = 0; i < length; i++) {
            char c = value.charAt(i);
            switch (c) {
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
                dateValue = dateValue * 10 + Character.digit(c, 10);
                break;
            default:
                return false;
            }
        }

        int[] date = new int[3];

        date[2] = dateValue % 100;
        dateValue /= 100;
        date[1] = dateValue % 100;
        dateValue /= 100;
        date[0] = dateValue;

        /*-
         * Add the century if the date is missing it - assume all dates
         * are current year or in the past.
         **/
        if (length == 6) {
            // TODO: Add reader property for date window
            int year = Calendar.getInstance().get(Calendar.YEAR);
            int century = year / 100;

            if (date[0] > (year % 100)) {
                date[0] = (century - 1) * 100 + date[0];
            } else {
                date[0] = century * 100 + date[0];
            }
        }

        return dateIsValid(date[0], date[1], date[2]);
    }

    static boolean dateIsValid(int year, int month, int day) {
        if (day < 1) {
            return false;
        }

        switch (month) {
        case 1:
        case 3:
        case 5:
        case 7:
        case 8:
        case 10:
        case 12:
            return day <= 31;
        case 4:
        case 6:
        case 9:
        case 11:
            return day <= 30;
        case 2:
            return day <= 28 || (isLeapYear(year) && day <= 29);
        default:
            return false;
        }
    }

    static boolean isLeapYear(int year) {
        return (year % 4 == 0) && (year % 100 != 0 || year % 400 == 0);
    }
}
