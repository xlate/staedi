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
package io.xlate.edi.schema;

import java.util.Set;

public interface EDISimpleType extends EDIType {

	public static final int BASE_STRING = 1;
	public static final int BASE_INTEGER = 2;
	public static final int BASE_DECIMAL = 3;
	public static final int BASE_DATE = 4;
	public static final int BASE_TIME = 5;
	public static final int BASE_BINARY = 6;
	public static final int BASE_IDENTIFIER = 7;

	int getBaseCode();
	int getNumber();
	int getMinLength();
	int getMaxLength();
	Set<String> getValueSet();

}
