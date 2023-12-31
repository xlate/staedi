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

import io.xlate.edi.internal.stream.tokenization.Dialect;

class DecimalValidator extends NumericValidator {

    @Override
    int validate(Dialect dialect, CharSequence value) {
        int length = value.length();

        int dec = 0;
        int exp = 0;
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

            case 'E':
                length--;

                if (++exp > 1) {
                    invalid = true;
                }
                break;

            case '-':
                length--;
                invalid = invalidNegativeSymbol(i, value, invalid);
                break;

            default:
                if (dialect.isDecimalMark(value.charAt(i))) {
                    length--;
                    invalid = invalidDecimalSymbol(++dec, exp, invalid);
                } else {
                    invalid = true;
                }

                break;
            }
        }

        return invalid ? -length : length;
    }

    boolean invalidNegativeSymbol(int currentIndex, CharSequence value, boolean currentlyInvalid) {
        return currentlyInvalid || (currentIndex > 0 && value.charAt(currentIndex - 1) != 'E');
    }

    boolean invalidDecimalSymbol(int decimalCount, int exponentCount, boolean currentlyInvalid) {
        return currentlyInvalid || decimalCount > 1 || exponentCount > 0;
    }
}
