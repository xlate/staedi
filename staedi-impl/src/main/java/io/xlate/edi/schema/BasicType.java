package io.xlate.edi.schema;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

abstract class BasicType implements EDIType, Externalizable {

	private static final long serialVersionUID = 4040260273915437397L;
	protected String id;
	protected int type;

	BasicType() {}

	BasicType(String id, int type) {
		super();
		this.id = id;
		this.type = type;
	}

	BasicType(EDIType other) {
		super();
		this.id = other.getId();
		this.type = other.getTypeCode();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		Externalizer.writeUTF(id, out);
		out.writeInt(type);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException {
		id = Externalizer.readUTF(in);
		type = in.readInt();
	}

	@Override
	public int compareTo(EDIType other) {
		if (type < other.getTypeCode()) {
			return -1;
		}

		if (type > other.getTypeCode()) {
			return +1;
		}

		return id.compareTo(other.getId());
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public int getTypeCode() {
		return type;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		if (other == null) {
			return false;
		}

		if (getClass() != other.getClass()) {
			return false;
		}

		BasicType otherType = (BasicType) other;

		if (this.type != otherType.type) {
			return false;
		}

		return this.id.equals(otherType.id);
	}

	@Override
	public abstract int hashCode();
}
