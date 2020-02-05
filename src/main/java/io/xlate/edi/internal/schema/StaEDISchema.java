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

import static io.xlate.edi.internal.schema.StaEDISchemaFactory.QN_INTERCHANGE;
import static io.xlate.edi.internal.schema.StaEDISchemaFactory.QN_TRANSACTION;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.Schema;

class StaEDISchema implements Schema {

    static final String INTERCHANGE = QN_INTERCHANGE.toString();
    static final String TRANSACTION = QN_TRANSACTION.toString();

    private Map<String, EDIType> types = Collections.emptyMap();
    private EDIComplexType standardLoop = null;

    @Override
    public EDIComplexType getMainLoop() {
        return getStandard();
    }

    @Override
    public EDIComplexType getStandard() {
        return standardLoop;
    }

    @Override
    public EDIComplexType getImplementation() {
        return null;
    }

    void setTypes(Map<String, EDIType> types) throws EDISchemaException {
        if (types == null) {
            throw new NullPointerException("types cannot be null");
        }

        this.types = Collections.unmodifiableMap(types);

        if (types.containsKey(QN_INTERCHANGE.toString())) {
            this.standardLoop = (EDIComplexType) types.get(QN_INTERCHANGE.toString());
        } else if (types.containsKey(QN_TRANSACTION.toString())) {
            this.standardLoop = (EDIComplexType) types.get(QN_TRANSACTION.toString());
        } else {
            throw new EDISchemaException("Schema must contain either " +
                    QN_INTERCHANGE + " or " + QN_TRANSACTION);
        }
    }

    @Override
    public EDIType getType(String name) {
        return types.get(name);
    }

    @Override
    public boolean containsSegment(String name) {
        final EDIType type = types.get(name);
        return type != null && type.isType(EDIType.Type.SEGMENT);
    }

    @Override
    public Iterator<EDIType> iterator() {
        return types.values().iterator();
    }
}
