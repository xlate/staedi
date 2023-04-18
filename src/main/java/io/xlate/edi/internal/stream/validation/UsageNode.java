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
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
        Objects.requireNonNull(link, "link");
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

    public static boolean hasMinimumUsage(String version, UsageNode node) {
        return node == null || node.hasMinimumUsage(version);
    }

    public static UsageNode getFirstChild(UsageNode node) {
        return node != null ? node.getFirstChild() : null;
    }

    static <I, T, R> R withTypeOrElseGet(I reference, Class<T> type, Function<T, R> mapper, Supplier<R> defaultValue) {
        if (type.isInstance(reference)) {
            return mapper.apply(type.cast(reference));
        }
        return defaultValue.get();
    }

    public UsageNode getFirstSiblingSameType() {
        EDIType type = getReferencedType();
        UsageNode sibling = getFirstSibling();

        while (!type.equals(sibling.getReferencedType())) {
            sibling = sibling.getNextSibling();
        }

        return sibling;
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

    List<UsageNode> getChildren(String version) {
        return children.stream().filter(c -> c == null || c.link.getMaxOccurs(version) > 0).collect(Collectors.toList());
    }

    UsageNode getChild(String version, int index) {
        final List<UsageNode> versionedChildren = getChildren(version);
        return (index < versionedChildren.size()) ? versionedChildren.get(index) : null;
    }

    boolean isImplementation() {
        return (link instanceof EDITypeImplementation);
    }

    String getId() {
        return withTypeOrElseGet(link, EDITypeImplementation.class, EDITypeImplementation::getId, link.getReferencedType()::getId);
    }

    EDISimpleType getSimpleType() {
        return withTypeOrElseGet(link, EDISimpleType.class, EDISimpleType.class::cast, () -> (EDISimpleType) link.getReferencedType());
    }

    void validate(Dialect dialect, CharSequence value, List<EDIStreamValidationError> errors) {
        validator.validate(dialect, getSimpleType(), value, errors);
    }

    void format(Dialect dialect, CharSequence value, StringBuilder result) {
        validator.format(dialect, getSimpleType(), value, result);
    }

    List<EDISyntaxRule> getSyntaxRules() {
        EDIType referencedNode = link.getReferencedType();
        return withTypeOrElseGet(referencedNode, EDIComplexType.class, EDIComplexType::getSyntaxRules, Collections::emptyList);
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

    boolean hasMinimumUsage(String version) {
        return usageCount >= link.getMinOccurs(version);
    }

    boolean hasVersions() {
        return getSimpleType().hasVersions();
    }

    boolean exceedsMaximumUsage(String version) {
        return usageCount > link.getMaxOccurs(version);
    }

    boolean isNodeType(EDIType.Type... types) {
        for (EDIType.Type type : types) {
            if (link.getReferencedType().isType(type)) {
                return true;
            }
        }
        return false;
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
        return parent != null ? parent.getChild(index) : null;
    }

    UsageNode getFirstSibling() {
        return getSibling(0);
    }

    UsageNode getNextSibling() {
        return getSibling(siblingIndex + 1);
    }

    public UsageNode getFirstChild() {
        return getChild(0);
    }

    private UsageNode getChildById(CharSequence id) {
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
