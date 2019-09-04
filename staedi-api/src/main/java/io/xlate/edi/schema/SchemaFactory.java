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

public abstract class SchemaFactory {

    static final String FACTORY_ID = "io.xlate.edi.schema.SchemaFactory";
    static final String DEFAULT_IMPL = "io.xlate.edi.internal.schema.StaEDISchemaFactory";

    /**
     * Create a new instance of the factory. This static method creates a new
     * factory instance. This method uses the following ordered lookup procedure
     * to determine the EDIOutputFactory implementation class to load:
     *
     * <ol>
     * <li>Use the io.xlate.edi.stream.EDIOutputFactory system property.
     * <li>Use the Services API (as detailed in the JAR specification), if
     * available, to determine the classname. The Services API will look for a
     * classname in the file
     * META-INF/services/io.xlate.edi.stream.EDIOutputFactory in jars available
     * to the runtime.
     * <li>Platform default EDIOutputFactory instance.
     * </ol>
     * Once an application has obtained a reference to an EDIOutputFactory it
     * can use the factory to configure and obtain stream instances.
     *
     * @return the factory implementation
     * @throws EDISchemaFactoryConfigurationError
     *             if an instance of this factory cannot be loaded
     */
    public static SchemaFactory newFactory() throws EDISchemaFactoryConfigurationError {
        return FactoryFinder.find(FACTORY_ID, DEFAULT_IMPL);
    }

    /**
     * Create a new instance of the factory. If the classLoader argument is
     * null, then the ContextClassLoader is used.
     *
     * @param factoryId
     *            - Name of the factory to find, same as a property name
     * @param classLoader
     *            - classLoader to use
     * @return the factory implementation
     * @throws EDISchemaFactoryConfigurationError
     *             if an instance of this factory cannot be loaded
     */
    public static SchemaFactory newFactory(String factoryId,
                                           ClassLoader classLoader) throws EDISchemaFactoryConfigurationError {
        return FactoryFinder.newInstance(factoryId, classLoader, false);
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
