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
package io.xlate.edi.internal.stream.tokenization;

import java.io.InputStream;

import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamValidationError;

public interface EventHandler {

    void interchangeBegin(Dialect dialect);

    void interchangeEnd();

    void loopBegin(CharSequence id);

    void loopEnd(CharSequence id);

    void segmentBegin(char[] text, int start, int length);

    void segmentEnd();

    void compositeBegin(boolean isNil);

    void compositeEnd(boolean isNil);

    void elementData(char[] text, int start, int length);

    void binaryData(InputStream binary);

    void segmentError(CharSequence token, EDIStreamValidationError error);

    void elementError(EDIStreamEvent event, EDIStreamValidationError error, int element, int component, int repetition);
}
