package io.xlate.edi.stream;

public class EDIFactoryConfigurationError extends Error {

	private static final long serialVersionUID = -7308441503194207012L;

	public EDIFactoryConfigurationError() {
		super();
	}

	public EDIFactoryConfigurationError(Exception cause) {
		super(cause);
	}

	public EDIFactoryConfigurationError(String message, Exception cause) {
		super(message, cause);
	}

	public EDIFactoryConfigurationError(String message) {
		super(message);
	}
}
