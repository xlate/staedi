package io.xlate.edi.stream.internal;

public enum CharacterClass {

	/*
	 * Characters are mapped into these character classes. This allows for a
	 * significant reduction in the size of the state transition table.
	 */
	SPACE(0),
	LATIN_A(1),
	LATIN_B(2),
	LATIN_E(3),
	LATIN_I(4),
	LATIN_N(5),
	LATIN_S(6),
	LATIN_U(7),
	LATIN_Z(8),
	ALPHANUMERIC(9),
	SEGMENT_DELIMITER(10),
	ELEMENT_DELIMITER(11),
	COMPONENT_DELIMITER(12),
	ELEMENT_REPEATER(13),
	RELEASE_CHARACTER(14),

	WHITESPACE(15), /* Other white space */
	CONTROL(16), /* Control Characters */
	OTHER(17), /* Everything else */
	INVALID(18);

	protected final int code;

	private CharacterClass(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public boolean isValid() {
		return code > -1;
	}
}
