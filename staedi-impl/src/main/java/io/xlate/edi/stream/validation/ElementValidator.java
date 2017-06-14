package io.xlate.edi.stream.validation;

import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.stream.EDIStreamConstants.ElementDataErrors;
import io.xlate.edi.stream.internal.EDIException;

import java.util.List;

abstract class ElementValidator implements ElementDataErrors {

	static ElementValidator getInstance(int type) {
		switch (type) {
		case EDISimpleType.BASE_IDENTIFIER:
		case EDISimpleType.BASE_STRING:
			return AlphaNumericValidator.getInstance();
		case EDISimpleType.BASE_INTEGER:
			return NumericValidator.getInstance();
		case EDISimpleType.BASE_DECIMAL:
			return DecimalValidator.getInstance();
		case EDISimpleType.BASE_DATE:
			return DateValidator.getInstance();
		case EDISimpleType.BASE_TIME:
			return TimeValidator.getInstance();
		case EDISimpleType.BASE_BINARY:
			return null;
		default:
			throw new IllegalArgumentException("Illegal type " + type);
		}
	}

	protected static boolean validLength(
			EDISimpleType element,
			int length,
			List<Integer> errors) {

		if (length > element.getMaxLength()) {
			errors.add(DATA_ELEMENT_TOO_LONG);
			return false;
		} else if (length < element.getMinLength()) {
			errors.add(DATA_ELEMENT_TOO_SHORT);
			return false;
		}

		return true;
	}

	protected static void checkLength(EDISimpleType element, int length)
			throws EDIException {

		if (length > element.getMaxLength()) {
			// DATA_ELEMENT_TOO_LONG
			throw new EDIException();
		}
	}

	abstract void validate(
			EDISimpleType element,
			CharSequence value,
			List<Integer> errors);

	abstract void format(
			EDISimpleType element,
			CharSequence value,
			Appendable result) throws EDIException;
}
