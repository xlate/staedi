/*******************************************************************************
 * Copyright 2020 xlate.io LLC, http://www.xlate.io
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
package io.xlate.edi.internal.stream;

import java.util.function.Function;

public interface Configurable {

    Object getProperty(String name);

    default <T> T getProperty(String name, Function<String, T> parser, T defaultValue) {
        Object property = getProperty(name);

        if (property == null) {
            return defaultValue;
        }

        return parser.apply(property.toString());
    }

}
