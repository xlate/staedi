package io.xlate.edi.stream.validation;

class DecimalValidator extends NumericValidator {

	private static final DecimalValidator singleton = new DecimalValidator();

	private DecimalValidator() {}

	static DecimalValidator getInstance() {
		return singleton;
	}

	@Override
	protected int validate(CharSequence value) {
		int length = value.length();

		int dec = 0;
		int exp = 0;
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

			case '.':
				length--;

				if (++dec > 1 || exp > 0) {
					invalid = true;
				}
				break;

			case 'E':
				length--;

				if (++exp > 1) {
					invalid = true;
				}
				break;

			case '-':
				length--;

				if (i > 0) {
					if (value.charAt(i - 1) != 'E') {
						invalid = true;
					}
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
