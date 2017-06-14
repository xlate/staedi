package io.xlate.edi.schema;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import io.xlate.edi.stream.EDIStreamConstants.Standards;

@SuppressWarnings({ "static-method", "resource" })
public class TestStaEDISchema {

	@Test(expected = NullPointerException.class)
	public void testSetTypesNullTypes() throws EDISchemaException {
		StaEDISchema schema = new StaEDISchema();
		schema.setTypes(null);
	}

	@Test
	public void testRootTypeIsLoop() throws EDISchemaException {
		StaEDISchema schema = new StaEDISchema();
		InputStream schemaStream =
				SchemaUtils.getStream("X12/00402/control-schema.xml");
		Map<String, EDIType> types =
				StaEDISchemaFactory.loadTypes(schemaStream);
		schema.setTypes(types);

		Assert.assertEquals(
				EDIType.TYPE_LOOP,
				schema.getType(StaEDISchema.MAIN).getTypeCode());
	}

	@Test
	public void testMapDbInterchangeSchemaTree() throws EDISchemaException {
		System.setProperty("io.xlate.edi.standards", "/home/michael/git/xlate-schema-tools/schema-tools");
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
