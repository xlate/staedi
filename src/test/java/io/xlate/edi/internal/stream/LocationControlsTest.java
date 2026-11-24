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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamConstants.Standards;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.EDIStreamWriter;
import io.xlate.edi.stream.EDIValidationException;
import io.xlate.edi.stream.Location;

class LocationControlsTest {

    static final String ISA = "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *200301*1430*^*00501*000000001*0*P*:~";

    /*
     * One interchange, one group, one transaction. Each case breaks one
     * trailer element. The correct trailers are SE*4*0001, GE*1*1 and
     * IEA*1*000000001.
     */
    static String x12(String se, String ge, String iea) {
        return ISA
            + "GS*FA*ReceiverDept*SenderDept*20200301*1430*1*X*005010~"
            + "ST*997*0001~"
            + "AK1*IN*1~"
            + "AK9*A*1*1*1~"
            + se + "~"
            + ge + "~"
            + iea + "~";
    }

    static List<Location> readErrors(String edi, List<EDIStreamValidationError> errors, List<String> texts) throws Exception {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        List<Location> locations = new ArrayList<>();

        try (EDIStreamReader reader = factory.createEDIStreamReader(new ByteArrayInputStream(edi.getBytes()))) {
            while (reader.hasNext()) {
                if (reader.next() == EDIStreamEvent.ELEMENT_DATA_ERROR) {
                    errors.add(reader.getErrorType());
                    texts.add(reader.getText());
                    locations.add(reader.getLocation().copy());
                }
            }
        }

        return locations;
    }

    @ParameterizedTest
    @CsvSource({
        // trailer segments, error, scope, trailer text, expected count, expected reference
        "SE*9*0001, GE*1*1, IEA*1*000000001, CONTROL_COUNT_DOES_NOT_MATCH_ACTUAL_COUNT, TRANSACTION, 9, 4, 0001",
        "SE*4*0002, GE*1*1, IEA*1*000000001, CONTROL_REFERENCE_MISMATCH, TRANSACTION, 0002, 4, 0001",
        "SE*4*0001, GE*3*1, IEA*1*000000001, CONTROL_COUNT_DOES_NOT_MATCH_ACTUAL_COUNT, GROUP, 3, 1, 1",
        "SE*4*0001, GE*1*7, IEA*1*000000001, CONTROL_REFERENCE_MISMATCH, GROUP, 7, 1, 1",
        "SE*4*0001, GE*1*1, IEA*5*000000001, CONTROL_COUNT_DOES_NOT_MATCH_ACTUAL_COUNT, INTERCHANGE, 5, 1, 000000001",
        "SE*4*0001, GE*1*1, IEA*1*000000009, CONTROL_REFERENCE_MISMATCH, INTERCHANGE, 000000009, 1, 000000001",
    })
    void testReaderControlErrorAtEachLevel(String se, String ge, String iea,
                                           EDIStreamValidationError error,
                                           EDIType.Type scope,
                                           String trailerText,
                                           int expectedCount,
                                           String expectedReference) throws Exception {
        List<EDIStreamValidationError> errors = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        List<Location> locations = readErrors(x12(se, ge, iea), errors, texts);

        assertEquals(Arrays.asList(error), errors);
        assertEquals(trailerText, texts.get(0));

        Location location = locations.get(0);
        assertEquals(expectedCount, location.getControlCount(scope));
        assertEquals(expectedReference, location.getControlReference(scope));
        // The trailer's own structure is the innermost open one
        assertEquals(expectedCount, location.getControlCount());
        assertEquals(expectedReference, location.getControlReference());
    }

    @Test
    void testReaderNestedValuesAtTransactionTrailer() throws Exception {
        List<EDIStreamValidationError> errors = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        Location location = readErrors(x12("SE*9*0001", "GE*1*1", "IEA*1*000000001"), errors, texts).get(0);

        assertEquals(4, location.getControlCount(EDIType.Type.TRANSACTION));
        assertEquals("0001", location.getControlReference(EDIType.Type.TRANSACTION));
        assertEquals(1, location.getControlCount(EDIType.Type.GROUP));
        assertEquals("1", location.getControlReference(EDIType.Type.GROUP));
        assertEquals(1, location.getControlCount(EDIType.Type.INTERCHANGE));
        assertEquals("000000001", location.getControlReference(EDIType.Type.INTERCHANGE));
    }

    @Test
    void testReaderValuesOutsideStructures() throws Exception {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        String edi = x12("SE*4*0001", "GE*1*1", "IEA*1*000000001");

        try (EDIStreamReader reader = factory.createEDIStreamReader(new ByteArrayInputStream(edi.getBytes()))) {
            while (reader.hasNext()) {
                EDIStreamEvent event = reader.next();

                if (event == EDIStreamEvent.START_SEGMENT && "GS".equals(reader.getText())) {
                    // The group is open, but its header reference is not read yet
                    Location location = reader.getLocation();
                    assertNull(location.getControlReference());
                    assertEquals(0, location.getControlCount());
                    assertEquals("000000001", location.getControlReference(EDIType.Type.INTERCHANGE));
                    assertEquals(-1, location.getControlCount(EDIType.Type.TRANSACTION));
                    assertNull(location.getControlReference(EDIType.Type.TRANSACTION));
                } else if (event == EDIStreamEvent.START_SEGMENT && "IEA".equals(reader.getText())) {
                    // Group and transaction closed
                    Location location = reader.getLocation();
                    assertNull(location.getControlReference(EDIType.Type.GROUP));
                    assertNull(location.getControlReference(EDIType.Type.TRANSACTION));
                    assertEquals(1, location.getControlCount());
                }
            }
        }
    }

    static String controls(Location location) {
        StringBuilder result = new StringBuilder();
        for (EDIType.Type scope : new EDIType.Type[] { EDIType.Type.INTERCHANGE, EDIType.Type.GROUP, EDIType.Type.TRANSACTION }) {
            result.append(location.getControlCount(scope)).append('/').append(location.getControlReference(scope)).append(' ');
        }
        return result.append(location.getControlCount()).append('/').append(location.getControlReference()).toString();
    }

    @Test
    void testReaderValuesAcrossRepeatedStructures() throws Exception {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        String edi = ISA
            + "GS*FA*ReceiverDept*SenderDept*20200301*1430*1*X*005010~"
            + "ST*997*0001~AK1*IN*1~AK9*A*1*1*1~SE*4*0001~"
            + "ST*997*0002~AK1*IN*1~SE*9*0002~"
            + "GE*2*1~"
            + "GS*FA*ReceiverDept*SenderDept*20200301*1430*2*X*005010~"
            + "ST*997*0003~AK1*IN*1~SE*3*0003~"
            + "GE*1*2~"
            + "IEA*2*000000001~";
        List<String> actual = new ArrayList<>();

        try (EDIStreamReader reader = factory.createEDIStreamReader(new ByteArrayInputStream(edi.getBytes()))) {
            while (reader.hasNext()) {
                EDIStreamEvent event = reader.next();

                switch (event) {
                case START_GROUP:
                case START_TRANSACTION:
                case END_TRANSACTION:
                case END_GROUP:
                    actual.add(event + " " + controls(reader.getLocation()));
                    break;
                case START_SEGMENT:
                case ELEMENT_DATA_ERROR:
                    actual.add(event + " " + reader.getText() + " " + controls(reader.getLocation()));
                    break;
                default:
                    break;
                }
            }
        }

        assertEquals(Arrays.asList(
            "START_SEGMENT ISA 0/null -1/null -1/null 0/null",
            "START_GROUP 1/000000001 0/null -1/null 0/null",
            "START_SEGMENT GS 1/000000001 0/null -1/null 0/null",
            "START_TRANSACTION 1/000000001 1/1 0/null 0/null",
            "START_SEGMENT ST 1/000000001 1/1 1/null 1/null",
            "START_SEGMENT AK1 1/000000001 1/1 2/0001 2/0001",
            "START_SEGMENT AK9 1/000000001 1/1 3/0001 3/0001",
            "START_SEGMENT SE 1/000000001 1/1 4/0001 4/0001",
            "END_TRANSACTION 1/000000001 1/1 -1/null 1/1",
            // A new occurrence starts with no values from the one before it
            "START_TRANSACTION 1/000000001 2/1 0/null 0/null",
            "START_SEGMENT ST 1/000000001 2/1 1/null 1/null",
            "START_SEGMENT AK1 1/000000001 2/1 2/0002 2/0002",
            "START_SEGMENT SE 1/000000001 2/1 3/0002 3/0002",
            "ELEMENT_DATA_ERROR 9 1/000000001 2/1 3/0002 3/0002",
            "END_TRANSACTION 1/000000001 2/1 -1/null 2/1",
            "START_SEGMENT GE 1/000000001 2/1 -1/null 2/1",
            "END_GROUP 1/000000001 -1/null -1/null 1/000000001",
            "START_GROUP 2/000000001 0/null -1/null 0/null",
            "START_SEGMENT GS 2/000000001 0/null -1/null 0/null",
            "START_TRANSACTION 2/000000001 1/2 0/null 0/null",
            "START_SEGMENT ST 2/000000001 1/2 1/null 1/null",
            "START_SEGMENT AK1 2/000000001 1/2 2/0003 2/0003",
            "START_SEGMENT SE 2/000000001 1/2 3/0003 3/0003",
            "END_TRANSACTION 2/000000001 1/2 -1/null 1/2",
            "START_SEGMENT GE 2/000000001 1/2 -1/null 1/2",
            "END_GROUP 2/000000001 -1/null -1/null 2/000000001",
            "START_SEGMENT IEA 2/000000001 -1/null -1/null 2/000000001"), actual);
    }

    @ParameterizedTest
    @EnumSource(value = EDIType.Type.class, names = { "LOOP", "SEGMENT", "COMPOSITE", "ELEMENT" })
    void testInvalidScopeThrows(EDIType.Type scope) {
        Location location = new StaEDIStreamLocation();
        assertThrows(IllegalArgumentException.class, () -> location.getControlCount(scope));
        assertThrows(IllegalArgumentException.class, () -> location.getControlReference(scope));
    }

    @Test
    void testNullScopeThrows() {
        Location location = new StaEDIStreamLocation();
        assertThrows(IllegalArgumentException.class, () -> location.getControlCount(null));
        assertThrows(IllegalArgumentException.class, () -> location.getControlReference(null));
    }

    @Test
    void testDefaultMethodsWithoutTracking() {
        Location location = new Location() {
            public int getLineNumber() { return -1; }
            public int getColumnNumber() { return -1; }
            public int getCharacterOffset() { return -1; }
            public int getSegmentPosition() { return -1; }
            public String getSegmentTag() { return null; }
            public int getElementPosition() { return -1; }
            public int getElementOccurrence() { return -1; }
            public int getComponentPosition() { return -1; }
            public Location copy() { return this; }
        };

        assertEquals(-1, location.getControlCount(EDIType.Type.GROUP));
        assertEquals(-1, location.getControlCount());
        assertNull(location.getControlReference(EDIType.Type.GROUP));
        assertNull(location.getControlReference());
    }

    static EDIStreamWriter newWriter(ByteArrayOutputStream stream) throws Exception {
        EDIStreamWriter writer = EDIOutputFactory.newFactory().createEDIStreamWriter(stream);
        writer.setControlSchema(SchemaFactory.newFactory().getControlSchema(Standards.X12, new String[] { "00501" }));
        writer.startInterchange();
        writer.writeStartSegment("ISA")
            .writeElement("00").writeElement("          ")
            .writeElement("00").writeElement("          ")
            .writeElement("ZZ").writeElement("ReceiverID     ")
            .writeElement("ZZ").writeElement("Sender         ")
            .writeElement("200301").writeElement("1430")
            .writeElement("^").writeElement("00501")
            .writeElement("000000001").writeElement("0")
            .writeElement("P").writeElement(":")
            .writeEndSegment();
        writer.writeStartSegment("GS")
            .writeElement("FA").writeElement("ReceiverDept").writeElement("SenderDept")
            .writeElement("20200301").writeElement("1430").writeElement("1")
            .writeElement("X").writeElement("005010")
            .writeEndSegment();
        writer.writeStartSegment("ST").writeElement("997").writeElement("0001").writeEndSegment();
        writer.writeStartSegment("AK1").writeElement("IN").writeElement("1").writeEndSegment();
        return writer;
    }

    @ParameterizedTest
    @CsvSource({
        // level, count element, reference element, error, expected count, expected reference
        "TRANSACTION, 9, 0001, CONTROL_COUNT_DOES_NOT_MATCH_ACTUAL_COUNT, 3, 0001",
        "TRANSACTION, 3, 0002, CONTROL_REFERENCE_MISMATCH, 3, 0001",
        "GROUP, 3, 1, CONTROL_COUNT_DOES_NOT_MATCH_ACTUAL_COUNT, 1, 1",
        "GROUP, 1, 7, CONTROL_REFERENCE_MISMATCH, 1, 1",
        "INTERCHANGE, 5, 000000001, CONTROL_COUNT_DOES_NOT_MATCH_ACTUAL_COUNT, 1, 000000001",
        "INTERCHANGE, 1, 000000009, CONTROL_REFERENCE_MISMATCH, 1, 000000001",
    })
    void testWriterControlErrorAtEachLevel(EDIType.Type scope,
                                           String countValue,
                                           String referenceValue,
                                           EDIStreamValidationError error,
                                           int expectedCount,
                                           String expectedReference) throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(4096);
        EDIValidationException thrown;

        try (EDIStreamWriter writer = newWriter(stream)) {
            String se = scope == EDIType.Type.TRANSACTION ? countValue : "3";
            String seRef = scope == EDIType.Type.TRANSACTION ? referenceValue : "0001";

            if (scope == EDIType.Type.TRANSACTION) {
                writer.writeStartSegment("SE");
                thrown = writeTrailer(writer, se, seRef, error);
            } else {
                writer.writeStartSegment("SE").writeElement(se).writeElement(seRef).writeEndSegment();

                if (scope == EDIType.Type.GROUP) {
                    writer.writeStartSegment("GE");
                    thrown = writeTrailer(writer, countValue, referenceValue, error);
                } else {
                    writer.writeStartSegment("GE").writeElement("1").writeElement("1").writeEndSegment();
                    writer.writeStartSegment("IEA");
                    thrown = writeTrailer(writer, countValue, referenceValue, error);
                }
            }
        }

        assertEquals(error, thrown.getError());
        Location location = thrown.getLocation();
        assertEquals(expectedCount, location.getControlCount(scope));
        assertEquals(expectedReference, location.getControlReference(scope));
        assertEquals(expectedCount, location.getControlCount());
        assertEquals(expectedReference, location.getControlReference());
    }

    static EDIValidationException writeTrailer(EDIStreamWriter writer, String count, String reference, EDIStreamValidationError error) throws Exception {
        if (error == EDIStreamValidationError.CONTROL_COUNT_DOES_NOT_MATCH_ACTUAL_COUNT) {
            return assertThrows(EDIValidationException.class, () -> writer.writeElement(count));
        }
        writer.writeElement(count);
        return assertThrows(EDIValidationException.class, () -> writer.writeElement(reference));
    }
}
