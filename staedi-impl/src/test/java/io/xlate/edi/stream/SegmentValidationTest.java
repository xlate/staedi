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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;

import org.junit.Test;

import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.schema.SchemaUtils;
import io.xlate.edi.stream.EDIStreamConstants.Events;
import io.xlate.edi.stream.EDIStreamConstants.Standards;

@SuppressWarnings("resource")
public class SegmentValidationTest {

	EDIStreamFilter segmentErrorFilter = new EDIStreamFilter() {
		@Override
		public boolean accept(EDIStreamReader reader) {
			return reader.getEventType() == Events.SEGMENT_ERROR;
		}
	};

	@Test
	public void testValidSequenceXml()
			throws EDISchemaException, EDIStreamException {
		EDIInputFactory factory = EDIInputFactory.newFactory();
		InputStream stream =
				new ByteArrayInputStream(
						("ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
								+ "S01*X~"
								+ "S11*X~"
								+ "S12*X~"
								+ "S19*X~"
								+ "S09*X~"
								+ "IEA*1*508121953~").getBytes());

		SchemaFactory schemaFactory = SchemaFactory.newFactory();
		URL schemaLocation =
				SchemaUtils.getURL("x12/EDISchemaSegmentValidation.xml");
		Schema schema = schemaFactory.createSchema(schemaLocation);

		EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);
		reader = factory.createFilteredReader(reader, segmentErrorFilter);

		assertTrue("Segment errors exist", !reader.hasNext());
	}

	@Test
	public void testValidSequenceEdifact()
			throws EDISchemaException, EDIStreamException {
		EDIInputFactory factory = EDIInputFactory.newFactory();
		InputStream stream =
				new ByteArrayInputStream(
						("UNB+UNOA:1+005435656:1+006415160:1+060515:1434+00000000000778'"
								+ "UNH+00000000000117+INVOIC:D:97B:UN'"
								+ "UNT+23+00000000000117'"
								+ "UNZ+1+00000000000778'").getBytes());

		EDIStreamReader reader = factory.createEDIStreamReader(stream);
		assertEquals(Events.START_INTERCHANGE, reader.next());
        assertArrayEquals(new String[] { "UNOA", "1" }, reader.getVersion());
        Schema schema = SchemaUtils.getControlSchema(Standards.EDIFACT, reader.getVersion());
        reader.setSchema(schema);
		reader = factory.createFilteredReader(reader, segmentErrorFilter);

		assertTrue("Segment errors exist", !reader.hasNext());
	}

	@Test
	public void testMissingMandatoryXml()
			throws EDISchemaException, EDIStreamException {
		EDIInputFactory factory = EDIInputFactory.newFactory();
		InputStream stream =
				new ByteArrayInputStream(
						("ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
								+ "S01*X~"
								+ "S11*X~"
								+ "S13*X~"
								+ "S09*X~"
								+ "IEA*1*508121953~").getBytes());

		SchemaFactory schemaFactory = SchemaFactory.newFactory();
		URL schemaLocation =
				SchemaUtils.getURL("x12/EDISchemaSegmentValidation.xml");
		Schema schema = schemaFactory.createSchema(schemaLocation);

		EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);
		reader = factory.createFilteredReader(reader, segmentErrorFilter);

		assertTrue("Segment errors do not exist", reader.hasNext());
		reader.next();
		assertEquals(EDIStreamValidationError.MANDATORY_SEGMENT_MISSING, reader.getErrorType());
		assertEquals("S12", reader.getText());
		reader.next();
		assertEquals(EDIStreamValidationError.MANDATORY_SEGMENT_MISSING, reader.getErrorType());
		assertEquals("S19", reader.getText());

		assertTrue("Unexpected segment errors exist", !reader.hasNext());
	}

	@Test
	public void testMissingMandatoryEdifact()
			throws EDISchemaException, EDIStreamException {
		EDIInputFactory factory = EDIInputFactory.newFactory();
		InputStream stream =
				new ByteArrayInputStream(
						("UNB+UNOA:1+005435656:1+006415160:1+060515:1434+00000000000778'"
								+ "UNH+00000000000117+INVOIC:D:97B:UN'"
								+ "UNZ+1+00000000000778'").getBytes());

		EDIStreamReader reader = factory.createEDIStreamReader(stream);
		assertEquals(Events.START_INTERCHANGE, reader.next());
        assertArrayEquals(new String[] { "UNOA", "1" }, reader.getVersion());
        Schema schema = SchemaUtils.getControlSchema(Standards.EDIFACT, reader.getVersion());
        reader.setSchema(schema);
		reader = factory.createFilteredReader(reader, segmentErrorFilter);

		assertTrue("Segment errors do not exist", reader.hasNext());
		reader.next();
		assertEquals(EDIStreamValidationError.MANDATORY_SEGMENT_MISSING, reader.getErrorType());
		assertEquals("UNT", reader.getText());

		assertTrue("Unexpected segment errors exist", !reader.hasNext());
	}

	@Test
	public void testUnexpected()
			throws EDISchemaException, EDIStreamException {
		EDIInputFactory factory = EDIInputFactory.newFactory();
		InputStream stream =
				new ByteArrayInputStream(
						("ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
								+ "S01*X~"
								+ "S11*X~"
								+ "S12*X~"
								+ "S0A*X~"
								+ "S19*X~"
								+ "S09*X~"
								+ "IEA*1*508121953~").getBytes());

		SchemaFactory schemaFactory = SchemaFactory.newFactory();
		URL schemaLocation =
				SchemaUtils.getURL("x12/EDISchemaSegmentValidation.xml");
		Schema schema = schemaFactory.createSchema(schemaLocation);

		EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);
		reader = factory.createFilteredReader(reader, segmentErrorFilter);

		assertTrue("Segment errors do not exist", reader.hasNext());
		reader.next();
		assertEquals(EDIStreamValidationError.UNEXPECTED_SEGMENT, reader.getErrorType());
		assertEquals("S0A", reader.getText());

		assertTrue("Unexpected segment errors exist", !reader.hasNext());
	}

	@Test
	public void testImproperSequence()
			throws EDISchemaException, EDIStreamException {
		EDIInputFactory factory = EDIInputFactory.newFactory();
		InputStream stream =
				new ByteArrayInputStream(
						("ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
								+ "S01*X~"
								+ "S11*X~"
								+ "S12*X~"
								+ "S14*X~"
								+ "S13*X~"
								+ "S13*X~"
								+ "S14*X~"
								+ "S19*X~"
								+ "S09*X~"
								+ "IEA*1*508121953~").getBytes());

		SchemaFactory schemaFactory = SchemaFactory.newFactory();
		URL schemaLocation =
				SchemaUtils.getURL("x12/EDISchemaSegmentValidation.xml");
		Schema schema = schemaFactory.createSchema(schemaLocation);

		EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);
		reader = factory.createFilteredReader(reader, segmentErrorFilter);

		assertTrue("Segment errors do not exist", reader.hasNext());
		reader.next();
		assertEquals(EDIStreamValidationError.SEGMENT_NOT_IN_PROPER_SEQUENCE, reader.getErrorType());
		assertEquals("S13", reader.getText());
		reader.next();
		assertEquals(EDIStreamValidationError.SEGMENT_NOT_IN_PROPER_SEQUENCE, reader.getErrorType());
		assertEquals("S13", reader.getText());
		reader.next();
		assertEquals(EDIStreamValidationError.SEGMENT_EXCEEDS_MAXIMUM_USE, reader.getErrorType());
		assertEquals("S13", reader.getText());
		reader.next();
		assertEquals(EDIStreamValidationError.SEGMENT_EXCEEDS_MAXIMUM_USE, reader.getErrorType());
		assertEquals("S14", reader.getText());

		assertTrue("Unexpected segment errors exist", !reader.hasNext());
	}

	@Test
	public void testSegmentNotDefined()
			throws EDISchemaException, EDIStreamException {
		EDIInputFactory factory = EDIInputFactory.newFactory();
		InputStream stream =
				new ByteArrayInputStream(
						("ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
								+ "S01*X~"
								+ "S11*X~"
								+ "S12*X~"
								+ "S0B*X~"
								+ "S19*X~"
								+ "S09*X~"
								+ "IEA*1*508121953~").getBytes());

		SchemaFactory schemaFactory = SchemaFactory.newFactory();
		URL schemaLocation =
				SchemaUtils.getURL("x12/EDISchemaSegmentValidation.xml");
		Schema schema = schemaFactory.createSchema(schemaLocation);

		EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);
		reader = factory.createFilteredReader(reader, segmentErrorFilter);

		assertTrue("Segment errors do not exist", reader.hasNext());
		reader.next();
		assertEquals(EDIStreamValidationError.SEGMENT_NOT_IN_DEFINED_TRANSACTION_SET, reader.getErrorType());
		assertEquals("S0B", reader.getText());

		assertTrue("Unexpected segment errors exist", !reader.hasNext());
	}

	@Test
	public void testLoopOccurrence()
			throws EDISchemaException, EDIStreamException {
		EDIInputFactory factory = EDIInputFactory.newFactory();
		InputStream stream =
				new ByteArrayInputStream(
						("ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
								+ "S01*X~"
								+ "S11*X~"
								+ "S12*X~"
								+ "S19*X~"
								+ "S11*X~"
								+ "S12*X~"
								+ "S19*X~"
								+ "S09*X~"
								+ "IEA*1*508121953~").getBytes());

		SchemaFactory schemaFactory = SchemaFactory.newFactory();
		URL schemaLocation =
				SchemaUtils.getURL("x12/EDISchemaSegmentValidation.xml");
		Schema schema = schemaFactory.createSchema(schemaLocation);

		EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);
		reader = factory.createFilteredReader(reader, segmentErrorFilter);

		assertTrue("Segment errors do not exist", reader.hasNext());
		reader.next();
		assertEquals(EDIStreamValidationError.LOOP_OCCURS_OVER_MAXIMUM_TIMES, reader.getErrorType());
		assertEquals("S11", reader.getText());

		assertTrue("Unexpected segment errors exist", !reader.hasNext());
	}

	@Test
	public void testOptionalLoopNotUsed()
			throws EDISchemaException, EDIStreamException {
		EDIInputFactory factory = EDIInputFactory.newFactory();
		InputStream stream =
				new ByteArrayInputStream(
						("ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
								+ "S01*X~"
								+ "S09*X~"
								+ "IEA*1*508121953~").getBytes());

		SchemaFactory schemaFactory = SchemaFactory.newFactory();
		URL schemaLocation = SchemaUtils.getURL("x12/EDISchemaSegmentValidation.xml");
		Schema schema = schemaFactory.createSchema(schemaLocation);

		EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);
		reader = factory.createFilteredReader(reader, segmentErrorFilter);
		assertTrue("Unexpected segment errors exist", !reader.hasNext());
	}

	@Test
	public void testRequiredLoopNotUsed()
			throws EDISchemaException, EDIStreamException {
		EDIInputFactory factory = EDIInputFactory.newFactory();
		InputStream stream =
				new ByteArrayInputStream(
						("ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
								+ "IEA*1*508121953~").getBytes());

		SchemaFactory schemaFactory = SchemaFactory.newFactory();
		URL schemaLocation = SchemaUtils.getURL("x12/EDISchemaSegmentValidation.xml");
		Schema schema = schemaFactory.createSchema(schemaLocation);

		EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);
		reader = factory.createFilteredReader(reader, segmentErrorFilter);

		assertTrue("Segment errors do not exist", reader.hasNext());
		reader.next();
		assertEquals(EDIStreamValidationError.MANDATORY_SEGMENT_MISSING, reader.getErrorType());
		assertEquals("S01", reader.getText());

		assertTrue("Unexpected segment errors exist", !reader.hasNext());
	}
}
