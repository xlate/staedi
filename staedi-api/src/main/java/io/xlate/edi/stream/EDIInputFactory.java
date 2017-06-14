package io.xlate.edi.stream;

import io.xlate.edi.schema.Schema;

import java.io.InputStream;

public abstract class EDIInputFactory {

	static final String FACTORY_ID = "io.xlate.edi.stream.EDIInputFactory";
	static final String DEFAULT_IMPL = "io.xlate.edi.stream.StaEDIInputFactory";

	public static EDIInputFactory newFactory() throws EDIFactoryConfigurationError {
		return FactoryFinder.find(FACTORY_ID, DEFAULT_IMPL);
	}

	public static EDIInputFactory newFactory(String factoryId,
			ClassLoader classLoader) throws EDIFactoryConfigurationError {

		try {
			return FactoryFinder.newInstance(factoryId, classLoader, false);
		} catch (FactoryFinder.ConfigurationError e) {
			String message = e.getMessage();
			Exception cause = e.getException();
			throw new EDIFactoryConfigurationError(message, cause);
		}
	}

	public abstract EDIStreamReader createEDIStreamReader(InputStream stream)
			throws EDIStreamException;

	public abstract EDIStreamReader createEDIStreamReader(InputStream stream,
			String encoding) throws EDIStreamException;

	public abstract EDIStreamReader createEDIStreamReader(InputStream stream,
			Schema schema) throws EDIStreamException;

	public abstract EDIStreamReader createEDIStreamReader(InputStream stream,
			String encoding, Schema schema) throws EDIStreamException;

	public abstract EDIStreamReader createFilteredReader(
			EDIStreamReader reader, EDIStreamFilter filter)
			throws EDIStreamException;

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
	public abstract Object getProperty(String name)
			throws IllegalArgumentException;

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
