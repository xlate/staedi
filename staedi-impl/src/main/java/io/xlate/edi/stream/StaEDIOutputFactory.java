package io.xlate.edi.stream;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StaEDIOutputFactory extends EDIOutputFactory {

	public static final String PRETTY_PRINT = "io.xlate.edi.stream.prettyPrint";

	private final Set<String> supportedCharsets;
	private final Set<String> supportedProperties;
	private final Map<String, Object> properties;

	public StaEDIOutputFactory() {
		supportedCharsets = new HashSet<>();
		supportedCharsets.add("US-ASCII");

		supportedProperties = new HashSet<>();
		supportedProperties.add(EDIStreamConstants.Delimiters.SEGMENT);
		supportedProperties.add(EDIStreamConstants.Delimiters.DATA_ELEMENT);
		supportedProperties.add(EDIStreamConstants.Delimiters.COMPONENT_ELEMENT);
		supportedProperties.add(EDIStreamConstants.Delimiters.REPETITION);
		supportedProperties.add(EDIStreamConstants.Delimiters.RELEASE);

		supportedProperties.add(PRETTY_PRINT);

		properties = new HashMap<>();
		properties.put(PRETTY_PRINT, Boolean.FALSE);
	}

	@Override
	public EDIStreamWriter createEDIStreamWriter(OutputStream stream)
			throws EDIStreamException {
		return new StaEDIStreamWriter(stream, properties);
	}

	@Override
	public EDIStreamWriter createEDIStreamWriter(OutputStream stream,
			String encoding) throws EDIStreamException {
		if (!supportedCharsets.contains(encoding)) {
			throw new EDIStreamException("Unsupported encoding: " + encoding);
		}
		return new StaEDIStreamWriter(stream, encoding, properties);
	}

	@Override
	public boolean isPropertySupported(String name) {
		return supportedProperties.contains(name);
	}

	@Override
	public Object getProperty(String name) throws IllegalArgumentException {
		if (!isPropertySupported(name)) {
			throw new IllegalArgumentException("Unsupported property: " + name);
		}

		return properties.get(name);
	}

	@Override
	public void setProperty(String name, Object value)
			throws IllegalArgumentException {

		if (!isPropertySupported(name)) {
			throw new IllegalArgumentException("Unsupported property: " + name);
		}

		if (PRETTY_PRINT.equals(name)) {
			if (!(value instanceof Boolean)) {
				throw new IllegalArgumentException(name + " must be Boolean");
			}
		}

		properties.put(name, value);
	}
}
