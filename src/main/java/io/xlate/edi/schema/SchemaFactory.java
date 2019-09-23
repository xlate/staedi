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
package io.xlate.edi.schema;

import java.io.InputStream;
import java.net.URL;

import io.xlate.edi.internal.schema.StaEDISchemaFactory;

public abstract class SchemaFactory {

    /**
     * Create a new instance of the factory. This static method creates a new
     * factory instance.
     *
     * Once an application has obtained a reference to an EDIOutputFactory it
     * can use the factory to configure and obtain stream instances.
     *
     * @return the factory implementation
     */
    public static SchemaFactory newFactory() {
        return new StaEDISchemaFactory();
    }

    public abstract Schema createSchema(URL location) throws EDISchemaException;

    public abstract Schema createSchema(InputStream stream) throws EDISchemaException;

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
