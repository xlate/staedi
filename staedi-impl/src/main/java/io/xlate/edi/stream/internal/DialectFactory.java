package io.xlate.edi.stream.internal;

public abstract class DialectFactory {

	private enum Dialects {
		X12("ISA", X12Dialect.class),
		EDIFACT_A("UNA", EDIFACTDialect.class),
		EDIFACT_B("UNB", EDIFACTDialect.class);

		private String tag;
		private Class<? extends Dialect> clazz;

		Dialects(String tag, Class<? extends Dialect> clazz) {
			this.tag = tag;
			this.clazz = clazz;
		}

		public static Class<? extends Dialect> forTag(String tag) {
			for (Dialects d : Dialects.values()) {
				if (d.tag.equals(tag)) {
					return d.clazz;
				}
			}
			return null;
		}
	}

	public static Dialect getDialect(char[] buffer, int start, int length) throws EDIException {
		String tag = new String(buffer, start, length);
		return getDialect(tag);
	}

	public static Dialect getDialect(String tag) throws EDIException {
		Class<? extends Dialect> type = Dialects.forTag(tag);

		if (type != null) {
			try {
				Dialect dialect = type.newInstance();
				dialect.setHeaderTag(tag);

				return dialect;
			} catch (InstantiationException e) {
				throw new EDIException(e);
			} catch (IllegalAccessException e) {
				throw new EDIException(e);
			}
		}
		throw new EDIException(EDIException.UNSUPPORTED_DIALECT, tag);
	}
}
