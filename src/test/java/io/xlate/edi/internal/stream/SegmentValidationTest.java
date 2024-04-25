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

import static io.xlate.edi.test.StaEDITestUtil.assertEvent;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.xlate.edi.internal.schema.SchemaUtils;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIOutputErrorReporter;
import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamConstants.Standards;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamFilter;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.EDIStreamWriter;
import io.xlate.edi.stream.Location;
import io.xlate.edi.test.StaEDITestEvent;
import io.xlate.edi.test.StaEDITestUtil;

@SuppressWarnings("resource")
class SegmentValidationTest {

    EDIStreamFilter segmentErrorFilter = new EDIStreamFilter() {
        @Override
        public boolean accept(EDIStreamReader reader) {
            switch (reader.getEventType()) {
            case START_TRANSACTION:
            case SEGMENT_ERROR:
            case ELEMENT_OCCURRENCE_ERROR:
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
        EDIStreamReader reader = StaEDITestUtil.filterEvents(
            factory,
            unfiltered,
            EDIStreamEvent.START_TRANSACTION,
            EDIStreamEvent.SEGMENT_ERROR,
            EDIStreamEvent.START_LOOP,
            EDIStreamEvent.END_LOOP);

        assertEvent(reader, EDIStreamEvent.START_TRANSACTION);
        reader.setTransactionSchema(schemaFactory.createSchema(getClass().getResource("/x12/EDISchemaSegmentValidationImpl.xml")));

        // Loop A
        assertEvent(reader, EDIStreamEvent.START_LOOP, "0000A");
        assertEvent(reader, EDIStreamEvent.SEGMENT_ERROR, EDIStreamValidationError.IMPLEMENTATION_UNUSED_SEGMENT_PRESENT, "S12");
        assertEvent(reader, EDIStreamEvent.SEGMENT_ERROR, EDIStreamValidationError.IMPLEMENTATION_SEGMENT_BELOW_MINIMUM_USE, "S13");
        assertEvent(reader, EDIStreamEvent.END_LOOP, "0000A");

        // Loop B - Occurrence 1
        assertEvent(reader, EDIStreamEvent.START_LOOP, "0000B");
        assertEvent(reader, EDIStreamEvent.SEGMENT_ERROR, EDIStreamValidationError.SEGMENT_EXCEEDS_MAXIMUM_USE, "S12");
        assertEvent(reader, EDIStreamEvent.END_LOOP, "0000B");

        // Loop B - Occurrence 2
        assertEvent(reader, EDIStreamEvent.START_LOOP, "0000B");
        assertEvent(reader, EDIStreamEvent.END_LOOP, "0000B");

        // Loop B - Occurrence 3
        assertEvent(reader, EDIStreamEvent.START_LOOP, "0000B");
        assertEvent(reader, EDIStreamEvent.SEGMENT_ERROR, EDIStreamValidationError.LOOP_OCCURS_OVER_MAXIMUM_TIMES, "S11", "0000B");
        assertEvent(reader, EDIStreamEvent.END_LOOP, "0000B");

        // Loop C - Occurrence 1
        assertEvent(reader, EDIStreamEvent.START_LOOP, "0000C");
        assertEvent(reader, EDIStreamEvent.END_LOOP, "0000C");

        // Loop D - Occurrence 1
        assertEvent(reader, EDIStreamEvent.START_LOOP, "0000D");

        // Standard Loop L0000 may only occur 5 times
        assertEvent(reader, EDIStreamEvent.SEGMENT_ERROR, EDIStreamValidationError.LOOP_OCCURS_OVER_MAXIMUM_TIMES, "S11", "L0000");
        assertEvent(reader, EDIStreamEvent.END_LOOP, "0000D");

        assertEvent(reader, EDIStreamEvent.SEGMENT_ERROR, EDIStreamValidationError.IMPLEMENTATION_LOOP_OCCURS_UNDER_MINIMUM_TIMES, "0000C");

        // Loop L0001 has minOccurs=1 in standard (not used in implementation, invalid configuration)
        assertEvent(reader, EDIStreamEvent.SEGMENT_ERROR, EDIStreamValidationError.MANDATORY_SEGMENT_MISSING, "S20");

        assertTrue(!reader.hasNext(), "Unexpected segment errors exist");
    }

    @SuppressWarnings("deprecation")
    @Test
    void testImplementationValidAlternateSequence() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        factory.setProperty(EDIInputFactory.EDI_ENABLE_LOOP_TEXT, "false");
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "S01*X~"
                + "S0A*X~"
                + "S11*A~"
                + "S13*X~"
                + "S11*C~"
                + "S11*B~"
                + "S12*X~"
                + "S12*X~"
                + "S11*D~"
                + "S11*C~"
                + "S09*X~"
                + "IEA*1*508121953~").getBytes());

        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        URL schemaLocation = getClass().getResource("/x12/EDISchemaSegmentValidation.xml");
        Schema schema = schemaFactory.createSchema(schemaLocation);

        EDIStreamReader unfiltered = factory.createEDIStreamReader(stream, schema);
        EDIStreamReader reader = StaEDITestUtil.filterEvents(
            factory,
            unfiltered,
            EDIStreamEvent.START_TRANSACTION,
            EDIStreamEvent.SEGMENT_ERROR,
            EDIStreamEvent.START_LOOP,
            EDIStreamEvent.END_LOOP);

        assertEvent(reader, EDIStreamEvent.START_TRANSACTION);
        reader.setTransactionSchema(schemaFactory.createSchema(getClass().getResource("/x12/EDISchemaSegmentValidationImpl.xml")));

        List<StaEDITestEvent> expected = Arrays.asList(
            StaEDITestEvent.forEvent(EDIStreamEvent.START_TRANSACTION, null, "TRANSACTION"),
            // Loop A
            StaEDITestEvent.forEvent(EDIStreamEvent.START_LOOP, null, "0000A"),
            StaEDITestEvent.forEvent(EDIStreamEvent.END_LOOP, null, "0000A"),
            // Loop C - Occurrence 1
            StaEDITestEvent.forEvent(EDIStreamEvent.START_LOOP, null, "0000C"),
            StaEDITestEvent.forEvent(EDIStreamEvent.END_LOOP, null, "0000C"),
            // Loop B
            StaEDITestEvent.forEvent(EDIStreamEvent.START_LOOP, null, "0000B"),
            StaEDITestEvent.forEvent(EDIStreamEvent.END_LOOP, null, "0000B"),
            // Loop D
            StaEDITestEvent.forEvent(EDIStreamEvent.START_LOOP, null, "0000D"),
            StaEDITestEvent.forEvent(EDIStreamEvent.END_LOOP, null, "0000D"),
            // Loop C - Occurrence 2
            StaEDITestEvent.forEvent(EDIStreamEvent.START_LOOP, null, "0000C"),
            StaEDITestEvent.forEvent(EDIStreamEvent.END_LOOP, null, "0000C"),

            StaEDITestEvent.forError(EDIStreamValidationError.MANDATORY_SEGMENT_MISSING, "S20", "S20"));

        List<StaEDITestEvent> events = new ArrayList<>();
        events.add(StaEDITestEvent.from(reader, false));

        while (reader.hasNext()) {
            reader.next();
            events.add(StaEDITestEvent.from(reader, false));
        }

        assertEquals(expected, events);
    }

    @Test
    void testImplementationValidSequenceAllMissingReader() throws EDISchemaException, EDIStreamException {
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
        reader = StaEDITestUtil.filterEvents(
            factory,
            reader,
            EDIStreamEvent.START_TRANSACTION,
            EDIStreamEvent.SEGMENT_ERROR,
            EDIStreamEvent.START_LOOP,
            EDIStreamEvent.END_LOOP);

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next(), "Expecting start of transaction");
        reader.setTransactionSchema(schemaFactory.createSchema(getClass().getResource("/x12/EDISchemaSegmentValidationImpl.xml")));

        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.IMPLEMENTATION_LOOP_OCCURS_UNDER_MINIMUM_TIMES, reader.getErrorType());
        assertEquals("0000A", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.IMPLEMENTATION_LOOP_OCCURS_UNDER_MINIMUM_TIMES, reader.getErrorType());
        assertEquals("0000B", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.IMPLEMENTATION_LOOP_OCCURS_UNDER_MINIMUM_TIMES, reader.getErrorType());
        assertEquals("0000C", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.IMPLEMENTATION_LOOP_OCCURS_UNDER_MINIMUM_TIMES, reader.getErrorType());
        assertEquals("0000D", reader.getReferenceCode());

        // Loop L0001 has minOccurs=1 in standard (not used in implementation, invalid configuration)
        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.MANDATORY_SEGMENT_MISSING, reader.getErrorType());
        assertEquals("S20", reader.getReferenceCode());

        assertTrue(!reader.hasNext(), "Unexpected segment errors exist");
    }

    @Test
    void testImplementationValidSequenceAllMissingWriter() throws EDISchemaException, EDIStreamException {
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema ctlSchema = schemaFactory.createSchema(getClass().getResource("/x12/EDISchemaSegmentValidation.xml"));
        Schema txnSchema = schemaFactory.createSchema(getClass().getResource("/x12/EDISchemaSegmentValidationImpl.xml"));

        List<StaEDITestEvent> errorEvents = new ArrayList<>();

        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        factory.setProperty(EDIOutputFactory.FORMAT_ELEMENTS, true);
        factory.setErrorReporter(new EDIOutputErrorReporter() {
            @Override
            public void report(EDIStreamValidationError errorType,
                               EDIStreamWriter writer,
                               Location location,
                               CharSequence data,
                               EDIReference typeReference) {
                EDIType type = typeReference instanceof EDIType ? EDIType.class.cast(typeReference) : typeReference.getReferencedType();
                errorEvents.add(StaEDITestEvent.forError(errorType, data.toString(), type.getCode()));
            }
        });

        EDIStreamWriter writer = factory.createEDIStreamWriter(new ByteArrayOutputStream());
        writer.setControlSchema(ctlSchema);
        writer.setTransactionSchema(txnSchema);

        writer.startInterchange();
        writer.writeStartSegment("ISA")
            .writeElement("00")
            .writeElement(" ")
            .writeElement("00")
            .writeElement(" ")
            .writeElement("ZZ")
            .writeElement("ReceiverID")
            .writeElement("ZZ")
            .writeElement("Sender")
            .writeElement("203001")
            .writeElement("1430")
            .writeElement("^")
            .writeElement("00501")
            .writeElement("000000001")
            .writeElement("0")
            .writeElement("P")
            .writeElement(":")
            .writeEndSegment();
        writer.writeStartSegment("S01")
            .writeElement("X")
            .writeEndSegment();
        writer.writeStartSegment("S09")
            .writeElement("X")
            .writeEndSegment();
        writer.writeStartSegment("IEA")
            .writeElement("1")
            .writeElement("000000001")
            .writeEndSegment();

        List<StaEDITestEvent> expected = Arrays.asList(
            StaEDITestEvent.forError(EDIStreamValidationError.IMPLEMENTATION_LOOP_OCCURS_UNDER_MINIMUM_TIMES, "L0000", "0000A"),
            StaEDITestEvent.forError(EDIStreamValidationError.IMPLEMENTATION_LOOP_OCCURS_UNDER_MINIMUM_TIMES, "L0000", "0000B"),
            StaEDITestEvent.forError(EDIStreamValidationError.IMPLEMENTATION_LOOP_OCCURS_UNDER_MINIMUM_TIMES, "L0000", "0000C"),
            StaEDITestEvent.forError(EDIStreamValidationError.IMPLEMENTATION_LOOP_OCCURS_UNDER_MINIMUM_TIMES, "L0000", "0000D"),
            StaEDITestEvent.forError(EDIStreamValidationError.MANDATORY_SEGMENT_MISSING, "S20", "S20")
        );

        assertEquals(expected, errorEvents);
    }

    @Test
    void testImplementationLoopsSelectedByWriter() throws EDISchemaException, EDIStreamException {
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema ctlSchema = schemaFactory.createSchema(getClass().getResource("/x12/EDISchemaSegmentValidation.xml"));
        Schema txnSchema = schemaFactory.createSchema(getClass().getResource("/x12/EDISchemaSegmentValidationImpl.xml"));

        List<StaEDITestEvent> errorEvents = new ArrayList<>();

        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        factory.setProperty(EDIOutputFactory.FORMAT_ELEMENTS, true);
        factory.setErrorReporter(new EDIOutputErrorReporter() {
            @Override
            public void report(EDIStreamValidationError errorType,
                               EDIStreamWriter writer,
                               Location location,
                               CharSequence data,
                               EDIReference typeReference) {
                EDIType type = typeReference instanceof EDIType ? EDIType.class.cast(typeReference) : typeReference.getReferencedType();
                errorEvents.add(StaEDITestEvent.forError(errorType, data.toString(), type.getCode()));
            }
        });

        EDIStreamWriter writer = factory.createEDIStreamWriter(new ByteArrayOutputStream());
        writer.setControlSchema(ctlSchema);
        writer.setTransactionSchema(txnSchema);

        writer.startInterchange();
        writer.writeStartSegment("ISA")
            .writeElement("00")
            .writeElement(" ")
            .writeElement("00")
            .writeElement(" ")
            .writeElement("ZZ")
            .writeElement("ReceiverID")
            .writeElement("ZZ")
            .writeElement("Sender")
            .writeElement("203001")
            .writeElement("1430")
            .writeElement("^")
            .writeElement("00501")
            .writeElement("000000001")
            .writeElement("0")
            .writeElement("P")
            .writeElement(":")
            .writeEndSegment();
        writer.writeStartSegment("S01")
            .writeElement("X")
            .writeEndSegment();

        // 0000A
        writer.writeStartSegment("S11")
            .writeElement("A")
            .writeEndSegment();
        // Unused for impl loop 0000A
        writer.writeStartSegment("S12")
            .writeElement("X")
            .writeEndSegment();
        // skipping S13, required in 0000A

        // 0000B
        for (int i = 0; i < 3; i++) {
            // loop can only appear twice, but we write 3 times
            writer.writeStartSegment("S11")
                .writeElement("B")
                .writeEndSegment();
        }

        // 0000C - requires exactly two occurrence, but we write once
        writer.writeStartSegment("S11")
            .writeElement("C")
            .writeEndSegment();

        // S20 is unused by the impl, but required by the standard (i.e. the impl is buggy/disagrees w/standard)
        writer.writeStartSegment("S20")
            .writeElement("X")
            .writeEndSegment();

        writer.writeStartSegment("S09")
            .writeElement("X")
            .writeEndSegment();
        writer.writeStartSegment("IEA")
            .writeElement("1")
            .writeElement("000000001")
            .writeEndSegment();

        List<StaEDITestEvent> expected = Arrays.asList(
            StaEDITestEvent.forError(EDIStreamValidationError.IMPLEMENTATION_UNUSED_SEGMENT_PRESENT, "S12", "S12"),
            StaEDITestEvent.forError(EDIStreamValidationError.IMPLEMENTATION_SEGMENT_BELOW_MINIMUM_USE, "S13", "S13"),
            StaEDITestEvent.forError(EDIStreamValidationError.LOOP_OCCURS_OVER_MAXIMUM_TIMES, "S11", "0000B"),
            StaEDITestEvent.forError(EDIStreamValidationError.IMPLEMENTATION_LOOP_OCCURS_UNDER_MINIMUM_TIMES, "L0000", "0000C"),
            StaEDITestEvent.forError(EDIStreamValidationError.IMPLEMENTATION_LOOP_OCCURS_UNDER_MINIMUM_TIMES, "L0000", "0000D"),
            StaEDITestEvent.forError(EDIStreamValidationError.IMPLEMENTATION_UNUSED_SEGMENT_PRESENT, "S20", "S20")
        );

        assertEquals(expected, errorEvents);
    }

    @SuppressWarnings("deprecation")
    @Test
    void testImplementationValidSequenceWithCompositeDiscr() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        factory.setProperty(EDIInputFactory.EDI_ENABLE_LOOP_TEXT, "false");
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
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

        URL schemaLocation = getClass().getResource("/x12/EDISchemaSegmentValidation.xml");
        Schema schema = schemaFactory.createSchema(schemaLocation);

        EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);
        reader = StaEDITestUtil.filterEvents(
            factory,
            reader,
            EDIStreamEvent.START_TRANSACTION,
            EDIStreamEvent.SEGMENT_ERROR,
            EDIStreamEvent.START_LOOP,
            EDIStreamEvent.END_LOOP);

        assertEvent(reader, EDIStreamEvent.START_TRANSACTION);
        reader.setTransactionSchema(schemaFactory.createSchema(getClass().getResource("/x12/EDISchemaSegmentValidationImpl2.xml")));

        List<StaEDITestEvent> expected = Arrays.asList(
            StaEDITestEvent.forEvent(EDIStreamEvent.START_TRANSACTION, null, "TRANSACTION"),
            // Loop A
            StaEDITestEvent.forEvent(EDIStreamEvent.START_LOOP, null, "0000A"),
            // Loop AXX
            StaEDITestEvent.forEvent(EDIStreamEvent.START_LOOP, null, "0002AXX"),
            StaEDITestEvent.forEvent(EDIStreamEvent.END_LOOP, null, "0002AXX"),
            // Loop AYY
            StaEDITestEvent.forEvent(EDIStreamEvent.START_LOOP, null, "0002AYY"),
            StaEDITestEvent.forError(EDIStreamValidationError.SEGMENT_EXCEEDS_MAXIMUM_USE, "S31", "S31"),
            StaEDITestEvent.forError(EDIStreamValidationError.IMPLEMENTATION_SEGMENT_BELOW_MINIMUM_USE, "S31", "S31B"),
            StaEDITestEvent.forEvent(EDIStreamEvent.END_LOOP, null, "0002AYY"),
            StaEDITestEvent.forEvent(EDIStreamEvent.END_LOOP, null, "0000A"));

        List<StaEDITestEvent> events = new ArrayList<>();
        events.add(StaEDITestEvent.from(reader, false));

        while (reader.hasNext()) {
            reader.next();
            events.add(StaEDITestEvent.from(reader, false));
        }

        assertEquals(expected, events);
    }

    @ParameterizedTest
    @CsvSource({
        "'41' ,      , '1000A'",
        "'41 ', true , '1000A'",
        "'41 ', false, 'L0001'",
        "'41 ',      , 'L0001'",
        "' 41',      , 'L0001'",
        "' 41', true , '1000A'",
        "' 41', false, 'L0001'",
    })
    void testTrimDiscriminatorValue(String nm101, Boolean trimDiscriminator, String expectedLoopId) throws EDIStreamException, EDISchemaException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        ByteArrayInputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *200711*0100*^*00501*000000001*0*T*:~"
                + "GS*HC*99999999999*888888888888*20111219*1340*1*X*005010X222~"
                + "ST*837*0001*005010X222~"
                + "BHT*0019*00*565743*20110523*154959*CH~"
                + "NM1*" + nm101 + "*2*SAMPLE INC*****46*496103~"
                // Skip below in test
                + "PER*IC*EDI DEPT*EM*FEEDBACK@example.com*TE*3305551212~"
                + "NM1*40*2*PPO BLUE*****46*54771~"
                + "HL*1**20*1~"
                + "HL*2*1*22*0~"
                + "SE*8*0001~"
                + "GE*1*1~"
                + "IEA*1*000000001~").getBytes());

        factory.setProperty(EDIInputFactory.EDI_TRIM_DISCRIMINATOR_VALUES, trimDiscriminator);
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        List<EDIStreamValidationError> errors = new ArrayList<>();
        String loopId = null;

        while (loopId == null && reader.hasNext()) {
            switch (reader.next()) {
            case START_TRANSACTION:
                reader.setTransactionSchema(SchemaFactory.newFactory()
                                                         .createSchema(getClass().getResource("/x12/005010X222/837_loop1000_only.xml")));
                break;
            case START_LOOP:
                loopId = reader.getReferenceCode();
                break;
            case SEGMENT_ERROR:
            case ELEMENT_OCCURRENCE_ERROR:
            case ELEMENT_DATA_ERROR:
                errors.add(reader.getErrorType());
                System.out.println("Unexpected error: " + reader.getErrorType() + "; " + reader.getText() + "; " + reader.getLocation());
                break;
            default:
                break;
            }
        }

        assertEquals(0, errors.size(), () -> errors.toString());

        assertEquals(expectedLoopId, loopId);
    }

    @SuppressWarnings("deprecation")
    @Test
    void testImplementation_Only_BHT_HL_Valid() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        factory.setProperty(EDIInputFactory.EDI_ENABLE_LOOP_TEXT, "false");
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/sample837-small.edi");

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        reader = StaEDITestUtil.filterEvents(
            factory,
            reader,
            EDIStreamEvent.START_TRANSACTION,
            EDIStreamEvent.SEGMENT_ERROR,
            EDIStreamEvent.START_LOOP,
            EDIStreamEvent.END_LOOP);

        assertEvent(reader, EDIStreamEvent.START_TRANSACTION);
        reader.setTransactionSchema(schemaFactory.createSchema(getClass().getResource("/x12/005010X222/837.xml")));

        List<StaEDITestEvent> expected = Arrays.asList(
            StaEDITestEvent.forEvent(EDIStreamEvent.START_TRANSACTION, null, "TRANSACTION"),
            // Occurrence 1
            StaEDITestEvent.forEvent(EDIStreamEvent.START_LOOP, null, "L0001"),
            StaEDITestEvent.forError(EDIStreamValidationError.IMPLEMENTATION_UNUSED_SEGMENT_PRESENT, "NM1", "NM1"),
            StaEDITestEvent.forError(EDIStreamValidationError.IMPLEMENTATION_UNUSED_SEGMENT_PRESENT, "PER", "PER"),
            StaEDITestEvent.forEvent(EDIStreamEvent.END_LOOP, null, "L0001"),
            // Occurrence 2
            StaEDITestEvent.forEvent(EDIStreamEvent.START_LOOP, null, "L0001"),
            StaEDITestEvent.forError(EDIStreamValidationError.IMPLEMENTATION_UNUSED_SEGMENT_PRESENT, "NM1", "NM1"),
            StaEDITestEvent.forEvent(EDIStreamEvent.END_LOOP, null, "L0001"),
            // Loop 2010A
            StaEDITestEvent.forEvent(EDIStreamEvent.START_LOOP, null, "2010A"),
            StaEDITestEvent.forError(EDIStreamValidationError.IMPLEMENTATION_UNUSED_SEGMENT_PRESENT, "PRV", "PRV"),
            StaEDITestEvent.forEvent(EDIStreamEvent.END_LOOP, null, "2010A"),
            StaEDITestEvent.forEvent(EDIStreamEvent.START_TRANSACTION, null, "TRANSACTION"),
            // Loop 2010A
            StaEDITestEvent.forEvent(EDIStreamEvent.START_LOOP, null, "2010A"),
            StaEDITestEvent.forEvent(EDIStreamEvent.END_LOOP, null, "2010A"));

        List<StaEDITestEvent> events = new ArrayList<>();
        events.add(StaEDITestEvent.from(reader, false));

        while (reader.hasNext()) {
            reader.next();
            events.add(StaEDITestEvent.from(reader, false));
        }

        assertEquals(expected, events);
    }

    @Test
    void testImplUnusedSegmentErrorWhenNotMatched() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/issue229/837-header-ref-only.edi");

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        reader = StaEDITestUtil.filterEvents(
            factory,
            reader,
            EDIStreamEvent.START_TRANSACTION,
            EDIStreamEvent.SEGMENT_ERROR,
            EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR,
            EDIStreamEvent.START_LOOP,
            EDIStreamEvent.END_LOOP);

        assertEvent(reader, EDIStreamEvent.START_TRANSACTION);
        reader.setTransactionSchema(schemaFactory.createSchema(getClass().getResource("/x12/005010X222/837_REF_impls.xml")));

        List<StaEDITestEvent> expected = Arrays.asList(
            StaEDITestEvent.forEvent(EDIStreamEvent.START_TRANSACTION, "TRANSACTION", "TRANSACTION"),
            StaEDITestEvent.forError(EDIStreamValidationError.REQUIRED_DATA_ELEMENT_MISSING, "", "REF0402"),
            StaEDITestEvent.forError(EDIStreamValidationError.IMPLEMENTATION_UNUSED_SEGMENT_PRESENT, "REF", "REF"),
            StaEDITestEvent.forError(EDIStreamValidationError.MANDATORY_SEGMENT_MISSING, "HL", "HL"));

        List<StaEDITestEvent> events = new ArrayList<>();
        events.add(StaEDITestEvent.from(reader, false));

        while (reader.hasNext()) {
            reader.next();
            events.add(StaEDITestEvent.from(reader, false));
        }

        assertEquals(expected, events);
    }
}
