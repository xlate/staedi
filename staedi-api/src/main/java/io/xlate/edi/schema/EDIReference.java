package io.xlate.edi.schema;

public interface EDIReference {

	EDIType getReferencedType();

	int getMinOccurs();

	int getMaxOccurs();

}
