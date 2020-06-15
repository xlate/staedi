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
import java.util.Set;

import io.xlate.edi.internal.stream.tokenization.Dialect;
import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.implementation.EDITypeImplementation;
import io.xlate.edi.stream.EDIStreamValidationError;

class UsageNode {

    private static final String TOSTRING_FORMAT = "usageCount: %d, depth: %d, link: { %s }";

    private final UsageNode parent;
    private final int depth;
    private final EDIReference link;
    private final int siblingIndex;

    private final ElementValidator validator;
    private final List<UsageNode> children = new ArrayList<>();
    private int usageCount;

    UsageNode(UsageNode parent, int depth, EDIReference link, int siblingIndex) {
        if (link == null) {
            throw new NullPointerException();
        }

        this.parent = parent;
        this.depth = depth;
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

    public static boolean hasMinimumUsage(UsageNode node) {
        return node == null || node.hasMinimumUsage();
    }

    public static UsageNode getParent(UsageNode node) {
        return node != null ? node.getParent() : null;
    }

    public static UsageNode getFirstChild(UsageNode node) {
        return node != null ? node.getFirstChild() : null;
    }

    public static void resetChildren(UsageNode... nodes) {
        for (UsageNode node : nodes) {
            if (node != null) {
                node.resetChildren();
            }
        }
    }

    @Override
    public String toString() {
        return String.format(TOSTRING_FORMAT, usageCount, depth, link);
    }

    UsageNode getParent() {
        return parent;
    }

    int getDepth() {
        return depth;
    }

    EDIReference getLink() {
        return link;
    }

    EDIType getReferencedType() {
        return link.getReferencedType();
    }

    List<UsageNode> getChildren() {
        return children;
    }

    UsageNode getChild(int index) {
        return (index < children.size()) ? children.get(index) : null;
    }

    boolean isImplementation() {
        return (link instanceof EDITypeImplementation);
    }

    String getId() {
        if (link instanceof EDITypeImplementation) {
            return ((EDITypeImplementation) link).getId();
        }

        return link.getReferencedType().getId();
    }

    String getCode() {
        if (link instanceof EDITypeImplementation) {
            return ((EDITypeImplementation) link).getCode();
        }

        return link.getReferencedType().getCode();
    }

    EDISimpleType getSimpleType() {
        if (link instanceof EDISimpleType) {
            return (EDISimpleType) link;
        }
        return (EDISimpleType) link.getReferencedType();
    }

    void validate(Dialect dialect, CharSequence value, boolean validateCodeValues, List<EDIStreamValidationError> errors) {
        if (validator == null) {
            throw new UnsupportedOperationException("simple type only");
        }

        final EDISimpleType element = getSimpleType();

        if (validateCodeValues) {
            validator.validate(dialect, element, value, errors);
        } else {
            validator.validate(dialect, UnenumeratedElement.from(element), value, errors);
        }
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

    boolean hasVersions() {
        return getSimpleType().hasVersions();
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
        for (UsageNode node : children) {
            if (node != null) {
                node.reset();
            }
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
        return (!children.isEmpty()) ? children.get(0) : null;
    }

    UsageNode getChildById(CharSequence id) {
        return children.stream()
                       .filter(c -> c != null && c.getId().contentEquals(id))
                       .findFirst()
                       .orElse(null);
    }

    UsageNode getSiblingById(CharSequence id) {
        return parent != null ? parent.getChildById(id) : null;
    }

    private static class UnenumeratedElement implements EDISimpleType {
        final EDISimpleType target;

        static EDISimpleType from(EDISimpleType target) {
            return new UnenumeratedElement(target);
        }

        private UnenumeratedElement(EDISimpleType target) {
            this.target = target;
        }

        @Override
        public String getId() {
            return target.getId();
        }

        @Override
        public Base getBase() {
            return target.getBase();
        }

        @Override
        public String getCode() {
            return target.getCode();
        }

        @Override
        public Type getType() {
            return Type.ELEMENT;
        }

        /**
         * @see io.xlate.edi.schema.EDISimpleType#getNumber()
         * @deprecated
         */
        @SuppressWarnings({ "java:S1123", "java:S1133" })
        @Override
        @Deprecated
        public int getNumber() {
            return target.getNumber();
        }

        @Override
        public long getMinLength() {
            return target.getMinLength();
        }

        @Override
        public long getMinLength(String version) {
            return target.getMinLength(version);
        }

        @Override
        public long getMaxLength() {
            return target.getMaxLength();
        }

        @Override
        public long getMaxLength(String version) {
            return target.getMaxLength(version);
        }

        @Override
        public Set<String> getValueSet() {
            return Collections.emptySet();
        }
    }
}
