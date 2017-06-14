package io.xlate.edi.stream;

import io.xlate.edi.schema.Schema;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StaEDIInputFactory extends EDIInputFactory {

	private static final String DEFAULT_ENCODING = "US-ASCII";

	private final Set<String> supportedCharsets;
	private final Map<String, Object> properties;
	private final Set<String> supportedProperties;

	public StaEDIInputFactory() {
		properties = new HashMap<>();
		supportedProperties = new HashSet<>();
		supportedCharsets = new HashSet<>();
		supportedCharsets.add(DEFAULT_ENCODING);
		//supportedCharsets.add("UTF-8");
	}

	@Override
	public EDIStreamReader createEDIStreamReader(InputStream stream)
			throws EDIStreamException {
		return new StaEDIStreamReader(stream, DEFAULT_ENCODING, properties);
	}

	@Override
	public EDIStreamReader createEDIStreamReader(InputStream stream,
			String encoding) throws EDIStreamException {
		if (!supportedCharsets.contains(encoding)) {
			throw new EDIStreamException("Unsupported encoding: " + encoding);
		}
		return new StaEDIStreamReader(stream, encoding, properties);
	}

	@Override
	public EDIStreamReader createEDIStreamReader(InputStream stream,
			Schema schema) throws EDIStreamException {
		return new StaEDIStreamReader(stream, DEFAULT_ENCODING, schema, properties);
	}

	@Override
	public EDIStreamReader createEDIStreamReader(InputStream stream,
			String encoding, Schema schema) throws EDIStreamException {
		if (!supportedCharsets.contains(encoding)) {
			throw new EDIStreamException("Unsupported encoding: " + encoding);
		}
		return new StaEDIStreamReader(stream, encoding, schema, properties);
	}

	@Override
	public EDIStreamReader createFilteredReader(EDIStreamReader reader,
			EDIStreamFilter filter) throws EDIStreamException {
		return new StaEDIFilteredStreamReader(reader, filter);
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

		properties.put(name, value);
	}
}
