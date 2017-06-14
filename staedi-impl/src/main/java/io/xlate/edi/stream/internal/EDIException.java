package io.xlate.edi.stream.internal;

import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.Location;

import java.util.HashMap;
import java.util.Map;

public class EDIException extends EDIStreamException {

	private static final long serialVersionUID = -2724168743697298348L;

	static final Integer MISSING_HANDLER = 1;
	static final Integer UNSUPPORTED_DIALECT = 2;
	static final Integer INVALID_STATE = 3;
	static final Integer INVALID_CHARACTER = 4;

	private static Map<Integer, String> exceptionMessages =
			new HashMap<>();

	static {
		exceptionMessages.put(
				MISSING_HANDLER,
				"EDIE001 - Missing required handler");
		exceptionMessages.put(
				UNSUPPORTED_DIALECT,
				"EDIE002 - Unsupported EDI dialect");
		exceptionMessages.put(
				INVALID_STATE,
				"EDIE003 - Invalid processing state");
		exceptionMessages.put(
				INVALID_CHARACTER,
				"EDIE004 - Invalid input character");
	}

	public EDIException() {
		super();
	}

	public EDIException(String message, Throwable cause) {
		super(message, cause);
	}

	EDIException(Integer id, Throwable cause) {
		super(exceptionMessages.get(id), cause);
	}

	EDIException(Integer id, Throwable cause, String message) {
		super(exceptionMessages.get(id) + message, cause);
	}

	public EDIException(String message) {
		super(message);
	}

	EDIException(Integer id) {
		super(exceptionMessages.get(id));
	}

	EDIException(Integer id, Location location) {
		super(exceptionMessages.get(id), location);
	}

	EDIException(Integer id, String message, Location location) {
		super(exceptionMessages.get(id) + message, location);
	}

	EDIException(Integer id, String message) {
		super(exceptionMessages.get(id) + message);
	}

	public EDIException(Throwable cause) {
		super(cause);
	}

}
