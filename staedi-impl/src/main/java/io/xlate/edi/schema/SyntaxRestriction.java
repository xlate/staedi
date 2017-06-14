package io.xlate.edi.schema;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

class SyntaxRestriction implements EDISyntaxRule, Externalizable {

	private static final long serialVersionUID = 6822195445310567196L;
	private int type;
	private List<Integer> positions;

	public SyntaxRestriction() {}

	SyntaxRestriction(int type, List<Integer> positions) {
		super();
		this.type = type;
		this.positions = new ArrayList<>(positions);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(type);
		out.writeInt(positions.size());

		for (int position : positions) {
			out.writeInt(position);
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException {
		type = in.readInt();
		int positionCount = in.readInt();
		positions = new ArrayList<>(positionCount);

		for (int i = 0; i < positionCount; i++) {
			positions.add(in.readInt());
		}
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder("type: ");
		switch (type) {
		case SYNTAX_CONDITIONAL:
			buffer.append("conditional");
			break;
		case SYNTAX_EXCLUSION:
			buffer.append("exclusion");
			break;
		case SYNTAX_LIST:
			buffer.append("list");
			break;
		case SYNTAX_PAIRED:
			buffer.append("paired");
			break;
		case SYNTAX_REQUIRED:
			buffer.append("required");
			break;
		}
		buffer.append(", positions: ");
		buffer.append(positions);
		return buffer.toString();
	}

	@Override
	public int getType() {
		return type;
	}

	@Override
	public List<Integer> getPositions() {
		return positions;
	}
}
