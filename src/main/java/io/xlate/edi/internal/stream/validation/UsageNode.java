/*******************************************************************************
 * Copyright 2017 xlate.io LLC, http://www.xlate.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package io.xlate.edi.internal.stream.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.xlate.edi.internal.stream.tokenization.Dialect;
import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.implementation.EDITypeImplementation;
import io.xlate.edi.stream.EDIStreamValidationError;

class UsageNode {

    private static final String TOSTRING_FORMAT = "usageCount: %d, link: { %s }";

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
            this.validator = ElementValidator.getInstance(simple.getBase());
        } else {
            this.validator = null;
        }

        this.siblingIndex = siblingIndex;
    }

    @Override
    public String toString() {
        return String.format(TOSTRING_FORMAT, usageCount, link);
    }

    EDIReference getLink() {
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
        if (link instanceof EDITypeImplementation) {
            return ((EDITypeImplementation) link).getId();
        }

        return link.getReferencedType().getId();
    }

    String getCode() {
        if (link instanceof EDITypeImplementation) {
            return ((EDITypeImplementation) link).getId();
        }

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

    void validate(Dialect dialect, CharSequence value, List<EDIStreamValidationError> errors) {
        if (validator == null) {
            throw new UnsupportedOperationException("simple type only");
        }

        final EDISimpleType element = (EDISimpleType) link.getReferencedType();
        validator.validate(dialect, element, value, errors);
    }

    List<EDISyntaxRule> getSyntaxRules() {
        EDIType referencedNode = link.getReferencedType();

        if (referencedNode instanceof EDIComplexType) {
            return ((EDIComplexType) referencedNode).getSyntaxRules();
        }

        return Collections.emptyList();
    }

    int getIndex() {
        return siblingIndex;
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

    boolean isNodeType(EDIType.Type type) {
        return link.getReferencedType().isType(type);
    }

    EDIType.Type getNodeType() {
        return link.getReferencedType().getType();
    }

    void reset() {
        usageCount = 0;
        resetChildren();
    }

    void resetChildren() {
        children.forEach(UsageNode::reset);
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
        return (!children.isEmpty()) ? children.get(0) : null;
    }

    UsageNode getChildById(CharSequence id) {
        return children.stream()
                       .filter(c -> c.getId().contentEquals(id))
                       .findFirst()
                       .orElse(null);
    }

    UsageNode getSiblingById(CharSequence id) {
        return parent != null ? parent.getChildById(id) : null;
    }
}
