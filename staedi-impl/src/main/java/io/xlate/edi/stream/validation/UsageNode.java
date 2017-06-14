package io.xlate.edi.stream.validation;

import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.schema.EDIType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class UsageNode {

	private final EDIReference link;
	private final ElementValidator validator;
	private final UsageNode parent;
	private int siblingIndex;
	private final List<UsageNode> children = new ArrayList<>();
	private int usageCount;

	UsageNode(UsageNode parent, EDIReference link, int siblingIndex) {
		if (link == null) {
			throw new NullPointerException();
		}

		this.parent = parent;
		this.link = link;

		EDIType referencedType = link.getReferencedType();

		if (referencedType instanceof EDISimpleType) {
			final EDISimpleType simple = (EDISimpleType) referencedType;
			final int baseCode = simple.getBaseCode();
			this.validator = ElementValidator.getInstance(baseCode);
		} else {
			this.validator = null;
		}

		this.siblingIndex = siblingIndex;
	}

	EDIReference getReference() {
		return link;
	}

	EDIType getReferencedType() {
		return link.getReferencedType();
	}

	UsageNode getParent() {
		return parent;
	}

	List<UsageNode> getChildren() {
		return children;
	}

	UsageNode getChild(int index) {
		return (index < children.size()) ? children.get(index) : null;
	}

	String getId() {
		return link.getReferencedType().getId();
	}

	String getCode() {
		EDIType referencedNode = link.getReferencedType();

		if (referencedNode instanceof EDIComplexType) {
			return ((EDIComplexType) referencedNode).getCode();
		}

		return null;
	}

	int getNumber() {
		EDIType referencedNode = link.getReferencedType();

		if (referencedNode instanceof EDISimpleType) {
			return ((EDISimpleType) referencedNode).getNumber();
		}

		return -1;
	}

	void validate(CharSequence value, List<Integer> errors) {
		if (validator == null) {
			throw new UnsupportedOperationException("simple type only");
		}

		final EDISimpleType element = (EDISimpleType) link.getReferencedType();
		validator.validate(element, value, errors);
	}

	List<? extends EDISyntaxRule> getSyntaxRules() {
		EDIType referencedNode = link.getReferencedType();

		if (referencedNode instanceof EDIComplexType) {
			return ((EDIComplexType) referencedNode).getSyntaxRules();
		}

		return Collections.emptyList();
	}

	int getIndex() {
		return siblingIndex;
	}

	void setIndex(int siblingIndex) {
		this.siblingIndex = siblingIndex;
	}

	void incrementUsage() {
		usageCount++;
	}

	boolean isUsed() {
		return usageCount > 0;
	}

	boolean isFirstChild() {
		return this == getFirstSibling();
	}

	boolean hasMinimumUsage() {
		return usageCount >= link.getMinOccurs();
	}

	boolean exceedsMaximumUsage() {
		return usageCount > link.getMaxOccurs();
	}

	int getUsageCount() {
		return usageCount;
	}

	boolean isNodeType(int type) {
		return link.getReferencedType().getTypeCode() == type;
	}

	int getNodeType() {
		return link.getReferencedType().getTypeCode();
	}

	void reset() {
		usageCount = 0;
		resetChildren();
	}

	void resetChildren() {
		for (UsageNode child : children) {
			child.reset();
		}
	}

	private UsageNode getSibling(int index) {
		return parent != null && parent.children.size() > index
				? parent.children.get(index)
				: null;
	}

	UsageNode getFirstSibling() {
		return getSibling(0);
	}

	UsageNode getNextSibling() {
		return getSibling(siblingIndex + 1);
	}

	public UsageNode getFirstChild() {
		return (children.size() > 0) ? children.get(0) : null;
	}

	UsageNode getChildById(CharSequence id) {
		for (UsageNode child : children) {
			if (child.getId().contentEquals(id)) {
				return child;
			}
		}
		return null;
	}

	UsageNode getSiblingById(CharSequence id) {
		return parent != null ? parent.getChildById(id) : null;
	}
}
