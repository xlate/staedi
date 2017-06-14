package io.xlate.edi.schema;

import java.util.List;

public interface EDISyntaxRule {

	public static final int SYNTAX_PAIRED = 1;
	public static final int SYNTAX_REQUIRED = 2;
	public static final int SYNTAX_EXCLUSION = 3;
	public static final int SYNTAX_CONDITIONAL = 4;
	public static final int SYNTAX_LIST = 5;

	int getType();

	List<Integer> getPositions();
}
