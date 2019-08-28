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

class Reference implements EDIReference {

	private String refId;
	private String refTag;
	private EDIType referencedType;
	private int minOccurs;
	private int maxOccurs;
	private Schema schema;

	Reference(String refId, String refTag, int minOccurs, int maxOccurs) {
		this.refId = refId;
		this.refTag = refTag;
		this.minOccurs = minOccurs;
		this.maxOccurs = maxOccurs;
	}

	Reference(EDIType referencedType, int minOccurs, int maxOccurs) {
		this.refId = referencedType.getId();
		this.refTag = null;
		this.referencedType = referencedType;
		this.minOccurs = minOccurs;
		this.maxOccurs = maxOccurs;
	}

	Reference(EDIReference other) {
		this.referencedType = other.getReferencedType();

		if (other instanceof Reference) {
			this.refId = ((Reference) other).getRefId();
		} else {
			this.refId = referencedType.getId();
		}

		this.refTag = null;
		this.minOccurs = other.getMinOccurs();
		this.maxOccurs = other.getMaxOccurs();
	}

	@Override
	public String toString() {
		return "refId: "
				+ refId
				+ ", minOccurs: "
				+ minOccurs
				+ ", maxOccurs: "
				+ maxOccurs;
	}

	String getRefId() {
		return refId;
	}

	String getRefTag() {
		return refTag;
	}

	@Override
	public EDIType getReferencedType() {
		if (referencedType == null && schema != null) {
			setReferencedType(schema.getType(refId));
		}

		return referencedType;
	}

	void setReferencedType(EDIType referencedType) {
		this.referencedType = referencedType;
	}

	@Override
	public int getMinOccurs() {
		return minOccurs;
	}

	@Override
	public int getMaxOccurs() {
		return maxOccurs;
	}

	void setSchema(Schema schema) {
		this.schema = schema;
	}
}
