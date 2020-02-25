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
package io.xlate.edi.internal.stream;


import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;

import org.junit.jupiter.api.Test;

import io.xlate.edi.internal.schema.SchemaUtils;
import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamFilter;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.EDIStreamConstants.Standards;

@SuppressWarnings("resource")
public class SegmentValidationTest {

    EDIStreamFilter segmentErrorFilter = new EDIStreamFilter() {
        @Override
        public boolean accept(EDIStreamReader reader) {
            switch (reader.getEventType()) {
            case START_TRANSACTION:
            case SEGMENT_ERROR:
                return true;
            default:
                return false;
            }
        }
    };

    @Test
    public void testValidSequenceXml() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "S01*X~"
                + "S11*X~"
                + "S12*X~"
                + "S19*X~"
                + "S09*X~"
                + "IEA*1*508121953~").getBytes());

        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        URL schemaLocation = getClass().getResource("/x12/EDISchemaSegmentValidation.xml");
        Schema schema = schemaFactory.createSchema(schemaLocation);

        EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);
        reader = factory.createFilteredReader(reader, segmentErrorFilter);

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next(), "Expecting start of transaction");
        reader.setTransactionSchema(schemaFactory.createSchema(getClass().getResource("/x12/EDISchemaSegmentValidationTx.xml")));
        assertTrue(!reader.hasNext(), "Unexpected segment errors exist");
    }

    @Test
    public void testValidSequenceEdifact() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "UNB+UNOA:1+005435656:1+006415160:1+060515:1434+00000000000778'"
                + "UNH+00000000000117+INVOIC:D:97B:UN'"
                + "UNT+23+00000000000117'"
                + "UNZ+1+00000000000778'").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        assertEquals(EDIStreamEvent.START_INTERCHANGE, reader.next());
        assertArrayEquals(new String[] { "UNOA", "1" }, reader.getVersion());
        Schema schema = SchemaUtils.getControlSchema(Standards.EDIFACT, reader.getVersion());
        reader.setControlSchema(schema);
        reader = factory.createFilteredReader(reader, segmentErrorFilter);

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next(), "Expecting start of transaction");
        assertTrue(!reader.hasNext(), "Segment errors exist");
    }

    @Test
    public void testMissingMandatoryXml() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "S01*X~"
                + "S11*X~"
                + "S13*X~"
                + "S09*X~"
                + "IEA*1*508121953~").getBytes());

        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        URL schemaLocation = getClass().getResource("/x12/EDISchemaSegmentValidation.xml");
        Schema schema = schemaFactory.createSchema(schemaLocation);

        EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);
        reader = factory.createFilteredReader(reader, segmentErrorFilter);

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next(), "Expecting start of transaction");
        reader.setTransactionSchema(schemaFactory.createSchema(getClass().getResource("/x12/EDISchemaSegmentValidationTx.xml")));

        assertTrue(reader.hasNext(), "Segment errors do not exist");
        reader.next();
        assertEquals(EDIStreamValidationError.MANDATORY_SEGMENT_MISSING, reader.getErrorType());
        assertEquals("S12", reader.getText());
        reader.next();
        assertEquals(EDIStreamValidationError.MANDATORY_SEGMENT_MISSING, reader.getErrorType());
        assertEquals("S19", reader.getText());

        assertTrue(!reader.hasNext(), "Unexpected segment errors exist");
    }

    @Test
    public void testMissingMandatoryEdifact() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "UNB+UNOA:1+005435656:1+006415160:1+060515:1434+00000000000778'"
                + "UNH+00000000000117+INVOIC:D:97B:UN'"
                + "UNZ+1+00000000000778'").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        assertEquals(EDIStreamEvent.START_INTERCHANGE, reader.next());
        assertArrayEquals(new String[] { "UNOA", "1" }, reader.getVersion());
        Schema schema = SchemaUtils.getControlSchema(Standards.EDIFACT, reader.getVersion());
        reader.setControlSchema(schema);
        reader = factory.createFilteredReader(reader, segmentErrorFilter);

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next(), "Expecting start of transaction");

        assertTrue(reader.hasNext(), "Segment errors do not exist");
        reader.next();
        assertEquals(EDIStreamValidationError.MANDATORY_SEGMENT_MISSING, reader.getErrorType());
        assertEquals("UNT", reader.getText());

        assertTrue(!reader.hasNext(), "Unexpected segment errors exist");
    }

    @Test
    public void testUnexpected() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "S01*X~"
                + "S11*X~"
                + "S12*X~"
                + "S21*X~"
                + "S19*X~"
                + "S09*X~"
                + "IEA*1*508121953~").getBytes());

        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        URL schemaLocation = getClass().getResource("/x12/EDISchemaSegmentValidation.xml");
        Schema schema = schemaFactory.createSchema(schemaLocation);

        EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);
        reader = factory.createFilteredReader(reader, segmentErrorFilter);

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next(), "Expecting start of transaction");
        reader.setTransactionSchema(schemaFactory.createSchema(getClass().getResource("/x12/EDISchemaSegmentValidationTx.xml")));

        assertTrue(reader.hasNext(), "Segment errors do not exist");
        reader.next();
        assertEquals(EDIStreamValidationError.UNEXPECTED_SEGMENT, reader.getErrorType());
        assertEquals("S21", reader.getText());

        assertTrue(!reader.hasNext(), "Unexpected segment errors exist");
    }

    @Test
    public void testImproperSequence()
                                       throws EDISchemaException,
                                       EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
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
        URL schemaLocation = getClass().getResource("/x12/EDISchemaSegmentValidation.xml");
        Schema schema = schemaFactory.createSchema(schemaLocation);

        EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);
        reader = factory.createFilteredReader(reader, segmentErrorFilter);

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next(), "Expecting start of transaction");
        reader.setTransactionSchema(schemaFactory.createSchema(getClass().getResource("/x12/EDISchemaSegmentValidationTx.xml")));

        assertTrue(reader.hasNext(), "Segment errors do not exist");
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

        assertTrue(!reader.hasNext(), "Unexpected segment errors exist");
    }

    @Test
    public void testSegmentNotDefined()
                                        throws EDISchemaException,
                                        EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "S01*X~"
                + "S11*X~"
                + "S12*X~"
                + "S0B*X~"
                + "S19*X~"
                + "S09*X~"
                + "IEA*1*508121953~").getBytes());

        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        URL schemaLocation = getClass().getResource("/x12/EDISchemaSegmentValidation.xml");
        Schema schema = schemaFactory.createSchema(schemaLocation);

        EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);
        reader = factory.createFilteredReader(reader, segmentErrorFilter);

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next(), "Expecting start of transaction");
        reader.setTransactionSchema(schemaFactory.createSchema(getClass().getResource("/x12/EDISchemaSegmentValidationTx.xml")));

        assertTrue(reader.hasNext(), "Segment errors do not exist");
        reader.next();
        assertEquals(EDIStreamValidationError.SEGMENT_NOT_IN_DEFINED_TRANSACTION_SET, reader.getErrorType());
        assertEquals("S0B", reader.getText());

        assertTrue(!reader.hasNext(), "Unexpected segment errors exist");
    }

    @Test
    public void testLoopMultiOccurrenceSingleSegment() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "S01*X~"
                + "S0A*X~"
                + "S11*X~"
                + "S12*X~"
                + "S19*X~"
                + "S20*X~"
                + "S20*X~"
                + "S09*X~"
                + "IEA*1*508121953~").getBytes());

        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        URL schemaLocation = getClass().getResource("/x12/EDISchemaSegmentValidation.xml");
        Schema schema = schemaFactory.createSchema(schemaLocation);

        EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);
        reader = factory.createFilteredReader(reader, new EDIStreamFilter() {
            @Override
            public boolean accept(EDIStreamReader reader) {
                switch (reader.getEventType()) {
                case START_TRANSACTION:
                case SEGMENT_ERROR:
                case START_LOOP:
                case END_LOOP:
                    return true;
                default:
                    return false;
                }
            }
        });

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next(), "Expecting start of transaction");
        reader.setTransactionSchema(schemaFactory.createSchema(getClass().getResource("/x12/EDISchemaSegmentValidationTx.xml")));

        assertEquals(EDIStreamEvent.START_LOOP, reader.next());
        assertEquals("0000", reader.getReferenceCode());
        assertEquals(EDIStreamEvent.END_LOOP, reader.next());
        assertEquals("0000", reader.getReferenceCode());

        for (int i = 0; i < 2; i++) {
            assertEquals(EDIStreamEvent.START_LOOP, reader.next());
            assertEquals("0001", reader.getReferenceCode());
            assertEquals(EDIStreamEvent.END_LOOP, reader.next());
            assertEquals("0001", reader.getReferenceCode());
        }

        assertTrue(!reader.hasNext(), "Unexpected segment errors exist");
    }

    @Test
    public void testLoopOccurrence() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
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
        URL schemaLocation = getClass().getResource("/x12/EDISchemaSegmentValidation.xml");
        Schema schema = schemaFactory.createSchema(schemaLocation);

        EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);
        reader = factory.createFilteredReader(reader, segmentErrorFilter);

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next(), "Expecting start of transaction");
        reader.setTransactionSchema(schemaFactory.createSchema(getClass().getResource("/x12/EDISchemaSegmentValidationTx.xml")));

        assertTrue(reader.hasNext(), "Segment errors do not exist");
        reader.next();
        assertEquals(EDIStreamValidationError.LOOP_OCCURS_OVER_MAXIMUM_TIMES, reader.getErrorType());
        assertEquals("S11", reader.getText());

        assertTrue(!reader.hasNext(), "Unexpected segment errors exist");
    }

    @Test
    public void testOptionalLoopNotUsed() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "S01*X~"
                + "S09*X~"
                + "IEA*1*508121953~").getBytes());

        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        URL schemaLocation = getClass().getResource("/x12/EDISchemaSegmentValidation.xml");
        Schema schema = schemaFactory.createSchema(schemaLocation);

        EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);
        reader = factory.createFilteredReader(reader, segmentErrorFilter);

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next(), "Expecting start of transaction");
        assertTrue(!reader.hasNext(), "Unexpected segment errors exist");
    }

    @Test
    public void testRequiredLoopNotUsed() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "IEA*1*508121953~").getBytes());

        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        URL schemaLocation = getClass().getResource("/x12/EDISchemaSegmentValidation.xml");
        Schema schema = schemaFactory.createSchema(schemaLocation);

        EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);
        reader = factory.createFilteredReader(reader, segmentErrorFilter);

        assertTrue(reader.hasNext(), "Segment errors do not exist");
        reader.next();
        assertEquals(EDIStreamValidationError.MANDATORY_SEGMENT_MISSING, reader.getErrorType());
        assertEquals("S01", reader.getText());

        assertTrue(!reader.hasNext(), "Unexpected segment errors exist");
    }

    @Test
    public void testImplementationValidSequence() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "S01*X~"
                + "S0A*X~"
                + "S11*A~"
                + "S12*X~" // IMPLEMENTATION_UNUSED_SEGMENT_PRESENT
                + "S11*B~"
                + "S12*X~"
                + "S12*X~"
                + "S12*X~" // SEGMENT_EXCEEDS_MAXIMUM_USE
                + "S11*B~"
                + "S09*X~"
                + "IEA*1*508121953~").getBytes());

        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        URL schemaLocation = getClass().getResource("/x12/EDISchemaSegmentValidation.xml");
        Schema schema = schemaFactory.createSchema(schemaLocation);

        EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);
        reader = factory.createFilteredReader(reader, new EDIStreamFilter() {
            @Override
            public boolean accept(EDIStreamReader reader) {
                switch (reader.getEventType()) {
                case START_TRANSACTION:
                case SEGMENT_ERROR:
                case START_LOOP:
                case END_LOOP:
                    return true;
                default:
                    return false;
                }
            }
        });

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next(), "Expecting start of transaction");
        reader.setTransactionSchema(schemaFactory.createSchema(getClass().getResource("/x12/EDISchemaSegmentValidationImpl.xml")));

        assertEquals(EDIStreamEvent.START_LOOP, reader.next());
        assertEquals("0000A", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.IMPLEMENTATION_UNUSED_SEGMENT_PRESENT, reader.getErrorType());
        assertEquals("S12", reader.getReferenceCode());

        // FIXME: S13 has min use = 1, check for segment error

        assertEquals(EDIStreamEvent.END_LOOP, reader.next());
        assertEquals("0000A", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.START_LOOP, reader.next());
        assertEquals("0000B", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.SEGMENT_EXCEEDS_MAXIMUM_USE, reader.getErrorType());
        assertEquals("S12", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.END_LOOP, reader.next());
        assertEquals("0000B", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.START_LOOP, reader.next());
        assertEquals("0000B", reader.getReferenceCode());
        assertEquals(EDIStreamEvent.END_LOOP, reader.next());
        assertEquals("0000B", reader.getReferenceCode());

        assertTrue(!reader.hasNext(), "Unexpected segment errors exist");
    }
}
