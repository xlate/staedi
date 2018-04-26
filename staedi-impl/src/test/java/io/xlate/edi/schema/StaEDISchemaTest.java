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
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import io.xlate.edi.stream.EDIStreamConstants.Standards;

@SuppressWarnings({ "static-method", "resource" })
public class StaEDISchemaTest {

	@Test(expected = NullPointerException.class)
	public void testSetTypesNullTypes() throws EDISchemaException {
		StaEDISchema schema = new StaEDISchema();
		schema.setTypes(null);
	}

	@Test
	public void testRootTypeIsLoop() throws EDISchemaException, IOException {
		StaEDISchema schema = new StaEDISchema();
		InputStream schemaStream =
				SchemaUtils.getStreams("X12/00402/control-schema.xml")
				.nextElement().openStream();
		Map<String, EDIType> types =
				StaEDISchemaFactory.loadTypes(schemaStream);
		schema.setTypes(types);

		Assert.assertEquals(
				EDIType.TYPE_LOOP,
				schema.getType(StaEDISchema.MAIN).getTypeCode());
	}

	@Test
	@org.junit.Ignore
	public void testMapDbInterchangeSchemaTree() throws EDISchemaException {
		Schema schema = SchemaUtils.getMapSchema(Standards.EDIFACT, "40200", "INTERCHANGE");
		EDIComplexType main = schema.getMainLoop();
		int matches = 0;

		List<? extends EDIReference> mainRefs = main.getReferences();

		Assert.assertEquals(3, mainRefs.size());

		if (mainRefs.get(0).getReferencedType().getId().equals("UNB")) {
			matches++;
		}

		if (mainRefs.get(1).getReferencedType().getId().equals("TRANSACTION")) {
			matches++;

			EDIComplexType tran = (EDIComplexType) mainRefs.get(1).getReferencedType();
			List<? extends EDIReference> tranRefs = tran.getReferences();
			Assert.assertEquals(2, tranRefs.size());

			if (tranRefs.get(0).getReferencedType().getId().equals("UNH")) {
				matches++;
			}

			if (tranRefs.get(1).getReferencedType().getId().equals("UNT")) {
				matches++;
			}
		}

		if (mainRefs.get(2).getReferencedType().getId().equals("UNZ")) {
			matches++;
		}

		Assert.assertEquals("Unexpected number of matches", 5, matches);
	}
}
