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
package io.xlate.edi.stream;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;

import org.junit.Test;

import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.schema.SchemaUtils;
import io.xlate.edi.stream.EDIStreamConstants.ElementDataErrors;
import io.xlate.edi.stream.EDIStreamConstants.ElementOccurrenceErrors;
import io.xlate.edi.stream.EDIStreamConstants.Events;
import io.xlate.edi.stream.EDIStreamConstants.Standards;

@SuppressWarnings("static-method")
public class TestEventSequence implements ElementDataErrors,
		ElementOccurrenceErrors {

	@Test
	public void testValidatedTagSequence()
			throws EDISchemaException, EDIStreamException, IllegalStateException {
		EDIInputFactory factory = EDIInputFactory.newFactory();
		InputStream stream =
				new ByteArrayInputStream(
						("ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
								+ "GS*FA*ReceiverDept*SenderDept*20050812*195335*000005*X*005010X230~"
								+ "ST*997*0001*005010X230~"
								+ "AK1*HC*000001~"
								+ "AK2*837*0021~"
								+ "AK3*NM1*8**8~"
								+ "AK4*8*66*7*MI~"
								+ "AK5*R*5~"
								+ "AK9*R*1*1*0~"
								+ "SE*8*0001~"
								+ "GE*1*000005~"
								+ "IEA*1*508121953~").getBytes());

		int event;

		@SuppressWarnings("resource")
		EDIStreamReader reader = factory.createEDIStreamReader(stream);
		assertEquals(Events.START_INTERCHANGE, reader.next());
		reader.setSchema(loadSchema(reader.getStandard(), reader.getVersion()));
		assertEquals(Events.START_SEGMENT, reader.next());
		assertEquals("ISA", reader.getText());
		while ((event = reader.next()) == Events.ELEMENT_DATA) {
			continue;
		}
		assertEquals(Events.END_SEGMENT, event);

		assertEquals(Events.START_LOOP, reader.next());
		assertEquals("GROUP", reader.getText());

		assertEquals(Events.START_SEGMENT, reader.next());
		assertEquals("GS", reader.getText());
		while ((event = reader.next()) == Events.ELEMENT_DATA) {
			continue;
		}
		assertEquals(Events.END_SEGMENT, event);

		assertEquals(Events.START_LOOP, reader.next());
		assertEquals("TRANSACTION", reader.getText());

		assertEquals(Events.START_SEGMENT, reader.next());
		assertEquals("ST", reader.getText());
		while ((event = reader.next()) == Events.ELEMENT_DATA) {
			continue;
		}
		assertEquals(Events.END_SEGMENT, event);

		assertEquals(Events.START_SEGMENT, reader.next());
		assertEquals("AK1", reader.getText());
		while ((event = reader.next()) == Events.ELEMENT_DATA) {
			continue;
		}
		assertEquals(Events.END_SEGMENT, event);

		assertEquals(Events.START_LOOP, reader.next());
		assertEquals("2000", reader.getText());

		assertEquals(Events.START_SEGMENT, reader.next());
		assertEquals("AK2", reader.getText());
		while ((event = reader.next()) == Events.ELEMENT_DATA) {
			continue;
		}
		assertEquals(Events.END_SEGMENT, event);

		assertEquals(Events.START_LOOP, reader.next());
		assertEquals("2100", reader.getText());

		assertEquals(Events.START_SEGMENT, reader.next());
		assertEquals("AK3", reader.getText());
		while ((event = reader.next()) == Events.ELEMENT_DATA) {
			continue;
		}
		assertEquals(Events.END_SEGMENT, event);

		assertEquals(Events.START_SEGMENT, reader.next());
		assertEquals("AK4", reader.getText());
		ak4:
		while (true) {
			switch (event = reader.next()) {
			case Events.ELEMENT_DATA:
			case Events.START_COMPOSITE:
			case Events.END_COMPOSITE:
				continue;
			default:
				break ak4;
			}
		}
		assertEquals(Events.END_SEGMENT, event);
		assertEquals(Events.END_LOOP, reader.next()); // 2100

		assertEquals(Events.START_SEGMENT, reader.next());
		assertEquals("AK5", reader.getText());
		while ((event = reader.next()) == Events.ELEMENT_DATA) {
			continue;
		}
		assertEquals(Events.END_SEGMENT, event);
		assertEquals(Events.END_LOOP, reader.next()); // 2000

		assertEquals(Events.START_SEGMENT, reader.next());
		assertEquals("AK9", reader.getText());
		while ((event = reader.next()) == Events.ELEMENT_DATA) {
			continue;
		}
		assertEquals(Events.END_SEGMENT, event);

		assertEquals(Events.START_SEGMENT, reader.next());
		assertEquals("SE", reader.getText());
		while ((event = reader.next()) == Events.ELEMENT_DATA) {
			continue;
		}
		assertEquals(Events.END_SEGMENT, event);
		assertEquals(Events.END_LOOP, reader.next()); // 0001

		assertEquals(Events.START_SEGMENT, reader.next());
		assertEquals("GE", reader.getText());
		while ((event = reader.next()) == Events.ELEMENT_DATA) {
			continue;
		}
		assertEquals(Events.END_SEGMENT, event);
		assertEquals(Events.END_LOOP, reader.next()); // 0000

		assertEquals(Events.START_SEGMENT, reader.next());
		assertEquals("IEA", reader.getText());
		assertEquals(Events.ELEMENT_DATA, reader.next()); // IEA01
		assertEquals(Events.ELEMENT_DATA, reader.next()); // IEA02
		assertEquals(Events.END_SEGMENT, reader.next()); // IEA
		assertEquals(Events.END_INTERCHANGE, reader.next());
	}

	@Test
	public void testMissingTagSequence()
			throws EDISchemaException, EDIStreamException, IllegalStateException {
		EDIInputFactory factory = EDIInputFactory.newFactory();
		InputStream stream =
				new ByteArrayInputStream(
						("ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
								+ "GS*FA*ReceiverDept*SenderDept*20050812*195335*000005*X*005010X230~"
								+ "ST*997*0001*005010X230~"
								+ "AK1*HC*000001~"
								+ "SE*8*0001~"
								+ "GE*1*000005~"
								+ "IEA*1*508121953~").getBytes());

		int event;

		@SuppressWarnings("resource")
		EDIStreamReader reader = factory.createEDIStreamReader(stream);
		assertEquals(Events.START_INTERCHANGE, reader.next());
		reader.setSchema(loadSchema(reader.getStandard(), reader.getVersion()));
		assertEquals(Events.START_SEGMENT, reader.next());
		assertEquals("ISA", reader.getText());
		while ((event = reader.next()) == Events.ELEMENT_DATA) {
			continue;
		}
		assertEquals(Events.END_SEGMENT, event);

		assertEquals(Events.START_LOOP, reader.next());
		assertEquals("GROUP", reader.getText());

		assertEquals(Events.START_SEGMENT, reader.next());
		assertEquals("GS", reader.getText());
		while ((event = reader.next()) == Events.ELEMENT_DATA) {
			continue;
		}
		assertEquals(Events.END_SEGMENT, event);

		assertEquals(Events.START_LOOP, reader.next());
		assertEquals("TRANSACTION", reader.getText());

		assertEquals(Events.START_SEGMENT, reader.next());
		assertEquals("ST", reader.getText());
		while ((event = reader.next()) == Events.ELEMENT_DATA) {
			continue;
		}
		assertEquals(Events.END_SEGMENT, event);

		assertEquals(Events.START_SEGMENT, reader.next());
		assertEquals("AK1", reader.getText());
		while ((event = reader.next()) == Events.ELEMENT_DATA) {
			continue;
		}
		assertEquals(Events.END_SEGMENT, event);

		// Missing AK9 in our test schema, does not match the standard.
		assertEquals(Events.SEGMENT_ERROR, reader.next());
		assertEquals("AK9", reader.getText());

		assertEquals(Events.START_SEGMENT, reader.next());
		assertEquals("SE", reader.getText());
		while ((event = reader.next()) == Events.ELEMENT_DATA) {
			continue;
		}
		assertEquals(Events.END_SEGMENT, event);
		assertEquals(Events.END_LOOP, reader.next()); // 0001

		assertEquals(Events.START_SEGMENT, reader.next());
		assertEquals("GE", reader.getText());
		while ((event = reader.next()) == Events.ELEMENT_DATA) {
			continue;
		}
		assertEquals(Events.END_SEGMENT, event);
		assertEquals(Events.END_LOOP, reader.next()); // 0000

		assertEquals(Events.START_SEGMENT, reader.next());
		assertEquals("IEA", reader.getText());
		assertEquals(Events.ELEMENT_DATA, reader.next()); // IEA01
		assertEquals(Events.ELEMENT_DATA, reader.next()); // IEA02
		assertEquals(Events.END_SEGMENT, reader.next()); // IEA
		assertEquals(Events.END_INTERCHANGE, reader.next());
	}
	
	@Test
	@org.junit.Ignore
	public void testValidSequenceMapDbEDIFACT()
			throws EDISchemaException, EDIStreamException {
		EDIInputFactory factory = EDIInputFactory.newFactory();
		InputStream stream =
				new ByteArrayInputStream(
						("UNB+UNOA:3+005435656:1+006415160:1+060515:1434+00000000000778'"
								+ "UNH+00000000000117+INVOIC:D:97B:UN'"
								+ "UNT+2+00000000000117'"
								+ "UNZ+1+00000000000778'").getBytes());

		@SuppressWarnings("resource")
		EDIStreamReader reader = factory.createEDIStreamReader(stream);
		assertEquals(Events.START_INTERCHANGE, reader.next());
		assertEquals("30000", reader.getVersion());

		Schema schema = SchemaUtils.getMapSchema(Standards.EDIFACT, "30000", "INTERCHANGE");
		reader.setSchema(schema);

		assertEquals(Events.START_SEGMENT, reader.next());
		assertEquals("UNB", reader.getText());

		assertEquals(Events.START_COMPOSITE, reader.next());
		assertEquals(Events.ELEMENT_DATA, reader.next());
		assertEquals("UNOA", reader.getText());
		assertEquals(Events.ELEMENT_DATA, reader.next());
		assertEquals("3", reader.getText());
		assertEquals(Events.END_COMPOSITE, reader.next());

		assertEquals(Events.START_COMPOSITE, reader.next());
		assertEquals(Events.ELEMENT_DATA, reader.next());
		assertEquals("005435656", reader.getText());
		assertEquals(Events.ELEMENT_DATA, reader.next());
		assertEquals("1", reader.getText());
		assertEquals(Events.END_COMPOSITE, reader.next());

		assertEquals(Events.START_COMPOSITE, reader.next());
		assertEquals(Events.ELEMENT_DATA, reader.next());
		assertEquals("006415160", reader.getText());
		assertEquals(Events.ELEMENT_DATA, reader.next());
		assertEquals("1", reader.getText());
		assertEquals(Events.END_COMPOSITE, reader.next());

		assertEquals(Events.START_COMPOSITE, reader.next());
		assertEquals(Events.ELEMENT_DATA, reader.next());
		assertEquals("060515", reader.getText());
		assertEquals(Events.ELEMENT_DATA, reader.next());
		assertEquals("1434", reader.getText());
		assertEquals(Events.END_COMPOSITE, reader.next());

		assertEquals(Events.ELEMENT_DATA, reader.next());
		assertEquals("00000000000778", reader.getText());
		assertEquals(Events.END_SEGMENT, reader.next());

		assertEquals(Events.START_LOOP, reader.next());
		assertEquals("TRANSACTION", reader.getText());

		assertEquals(Events.START_SEGMENT, reader.next());
		assertEquals("UNH", reader.getText());
		assertEquals(Events.ELEMENT_DATA, reader.next());
		assertEquals("00000000000117", reader.getText());

		assertEquals(Events.START_COMPOSITE, reader.next());
		assertEquals(Events.ELEMENT_DATA, reader.next());
		assertEquals("INVOIC", reader.getText());
		assertEquals(Events.ELEMENT_DATA, reader.next());
		assertEquals("D", reader.getText());
		assertEquals(Events.ELEMENT_DATA, reader.next());
		assertEquals("97B", reader.getText());
		assertEquals(Events.ELEMENT_DATA, reader.next());
		assertEquals("UN", reader.getText());
		assertEquals(Events.END_COMPOSITE, reader.next());
		assertEquals(Events.END_SEGMENT, reader.next());

		assertEquals(Events.START_SEGMENT, reader.next());
		assertEquals("UNT", reader.getText());
		assertEquals(Events.ELEMENT_DATA, reader.next());
		assertEquals("2", reader.getText());
		assertEquals(Events.ELEMENT_DATA, reader.next());
		assertEquals("00000000000117", reader.getText());
		assertEquals(Events.END_SEGMENT, reader.next());
		assertEquals(Events.END_LOOP, reader.next()); //TRANSACTION

		assertEquals(Events.START_SEGMENT, reader.next());
		assertEquals("UNZ", reader.getText());
		assertEquals(Events.ELEMENT_DATA, reader.next());
		assertEquals("1", reader.getText());
		assertEquals(Events.ELEMENT_DATA, reader.next());
		assertEquals("00000000000778", reader.getText());
		assertEquals(Events.END_SEGMENT, reader.next());
		assertEquals(Events.END_INTERCHANGE, reader.next());
	}

	@Test
	public void testElementErrorSequence()
			throws EDISchemaException, EDIStreamException, IllegalStateException {
		EDIInputFactory factory = EDIInputFactory.newFactory();
		InputStream stream =
				new ByteArrayInputStream(
						("ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
								+ "GS*FA*ReceiverDept*SenderDept*BAD_DATE*295335*000005*X*005010X230~"
								+ "ST*997*0001~"
								+ "AK1*<NOT_IN_CODESET>*000001~"
								+ "AK9*R*1*1*0~"
								+ "AK9*~"
								+ "SE*8*0001~"
								+ "GE*1*<TOO_LONG_AND_NOT_NUMERIC>^0001^AGAIN!*:~"
								+ "IEA*1*508121953~").getBytes());

		@SuppressWarnings("resource")
		EDIStreamReader reader = factory.createEDIStreamReader(stream);
		assertEquals(Events.START_INTERCHANGE, reader.next());
		reader.setSchema(loadSchema(reader.getStandard(), reader.getVersion()));
		assertEquals(Events.START_SEGMENT, reader.nextTag());
		assertEquals("ISA", reader.getText());
		assertEquals(Events.START_SEGMENT, reader.nextTag());
		assertEquals("GS", reader.getText());
		assertEquals(Events.ELEMENT_DATA, reader.next()); // GS01
		assertEquals(Events.ELEMENT_DATA, reader.next()); // GS02
		assertEquals(Events.ELEMENT_DATA, reader.next()); // GS03

		// GS04 BAD_DATE
		assertEquals(Events.ELEMENT_DATA_ERROR, reader.next());
		assertEquals("373", reader.getReferenceCode());
		assertEquals(INVALID_DATE, reader.getErrorType());
		assertEquals("BAD_DATE", reader.getText());
		assertEquals(Events.ELEMENT_DATA, reader.next()); // GS04

		// GS05 invalid time
		assertEquals(Events.ELEMENT_DATA_ERROR, reader.next());
		assertEquals(INVALID_TIME, reader.getErrorType());
		assertEquals("295335", reader.getText());
		assertEquals(Events.ELEMENT_DATA, reader.next()); // GS05

		assertEquals(Events.START_SEGMENT, reader.nextTag());
		assertEquals("ST", reader.getText());

		assertEquals(Events.START_SEGMENT, reader.nextTag());
		assertEquals("AK1", reader.getText());
		// AK01 <NOT_IN_CODESET>
		assertEquals(Events.ELEMENT_DATA_ERROR, reader.next());
		assertEquals(DATA_ELEMENT_TOO_LONG, reader.getErrorType());
		assertEquals("<NOT_IN_CODESET>", reader.getText());
		assertEquals(Events.ELEMENT_DATA_ERROR, reader.next());
		assertEquals(INVALID_CODE_VALUE, reader.getErrorType());
		assertEquals("<NOT_IN_CODESET>", reader.getText());
		assertEquals(Events.ELEMENT_DATA, reader.next()); // AK01

		assertEquals(Events.START_SEGMENT, reader.nextTag());
		assertEquals("AK9", reader.getText());

		assertEquals(Events.START_SEGMENT, reader.nextTag());
		assertEquals("AK9", reader.getText());

		assertEquals(Events.START_SEGMENT, reader.nextTag());
		assertEquals("SE", reader.getText());

		assertEquals(Events.START_SEGMENT, reader.nextTag());
		assertEquals("GE", reader.getText());
		assertEquals(Events.ELEMENT_DATA, reader.next()); // GE01

		// GE02 <TOO_LONG_AND_NOT_NUMERIC>
		assertEquals(Events.ELEMENT_DATA_ERROR, reader.next());
		assertEquals(DATA_ELEMENT_TOO_LONG, reader.getErrorType());
		assertEquals("<TOO_LONG_AND_NOT_NUMERIC>", reader.getText());
		assertEquals(Events.ELEMENT_DATA_ERROR, reader.next());
		assertEquals(INVALID_CHARACTER_DATA, reader.getErrorType());
		assertEquals("<TOO_LONG_AND_NOT_NUMERIC>", reader.getText());
		assertEquals(Events.ELEMENT_DATA, reader.next()); // GE02
		assertEquals("97", reader.getReferenceCode());

		// GE02 - 2nd occurrence
		assertEquals(Events.ELEMENT_OCCURRENCE_ERROR, reader.next());
		assertEquals(TOO_MANY_REPETITIONS, reader.getErrorType());
		assertEquals(Events.ELEMENT_DATA, reader.next());
		assertEquals("0001", reader.getText());

		// GE02 - 3rd occurrence plus data error
		assertEquals(Events.ELEMENT_OCCURRENCE_ERROR, reader.next());
		assertEquals("97", reader.getReferenceCode());
		assertEquals(TOO_MANY_REPETITIONS, reader.getErrorType());
		assertEquals(Events.ELEMENT_DATA_ERROR, reader.next());
		assertEquals(INVALID_CHARACTER_DATA, reader.getErrorType());
		assertEquals("AGAIN!", reader.getText()); // data association with error
		assertEquals(Events.ELEMENT_DATA, reader.next());
		assertEquals("AGAIN!", reader.getText()); // here comes the element data
													// event

		// GE03 too many elements
		assertEquals(Events.ELEMENT_OCCURRENCE_ERROR, reader.next());
		assertEquals(TOO_MANY_DATA_ELEMENTS, reader.getErrorType());

		assertEquals(Events.START_SEGMENT, reader.nextTag());
		assertEquals("IEA", reader.getText());
		assertEquals(Events.ELEMENT_DATA, reader.next()); // IEA01
		assertEquals(Events.ELEMENT_DATA, reader.next()); // IEA02
		assertEquals(Events.END_SEGMENT, reader.next()); // IEA
		assertEquals(Events.END_INTERCHANGE, reader.next());
	}

	private Schema loadSchema(String standard, String version)
			throws EDISchemaException {

		Schema control = SchemaUtils.getControlSchema(standard, version);

		SchemaFactory schemaFactory = SchemaFactory.newFactory();
		URL schemaLocation;
		Schema schema;
		EDIComplexType parent;
		EDIReference child;

		schemaLocation = SchemaUtils.getURL("x12/EDISchema997.xml");
		Schema transaction = schemaFactory.createSchema(schemaLocation);
		parent = (EDIComplexType) control.getType("TRANSACTION");
		child = parent.getReferences().get(1); // SE
		schema = control.reference(transaction, parent, child);

		return schema;
	}
}
