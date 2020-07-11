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
class SegmentValidationTest {

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
    void testValidSequenceXml() throws EDISchemaException, EDIStreamException {
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
    void testValidSequenceEdifact() throws EDISchemaException, EDIStreamException {
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
    void testMissingMandatoryXml() throws EDISchemaException, EDIStreamException {
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
    void testMissingMandatoryEdifact() throws EDISchemaException, EDIStreamException {
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
    void testUnexpected() throws EDISchemaException, EDIStreamException {
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
    void testImproperSequence()
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
    void testSegmentNotDefined()
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
    void testLoopMultiOccurrenceSingleSegment() throws EDISchemaException, EDIStreamException {
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
        assertEquals("L0000", reader.getReferenceCode());
        assertEquals(EDIStreamEvent.END_LOOP, reader.next());
        assertEquals("L0000", reader.getReferenceCode());

        for (int i = 0; i < 2; i++) {
            assertEquals(EDIStreamEvent.START_LOOP, reader.next());
            assertEquals("L0001", reader.getReferenceCode());
            assertEquals(EDIStreamEvent.END_LOOP, reader.next());
            assertEquals("L0001", reader.getReferenceCode());
        }

        assertTrue(!reader.hasNext(), "Unexpected segment errors exist");
    }

    @Test
    void testLoopOccurrence() throws EDISchemaException, EDIStreamException {
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
    void testOptionalLoopNotUsed() throws EDISchemaException, EDIStreamException {
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
    void testRequiredLoopNotUsed() throws EDISchemaException, EDIStreamException {
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
    void testImplementationValidSequence() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "S01*X~"
                + "S0A*X~"
                + "S11*A~"
                + "S12*X~" // IMPLEMENTATION_UNUSED_SEGMENT_PRESENT
                // IMPLEMENTATION_SEGMENT_BELOW_MINIMUM_USE (missing S13)
                + "S11*B~"
                + "S12*X~"
                + "S12*X~"
                + "S12*X~" // SEGMENT_EXCEEDS_MAXIMUM_USE
                + "S11*B~"
                + "S11*B~" // LOOP_OCCURS_OVER_MAXIMUM_TIMES
                + "S11*C~"
                // IMPLEMENTATION_LOOP_OCCURS_UNDER_MINIMUM_TIMES
                + "S11*D~"
                + "S09*X~"
                + "IEA*1*508121953~").getBytes());

        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        URL schemaLocation = getClass().getResource("/x12/EDISchemaSegmentValidation.xml");
        Schema schema = schemaFactory.createSchema(schemaLocation);

        EDIStreamReader unfiltered = factory.createEDIStreamReader(stream, schema);
        EDIStreamReader reader = factory.createFilteredReader(unfiltered, new EDIStreamFilter() {
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

        // Loop A
        assertEquals(EDIStreamEvent.START_LOOP, reader.next());
        assertEquals("0000A", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.IMPLEMENTATION_UNUSED_SEGMENT_PRESENT, reader.getErrorType());
        assertEquals("S12", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.IMPLEMENTATION_SEGMENT_BELOW_MINIMUM_USE, reader.getErrorType(), () -> {
            return "Unexpected event for code: " + reader.getReferenceCode();
        });
        assertEquals("S13", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.END_LOOP, reader.next());
        assertEquals("0000A", reader.getReferenceCode());

        // Loop B - Occurrence 1
        assertEquals(EDIStreamEvent.START_LOOP, reader.next());
        assertEquals("0000B", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.SEGMENT_EXCEEDS_MAXIMUM_USE, reader.getErrorType(), () -> {
            return "Unexpected event for code: " + reader.getReferenceCode();
        });
        assertEquals("S12", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.END_LOOP, reader.next());
        assertEquals("0000B", reader.getReferenceCode());

        // Loop B - Occurrence 2
        assertEquals(EDIStreamEvent.START_LOOP, reader.next());
        assertEquals("0000B", reader.getReferenceCode());
        assertEquals(EDIStreamEvent.END_LOOP, reader.next());
        assertEquals("0000B", reader.getReferenceCode());

        // Loop B - Occurrence 3
        assertEquals(EDIStreamEvent.START_LOOP, reader.next());
        assertEquals("0000B", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.LOOP_OCCURS_OVER_MAXIMUM_TIMES, reader.getErrorType());
        assertEquals("S11", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.END_LOOP, reader.next());
        assertEquals("0000B", reader.getReferenceCode());

        // Loop C - Occurrence 1
        assertEquals(EDIStreamEvent.START_LOOP, reader.next());
        assertEquals("0000C", reader.getReferenceCode());
        assertEquals(EDIStreamEvent.END_LOOP, reader.next());
        assertEquals("0000C", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.IMPLEMENTATION_LOOP_OCCURS_UNDER_MINIMUM_TIMES, reader.getErrorType());
        assertEquals("S11", reader.getReferenceCode());

        // Loop D - Occurrence 1
        assertEquals(EDIStreamEvent.START_LOOP, reader.next());
        assertEquals("0000D", reader.getReferenceCode());

        // Standard Loop L0000 may only occur 5 times
        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.LOOP_OCCURS_OVER_MAXIMUM_TIMES, reader.getErrorType());
        assertEquals("S11", reader.getText());
        assertEquals("L0000", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.END_LOOP, reader.next());
        assertEquals("0000D", reader.getReferenceCode());

        // Loop L0001 has minOccurs=1 in standard (not used in implementation, invalid configuration)
        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.MANDATORY_SEGMENT_MISSING, reader.getErrorType());
        assertEquals("S20", reader.getReferenceCode());

        assertTrue(!reader.hasNext(), "Unexpected segment errors exist");
    }

    @Test
    void testImplementationValidSequenceAllMissing() throws EDISchemaException, EDIStreamException {
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

        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.IMPLEMENTATION_LOOP_OCCURS_UNDER_MINIMUM_TIMES, reader.getErrorType());
        assertEquals("S11", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.IMPLEMENTATION_LOOP_OCCURS_UNDER_MINIMUM_TIMES, reader.getErrorType());
        assertEquals("S11", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.IMPLEMENTATION_LOOP_OCCURS_UNDER_MINIMUM_TIMES, reader.getErrorType());
        assertEquals("S11", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.IMPLEMENTATION_LOOP_OCCURS_UNDER_MINIMUM_TIMES, reader.getErrorType());
        assertEquals("S11", reader.getReferenceCode());

        // Loop L0001 has minOccurs=1 in standard (not used in implementation, invalid configuration)
        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.MANDATORY_SEGMENT_MISSING, reader.getErrorType());
        assertEquals("S20", reader.getReferenceCode());

        assertTrue(!reader.hasNext(), "Unexpected segment errors exist");
    }

    @Test
    void testImplementationValidSequenceWithCompositeDiscr() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "S01*X~"
                + "S02*X~"
                + "S10*A~"
                + "S12*X~"
                + "S30*A*E888:XX~"
                + "S32*X~"
                + "S30*B*E888:YY~"
                + "S31*A~"
                + "S31*A~"
                + "S31*B~"
                + "S09*X~IEA*1*508121953~").getBytes());

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
        reader.setTransactionSchema(schemaFactory.createSchema(getClass().getResource("/x12/EDISchemaSegmentValidationImpl2.xml")));

        // Loop A
        assertEquals(EDIStreamEvent.START_LOOP, reader.next());
        assertEquals("0000A", reader.getReferenceCode());

        // Loop AXX
        assertEquals(EDIStreamEvent.START_LOOP, reader.next());
        assertEquals("0002AXX", reader.getReferenceCode());
        assertEquals(EDIStreamEvent.END_LOOP, reader.next());
        assertEquals("0002AXX", reader.getReferenceCode());

        // Loop AYY
        assertEquals(EDIStreamEvent.START_LOOP, reader.next());
        assertEquals("0002AYY", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.SEGMENT_EXCEEDS_MAXIMUM_USE, reader.getErrorType());
        assertEquals("S31", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.IMPLEMENTATION_SEGMENT_BELOW_MINIMUM_USE, reader.getErrorType());
        assertEquals("S31", reader.getText());
        assertEquals("S31B", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.END_LOOP, reader.next());
        assertEquals("0002AYY", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.END_LOOP, reader.next());
        assertEquals("0000A", reader.getReferenceCode());

        assertTrue(!reader.hasNext(), "Unexpected segment errors exist");
    }

    @Test
    void testImplementation_Only_BHT_HL_Valid() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/sample837-small.edi");

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
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
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        URL schemaLocation = getClass().getResource("/x12/005010X222/837.xml");
        Schema schema = schemaFactory.createSchema(schemaLocation);
        reader.setTransactionSchema(schema);

        // Occurrence 1
        assertEquals(EDIStreamEvent.START_LOOP, reader.next());
        assertEquals("L0001", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.IMPLEMENTATION_UNUSED_SEGMENT_PRESENT, reader.getErrorType());
        assertEquals("NM1", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.IMPLEMENTATION_UNUSED_SEGMENT_PRESENT, reader.getErrorType());
        assertEquals("PER", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.END_LOOP, reader.next());
        assertEquals("L0001", reader.getReferenceCode());

        // Occurrence 2
        assertEquals(EDIStreamEvent.START_LOOP, reader.next());
        assertEquals("L0001", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.IMPLEMENTATION_UNUSED_SEGMENT_PRESENT, reader.getErrorType());
        assertEquals("NM1", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.END_LOOP, reader.next());
        assertEquals("L0001", reader.getReferenceCode());

        // Loop 2010A
        assertEquals(EDIStreamEvent.START_LOOP, reader.next());
        assertEquals("2010A", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.IMPLEMENTATION_UNUSED_SEGMENT_PRESENT, reader.getErrorType());
        assertEquals("PRV", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.END_LOOP, reader.next());
        assertEquals("2010A", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next(), "Expecting start of 2nd transaction");

        // Loop 2010A
        assertEquals(EDIStreamEvent.START_LOOP, reader.next());
        assertEquals("2010A", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.END_LOOP, reader.next());
        assertEquals("2010A", reader.getReferenceCode());

        assertTrue(!reader.hasNext(), "Unexpected events exist");
    }
}
