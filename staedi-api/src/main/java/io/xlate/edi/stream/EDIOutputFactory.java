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
package io.xlate.edi.stream;

import java.io.OutputStream;

public abstract class EDIOutputFactory {

	/*static final char DFLT_SEGMENT_TERMINATOR = '~';
	static final char DFLT_DATA_ELEMENT_SEPARATOR = '*';
	static final char DFLT_COMPONENT_ELEMENT_SEPARATOR = ':';
	static final char DFLT_REPETITION_SEPARATOR = '^';*/

	static final String FACTORY_ID = "io.xlate.edi.stream.EDIOutputFactory";
	static final String DEFAULT_IMPL = "io.xlate.edi.stream.StaEDIOutputFactory";

	/**
	 * Create a new instance of the factory. This static method creates a new
	 * factory instance. This method uses the following ordered lookup procedure
	 * to determine the EDIOutputFactory implementation class to load:
	 *
	 * <ol>
	 * <li>
	 * Use the io.xlate.edi.stream.EDIOutputFactory system property.
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
	 * @throws EDIFactoryConfigurationError
	 *             if an instance of this factory cannot be loaded
	 */
	public static EDIOutputFactory newFactory()
			throws EDIFactoryConfigurationError {
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
	 * @throws EDIFactoryConfigurationError
	 *             if an instance of this factory cannot be loaded
	 */
	public static EDIOutputFactory newFactory(String factoryId,
			ClassLoader classLoader) throws EDIFactoryConfigurationError {

		try {
			return FactoryFinder.newInstance(factoryId, classLoader, false);
		} catch (FactoryFinder.ConfigurationError e) {
			String message = e.getMessage();
			Exception cause = e.getException();
			throw new EDIFactoryConfigurationError(message, cause);
		}
	}

	/**
	 * Create a new EDIStreamWriter that writes to a stream
	 *
	 * @param stream
	 *            - the stream to write to
	 * @return the writer instance
	 * @throws EDIStreamException
	 */
	public abstract EDIStreamWriter createEDIStreamWriter(OutputStream stream)
			throws EDIStreamException;

	/**
	 * Create a new EDIStreamWriter that writes to a stream
	 *
	 * @param stream
	 *            - the stream to write to
	 * @param encoding
	 *            - the encoding to use
	 * @return the writer instance
	 * @throws EDIStreamException
	 */
	public abstract EDIStreamWriter createEDIStreamWriter(OutputStream stream,
			String encoding) throws EDIStreamException;

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
	public abstract Object getProperty(String name) throws IllegalArgumentException;

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
	public abstract void setProperty(String name, Object value)
			throws IllegalArgumentException;
}
