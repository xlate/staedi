/*******************************************************************************
 * Copyright 2017 xlate.io LLC, http://www.xlate.io
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package io.xlate.edi.schema;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Structure extends BasicType implements EDIComplexType {

	private static final long serialVersionUID = -5094166920882750162L;

	private String code;
	private List<Reference> references;
	private List<SyntaxRestriction> syntaxRules;

	public Structure() {}

	Structure(String id, int type, String code, List<Reference> references, List<SyntaxRestriction> syntaxRules) {
		super(id, type);
		this.code = code;
		this.references = Collections.unmodifiableList(references);
		this.syntaxRules = Collections.unmodifiableList(syntaxRules);
	}

	Structure(EDIComplexType other, List<Reference> references, List<SyntaxRestriction> syntaxRules) {
		super(other);
		this.code = other.getCode();
		this.references = Collections.unmodifiableList(references);
		this.syntaxRules = Collections.unmodifiableList(syntaxRules);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		Externalizer.writeUTF(code, out);
		Externalizer.writeExternalizables(references, out);
		Externalizer.writeExternalizables(syntaxRules, out);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException {
		super.readExternal(in);
		code = Externalizer.readUTF(in);
		references = Externalizer.readExternalizables(Reference.class, ArrayList.class, in);
		syntaxRules = Externalizer.readExternalizables(SyntaxRestriction.class, ArrayList.class, in);
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder("id: ");
		buffer.append(super.id);
		buffer.append("\n, type: ");
		switch (super.type) {
		case TYPE_COMPOSITE:
			buffer.append("composite");
			break;
		case TYPE_LOOP:
			buffer.append("loop");
			break;
		case TYPE_SEGMENT:
			buffer.append("segment");
			break;
		}
		buffer.append("\n, code: ");
		buffer.append(code);
		buffer.append("\n, references: [");
		for (EDIReference reference : references) {
			buffer.append("\n\t");
			buffer.append(reference);
		}
		buffer.append("\n]\n, syntaxRestrictions: ");
		for (EDISyntaxRule rule: syntaxRules) {
			buffer.append("\n\t");
			buffer.append(rule);
		}
		buffer.append("\n]\n");
		return buffer.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (!super.equals(other)) {
			return false;
		}

		Structure otherStructure = (Structure) other;

		if (code == null) {
			if (otherStructure.code != null) {
				return false;
			}
		} else if (!code.equals(otherStructure.code)) {
			return false;
		}

		if (references == null) {
			if (otherStructure.references != null) {
				return false;
			}
		} else if (!references.equals(otherStructure.references)) {
			return false;
		}

		if (syntaxRules == null) {
			if (otherStructure.syntaxRules != null) {
				return false;
			}
		} else if (!syntaxRules.equals(otherStructure.syntaxRules)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String getCode() {
		return code;
	}

	@Override
	public List<? extends EDIReference> getReferences() {
		return references;
	}

	@Override
	public List<? extends EDISyntaxRule> getSyntaxRules() {
		return syntaxRules;
	}
}
