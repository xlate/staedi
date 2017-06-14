package io.xlate.edi.schema;

public class EDISchemaFactoryConfigurationError extends Error {

	private static final long serialVersionUID = -7308441503194207012L;

	public EDISchemaFactoryConfigurationError() {
		super();
	}

	public EDISchemaFactoryConfigurationError(Exception cause) {
		super(cause);
	}

	public EDISchemaFactoryConfigurationError(String message, Exception cause) {
		super(message, cause);
	}

	public EDISchemaFactoryConfigurationError(String message) {
		super(message);
	}
}
