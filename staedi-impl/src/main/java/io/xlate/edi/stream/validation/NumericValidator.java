package io.xlate.edi.stream.validation;

import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.stream.internal.EDIException;

import java.io.IOException;
import java.util.List;

class NumericValidator extends ElementValidator {

	private static final NumericValidator singleton = new NumericValidator();

	protected NumericValidator() {}

	static NumericValidator getInstance() {
		return singleton;
	}

	@Override
	void validate(EDISimpleType element, CharSequence value, List<Integer> errors) {
		int length = validate(value);
		super.validLength(element, Math.abs(length), errors);

		if (length < 0) {
			errors.add(INVALID_CHARACTER_DATA);
		}
	}

	@Override
	void format(EDISimpleType element, CharSequence value, Appendable result)
			throws EDIException {
		int length = validate(value);
		super.checkLength(element, Math.abs(length));

		if (length < 0) {
			// TODO: INVALID_CHARACTER_DATA
			throw new EDIException();
		}

		for (int i = length, min = element.getMinLength(); i < min; i++) {
			try {
				result.append('0');
			} catch (IOException e) {
				throw new EDIException(e);
			}
		}

		try {
			result.append(value);
		} catch (IOException e) {
			throw new EDIException(e);
		}
	}

	@SuppressWarnings("static-method")
	protected int validate(CharSequence value) {
		int length = value.length();
		boolean invalid = false;

		for (int i = 0, m = length; i < m; i++) {
			switch (value.charAt(i)) {
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				break;
			case '-':
				length--;

				if (i > 0) {
					invalid = true;
				}
				break;

			default:
				invalid = true;
				break;
			}
		}

		return invalid ? -length : length;
	}
}
