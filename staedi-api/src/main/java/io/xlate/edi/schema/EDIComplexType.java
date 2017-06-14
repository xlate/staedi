package io.xlate.edi.schema;

import java.util.List;

public interface EDIComplexType extends EDIType {

	List<? extends EDIReference> getReferences();

	List<? extends EDISyntaxRule> getSyntaxRules();

	String getCode();
}
