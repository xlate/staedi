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
package io.xlate.edi.internal.schema;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.EDIType.Type;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.implementation.LoopImplementation;

public class StaEDISchema implements Schema {

    public static final String ID_PREFIX = "io.xlate.edi.internal.schema.";

    public static final String INTERCHANGE_ID = ID_PREFIX + Type.INTERCHANGE.name();
    public static final String GROUP_ID = ID_PREFIX + Type.GROUP.name();
    public static final String TRANSACTION_ID = ID_PREFIX + Type.TRANSACTION.name();
    public static final String IMPLEMENTATION_ID = ID_PREFIX + "IMPLEMENTATION";

    public static final String ANY_ELEMENT_ID = ID_PREFIX + "ANY_ELEMENT";
    public static final String ANY_COMPOSITE_ID = ID_PREFIX + "ANY_COMPOSITE";

    private volatile Integer hash = null;

    final String interchangeName;
    final String transactionStandardName;
    final String implementationName;
    Map<String, EDIType> types = Collections.emptyMap();
    EDIComplexType standardLoop = null;
    LoopImplementation implementationLoop = null;

    public StaEDISchema(String interchangeName, String transactionStandardName, String implementationName) {
        super();
        this.interchangeName = interchangeName;
        this.transactionStandardName = transactionStandardName;
        this.implementationName = implementationName;
    }

    public StaEDISchema(String interchangeName, String transactionStandardName) {
        this(interchangeName, transactionStandardName, null);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Schema) {
            Schema other = (Schema) o;

            // Count the differences of each entry
            return StreamSupport.stream(spliterator(), false)
                                .filter(type -> {
                                    final EDIType otherType = other.getType(type.getId());
                                    return !type.equals(otherType);
                                })
                                .count() == 0;
        }

        return false;
    }

    @Override
    public synchronized int hashCode() {
        Integer localHash = hash;

        if (localHash == null) {
            localHash = hash = StreamSupport.stream(spliterator(), false)
                                            .collect(Collectors.summingInt(EDIType::hashCode));
        }

        return localHash.intValue();
    }

    @Override
    public EDIComplexType getMainLoop() {
        return getStandard();
    }

    @Override
    public EDIComplexType getStandard() {
        return standardLoop;
    }

    @Override
    public LoopImplementation getImplementation() {
        return implementationLoop;
    }

    void setTypes(Map<String, EDIType> types) throws EDISchemaException {
        if (types == null) {
            throw new NullPointerException("types cannot be null");
        }

        this.types = Collections.unmodifiableMap(types);

        if (types.containsKey(interchangeName)) {
            this.standardLoop = (EDIComplexType) types.get(interchangeName);
        } else if (types.containsKey(transactionStandardName)) {
            this.standardLoop = (EDIComplexType) types.get(transactionStandardName);
        } else {
            throw new EDISchemaException("Schema must contain either " +
                    interchangeName + " or " + transactionStandardName);
        }

        if (implementationName != null && types.containsKey(implementationName)) {
            this.implementationLoop = (LoopImplementation) types.get(implementationName);
        }
    }

    @Override
    public EDIType getType(String name) {
        return types.get(name);
    }

    @Override
    public boolean containsSegment(String name) {
        final EDIType type = getType(name);
        return type != null && type.isType(EDIType.Type.SEGMENT);
    }

    @Override
    public Iterator<EDIType> iterator() {
        return types.values().iterator();
    }
}
