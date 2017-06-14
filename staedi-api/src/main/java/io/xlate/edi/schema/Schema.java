package io.xlate.edi.schema;

import java.io.Serializable;

public abstract class Schema implements Iterable<EDIType>, Serializable {

	private static final long serialVersionUID = 6722413554348589041L;

	public abstract EDIComplexType getMainLoop();

	public abstract EDIType getType(String name);

	public abstract boolean containsSegment(String name);

	/**
	 * Attach a schema to this schema to be referenced by the parent type
	 * provided and inserted before the provided child reference type.
	 *
	 * Any type defined by the included schema that are already defined by this
	 * schema will be ignored.
	 *
	 * Neither this schema nor the included schema will be modified by this
	 * operation. This method will return a new schema object which represents
	 * the combination of the two.
	 *
	 * @param referenced
	 *            the schema to include
	 * @param parent
	 *            the parent type which will reference the root element of the
	 *            included schema
	 * @param child
	 *            the reference point prior to which the new schema's root type
	 *            will be referenced
	 * @return the new combined schema
	 */
	public abstract Schema reference(
			Schema referenced,
			EDIComplexType parent,
			EDIReference child) throws EDISchemaException;
}
