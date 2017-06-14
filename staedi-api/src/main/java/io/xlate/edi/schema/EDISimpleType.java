package io.xlate.edi.schema;

import java.util.Set;

public interface EDISimpleType extends EDIType {

	public static final int BASE_STRING = 1;
	public static final int BASE_INTEGER = 2;
	public static final int BASE_DECIMAL = 3;
	public static final int BASE_DATE = 4;
	public static final int BASE_TIME = 5;
	public static final int BASE_BINARY = 6;
	public static final int BASE_IDENTIFIER = 7;

	int getBaseCode();
	int getNumber();
	int getMinLength();
	int getMaxLength();
	Set<String> getValueSet();

}
