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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import io.xlate.edi.internal.schema.SchemaUtils;
import io.xlate.edi.internal.schema.StaEDISchema;
import io.xlate.edi.internal.schema.StaEDISchemaFactory;
import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.EDIType;

@SuppressWarnings({ "resource" })
public class StaEDISchemaTest {

    @Test(expected = NullPointerException.class)
    public void testSetTypesNullTypes() throws EDISchemaException {
        StaEDISchema schema = new StaEDISchema();
        schema.setTypes(null);
    }

    @Test
    public void testRootTypeIsLoop() throws EDISchemaException, IOException {
        StaEDISchema schema = new StaEDISchema();
        InputStream schemaStream = SchemaUtils.getStreams("X12/v00402.xml")
                                              .nextElement()
                                              .openStream();
        Map<String, EDIType> types = new StaEDISchemaFactory().loadTypes(schemaStream);
        schema.setTypes(types);

        Assert.assertEquals(EDIType.Type.LOOP, schema.getType(StaEDISchema.MAIN).getType());
    }
}
