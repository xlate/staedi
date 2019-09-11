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
package io.xlate.edi.stream;

import io.xlate.edi.schema.Schema;

import java.io.InputStream;

public abstract class EDIInputFactory {

    public static final String EDI_VALIDATE_CONTROL_STRUCTURE = "io.xlate.edi.stream.EDI_VALIDATE_CONTROL_STRUCTURE";

    static final String FACTORY_ID = "io.xlate.edi.stream.EDIInputFactory";
    static final String DEFAULT_IMPL = "io.xlate.edi.internal.stream.StaEDIInputFactory";

    public static EDIInputFactory newFactory() throws EDIFactoryConfigurationError {
        return FactoryFinder.find(FACTORY_ID, DEFAULT_IMPL);
    }

    public static EDIInputFactory newFactory(String factoryId,
                                             ClassLoader classLoader) throws EDIFactoryConfigurationError {

        return FactoryFinder.newInstance(factoryId, classLoader, false);
    }

    public abstract EDIStreamReader createEDIStreamReader(InputStream stream) throws EDIStreamException;

    public abstract EDIStreamReader createEDIStreamReader(InputStream stream,
                                                          String encoding) throws EDIStreamException;

    public abstract EDIStreamReader createEDIStreamReader(InputStream stream, Schema schema) throws EDIStreamException;

    public abstract EDIStreamReader createEDIStreamReader(InputStream stream,
                                                          String encoding,
                                                          Schema schema) throws EDIStreamException;

    public abstract EDIStreamReader createFilteredReader(EDIStreamReader reader,
                                                         EDIStreamFilter filter) throws EDIStreamException;

    /**
     * Query the set of properties that this factory supports.
     *
     * @param name
     *            - The name of the property (may not be null)
     * @return true if the property is supported and false otherwise
     */
    public abstract boolean isPropertySupported(String name);

    /**
     * Get the value of a feature/property from the underlying implementation
     *
     * @param name
     *            - The name of the property (may not be null)
     * @return The value of the property
     * @throws IllegalArgumentException
     *             if the property is not supported
     */
    public abstract Object getProperty(String name);

    /**
     * Allows the user to set specific feature/property on the underlying
     * implementation. The underlying implementation is not required to support
     * every setting of every property in the specification and may use
     * IllegalArgumentException to signal that an unsupported property may not
     * be set with the specified value.
     *
     * @param name
     *            - The name of the property (may not be null)
     * @param value
     *            - The value of the property
     * @throws IllegalArgumentException
     *             if the property is not supported
     */
    public abstract void setProperty(String name, Object value);

}
