package io.xlate.edi.schema;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

class Element extends BasicType implements EDISimpleType {

	private static final long serialVersionUID = 1678383730368794011L;

	private int base;
	private int number;
	private int minLength;
	private int maxLength;
	private Set<String> values;

	public Element() {}

	Element(String id, int base, int number, int minLength, int maxLength) {
		this(id, base, number, minLength, maxLength, Collections.emptySet());
	}

	Element(String id, int base, int number, int minLength, int maxLength, Set<String> values) {
		super(id, TYPE_ELEMENT);
		this.base = base;
		this.number = number;
		this.minLength = minLength;
		this.maxLength = maxLength;
		this.values = Collections.unmodifiableSet(values);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeInt(base);
		out.writeInt(number);
		out.writeInt(minLength);
		out.writeInt(maxLength);
		out.writeInt(values.size());
		for (String value : values) {
			Externalizer.writeUTF(value, out);
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException {
		super.readExternal(in);
		base = in.readInt();
		number = in.readInt();
		minLength = in.readInt();
		maxLength = in.readInt();

		int valueCount = in.readInt();
		values = new HashSet<>(valueCount);

		for (int i = 0; i < valueCount; i++) {
			values.add(Externalizer.readUTF(in));
		}
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder("id: ");
		buffer.append(super.id);
		buffer.append(", type: element");
		buffer.append(", base: ");
		switch (base) {
		case BASE_BINARY:
			buffer.append("binary");
			break;
		case BASE_DATE:
			buffer.append("date");
			break;
		case BASE_DECIMAL:
			buffer.append("decimal");
			break;
		case BASE_IDENTIFIER:
			buffer.append("identifier");
			break;
		case BASE_STRING:
			buffer.append("string");
			break;
		case BASE_TIME:
			buffer.append("time");
			break;
		}
		buffer.append(", number: ");
		buffer.append(number);
		buffer.append(", minLength: ");
		buffer.append(minLength);
		buffer.append(", maxLength: ");
		buffer.append(maxLength);
		buffer.append(", values: ");
		buffer.append(values);
		return buffer.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + number;
		result = prime * result + maxLength;
		result = prime * result + minLength;
		result = prime * result + ((values == null) ? 0 : values.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object other) {
		if (!super.equals(other)) {
			return false;
		}

		Element otherType = (Element) other;

		if (number != otherType.number) {
			return false;
		}
		if (maxLength != otherType.maxLength) {
			return false;
		}
		if (minLength != otherType.minLength) {
			return false;
		}
		if (values == null) {
			if (otherType.values != null) {
				return false;
			}
		} else if (!values.equals(otherType.values)) {
			return false;
		}
		return true;
	}

	@Override
	public int getBaseCode() {
		return base;
	}

	@Override
	public int getNumber() {
		return number;
	}

	@Override
	public int getMinLength() {
		return minLength;
	}

	@Override
	public int getMaxLength() {
		return maxLength;
	}

	@Override
	public Set<String> getValueSet() {
		return values;
	}
}
