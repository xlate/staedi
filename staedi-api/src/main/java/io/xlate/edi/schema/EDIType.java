package io.xlate.edi.schema;

import java.io.Serializable;


public interface EDIType extends Comparable<EDIType>, Serializable {

	public static final int TYPE_LOOP = 2;
	public static final int TYPE_SEGMENT = 3;
	public static final int TYPE_COMPOSITE = 4;
	public static final int TYPE_ELEMENT = 5;

	String getId();

	int getTypeCode();

}
