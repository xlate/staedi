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

import static io.xlate.edi.test.StaEDITestUtil.assertLocation;
import static io.xlate.edi.test.StaEDITestUtil.assertTextLocation;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import io.xlate.edi.internal.schema.SchemaUtils;
import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.EDIType.Type;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamConstants.Delimiters;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.Location;

@SuppressWarnings({ "resource", "unused" })
class StaEDIStreamReaderTest implements ConstantsTest {

    private Set<EDIStreamEvent> possible = new HashSet<>();

    public StaEDIStreamReaderTest() {
        possible.addAll(Arrays.asList(EDIStreamEvent.ELEMENT_DATA,
                                      EDIStreamEvent.START_INTERCHANGE,
                                      EDIStreamEvent.START_GROUP,
                                      EDIStreamEvent.START_TRANSACTION,
                                      EDIStreamEvent.START_LOOP,
                                      EDIStreamEvent.START_SEGMENT,
                                      EDIStreamEvent.START_COMPOSITE,
                                      EDIStreamEvent.END_INTERCHANGE,
                                      EDIStreamEvent.END_GROUP,
                                      EDIStreamEvent.END_TRANSACTION,
                                      EDIStreamEvent.END_LOOP,
                                      EDIStreamEvent.END_SEGMENT,
                                      EDIStreamEvent.END_COMPOSITE));
    }

    @Test
    void testGetProperty() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        assertNull(reader.getProperty("NONE"), "Property was not null");
        assertThrows(IllegalArgumentException.class, () -> reader.getProperty(null));
    }

    @Test
    void testGetDelimitersX12() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        Map<String, Character> expected = new HashMap<>(5);
        expected.put(Delimiters.SEGMENT, '~');
        expected.put(Delimiters.DATA_ELEMENT, '*');
        expected.put(Delimiters.COMPONENT_ELEMENT, ':');
        expected.put(Delimiters.REPETITION, '^');
        expected.put(Delimiters.DECIMAL, '.');

        Map<String, Character> delimiters = null;
        int delimiterExceptions = 0;

        while (reader.hasNext()) {
            try {
                reader.getDelimiters();
            } catch (IllegalStateException e) {
                delimiterExceptions++;
            }

            if (reader.next() == EDIStreamEvent.START_INTERCHANGE) {
                delimiters = reader.getDelimiters();
            }
        }

        assertEquals(expected, delimiters, "Unexpected delimiters");
        assertEquals(1, delimiterExceptions, "Unexpected exceptions");
    }

    @ParameterizedTest
    @CsvSource({
                 "Basic TRADACOMS, /TRADACOMS/order.edi",
                 "Basic TRADACOMS (extra MHD data), /TRADACOMS/order-extra-mhd-data.edi",
    })
    void testGetDelimitersTRADACOMS(String title, String resourceName) throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream(resourceName);
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        Map<String, Character> expected = new HashMap<>(5);
        expected.put(Delimiters.SEGMENT, '\'');
        expected.put(Delimiters.DATA_ELEMENT, '+');
        expected.put(Delimiters.COMPONENT_ELEMENT, ':');
        expected.put(Delimiters.RELEASE, '?');

        Map<String, Character> delimiters = null;
        int delimiterExceptions = 0;

        while (reader.hasNext()) {
            try {
                reader.getDelimiters();
            } catch (IllegalStateException e) {
                delimiterExceptions++;
            }

            if (reader.next() == EDIStreamEvent.START_INTERCHANGE) {
                delimiters = reader.getDelimiters();
            }
        }

        assertEquals(expected, delimiters, "Unexpected delimiters");
        assertEquals(1, delimiterExceptions, "Unexpected exceptions");
    }

    @Test
    void testGetDelimitersEDIFACTA() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/EDIFACT/invoic_d93a_una.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        Map<String, Character> expected = new HashMap<>(5);
        expected.put(Delimiters.SEGMENT, '\'');
        expected.put(Delimiters.DATA_ELEMENT, '+');
        expected.put(Delimiters.COMPONENT_ELEMENT, ':');
        expected.put(Delimiters.RELEASE, '?');
        expected.put(Delimiters.DECIMAL, ',');

        Map<String, Character> delimiters = null;
        int delimiterExceptions = 0;

        while (reader.hasNext()) {
            try {
                reader.getDelimiters();
            } catch (IllegalStateException e) {
                delimiterExceptions++;
            }

            if (reader.next() == EDIStreamEvent.START_INTERCHANGE) {
                delimiters = reader.getDelimiters();
            }
        }

        assertEquals(expected, delimiters, "Unexpected delimiters");
        assertEquals(1, delimiterExceptions, "Unexpected exceptions");
    }

    @Test
    void testGetDelimitersEDIFACTB() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/EDIFACT/invoic_d97b.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        Map<String, Character> expected = new HashMap<>(5);
        expected.put(Delimiters.SEGMENT, '\'');
        expected.put(Delimiters.DATA_ELEMENT, '+');
        expected.put(Delimiters.COMPONENT_ELEMENT, ':');
        // expected.put(Delimiters.REPETITION, '*'); Input is version 3. No repetition character until version 4
        expected.put(Delimiters.RELEASE, '?');
        expected.put(Delimiters.DECIMAL, '.');

        Map<String, Character> delimiters = null;
        int delimiterExceptions = 0;

        while (reader.hasNext()) {
            try {
                reader.getDelimiters();
            } catch (IllegalStateException e) {
                delimiterExceptions++;
            }

            if (reader.next() == EDIStreamEvent.START_INTERCHANGE) {
                delimiters = reader.getDelimiters();
            }
        }

        assertEquals(expected, delimiters, "Unexpected delimiters");
        assertEquals(1, delimiterExceptions, "Unexpected exceptions");
    }

    @Test
    void testAlternateEncodingEDIFACT() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/EDIFACT/invoic_d97b.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        int matches = 0;

        while (reader.hasNext()) {
            switch (reader.next()) {
            case ELEMENT_DATA:
                Location location = reader.getLocation();
                if ("NAD".equals(location.getSegmentTag())
                        && location.getSegmentPosition() == 7
                        && location.getElementPosition() == 4) {
                    assertEquals("BÜTTNER WIDGET COMPANY", reader.getText());
                    matches++;
                }
                break;
            default:
                break;
            }
        }

        assertEquals(1, matches);
    }

    @Test
    void testGetDelimitersX12_WithISX_00401() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        ByteArrayInputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *200711*0100*U*00401*000000001*0*T*:~"
                + "ISX*\\~"
                + "GS*FA*ReceiverDept*SenderDept*20200711*010015*1*X*005010~"
                + "ST*997*0001*005010X230~"
                + "SE*2*0001~"
                + "GE*1*1~"
                + "IEA*1*000000001~").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        Map<String, Character> expected = new HashMap<>(5);
        expected.put(Delimiters.SEGMENT, '~');
        expected.put(Delimiters.DATA_ELEMENT, '*');
        expected.put(Delimiters.COMPONENT_ELEMENT, ':');
        //expected.put(Delimiters.REPETITION, '*');
        //expected.put(Delimiters.RELEASE, '\\');
        expected.put(Delimiters.DECIMAL, '.');

        Map<String, Character> delimiters = null;
        int delimiterExceptions = 0;
        List<EDIStreamValidationError> errors = new ArrayList<>();

        while (reader.hasNext()) {
            try {
                reader.getDelimiters();
            } catch (IllegalStateException e) {
                delimiterExceptions++;
            }

            switch (reader.next()) {
            case START_GROUP:
                delimiters = reader.getDelimiters();
                break;
            case SEGMENT_ERROR:
            case ELEMENT_OCCURRENCE_ERROR:
            case ELEMENT_DATA_ERROR:
                errors.add(reader.getErrorType());
                break;
            default:
                break;
            }
        }

        assertEquals(expected, delimiters, "Unexpected delimiters");
        assertArrayEquals(new EDIStreamValidationError[] { EDIStreamValidationError.UNEXPECTED_SEGMENT },
                          errors.toArray(new EDIStreamValidationError[errors.size()]));
        assertEquals(1, delimiterExceptions, "Unexpected exceptions");
    }

    @Test
    void testGetDelimitersX12_WithISX_00501() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        ByteArrayInputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *200711*0100*^*00501*000000001*0*T*:~"
                + "ISX*\\~"
                + "GS*FA*ReceiverDept*SenderDept*20200711*010015*1*X*005010~"
                + "ST*997*0001*005010X230~"
                + "SE*2*0001~"
                + "GE*1*1~"
                + "IEA*1*000000001~").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        Map<String, Character> expected = new HashMap<>(5);
        expected.put(Delimiters.SEGMENT, '~');
        expected.put(Delimiters.DATA_ELEMENT, '*');
        expected.put(Delimiters.COMPONENT_ELEMENT, ':');
        expected.put(Delimiters.REPETITION, '^');
        //expected.put(Delimiters.RELEASE, '\\');
        expected.put(Delimiters.DECIMAL, '.');

        Map<String, Character> delimiters = null;
        int delimiterExceptions = 0;
        List<EDIStreamValidationError> errors = new ArrayList<>();

        while (reader.hasNext()) {
            try {
                reader.getDelimiters();
            } catch (IllegalStateException e) {
                delimiterExceptions++;
            }

            switch (reader.next()) {
            case START_GROUP:
                delimiters = reader.getDelimiters();
                break;
            case SEGMENT_ERROR:
            case ELEMENT_OCCURRENCE_ERROR:
            case ELEMENT_DATA_ERROR:
                errors.add(reader.getErrorType());
                break;
            default:
                break;
            }
        }

        assertEquals(expected, delimiters, "Unexpected delimiters");
        assertArrayEquals(new EDIStreamValidationError[] { EDIStreamValidationError.UNEXPECTED_SEGMENT },
                          errors.toArray(new EDIStreamValidationError[errors.size()]));
        assertEquals(1, delimiterExceptions, "Unexpected exceptions");
    }

    @Test
    void testGetDelimitersX12_WithISX_00501_Invalid_Repitition() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        ByteArrayInputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *200711*0100*V*00501*000000001*0*T*:~"
                + "ISX*\\~"
                + "GS*FA*ReceiverDept*SenderDept*20200711*010015*1*X*005010~"
                + "ST*997*0001*005010X230~"
                + "SE*2*0001~"
                + "GE*1*1~"
                + "IEA*1*000000001~").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        Map<String, Character> expected = new HashMap<>(5);
        expected.put(Delimiters.SEGMENT, '~');
        expected.put(Delimiters.DATA_ELEMENT, '*');
        expected.put(Delimiters.COMPONENT_ELEMENT, ':');
        expected.put(Delimiters.DECIMAL, '.');

        Map<String, Character> delimiters = null;
        int delimiterExceptions = 0;
        List<EDIStreamValidationError> errors = new ArrayList<>();

        while (reader.hasNext()) {
            try {
                reader.getDelimiters();
            } catch (IllegalStateException e) {
                delimiterExceptions++;
            }

            switch (reader.next()) {
            case START_GROUP:
                delimiters = reader.getDelimiters();
                break;
            case SEGMENT_ERROR:
            case ELEMENT_OCCURRENCE_ERROR:
            case ELEMENT_DATA_ERROR:
                errors.add(reader.getErrorType());
                break;
            default:
                break;
            }
        }

        assertEquals(expected, delimiters, "Unexpected delimiters");
        assertArrayEquals(new EDIStreamValidationError[] { EDIStreamValidationError.UNEXPECTED_SEGMENT },
                          errors.toArray(new EDIStreamValidationError[errors.size()]));
        assertEquals(1, delimiterExceptions, "Unexpected exceptions");
    }

    @Test
    void testGetDelimitersX12_WithISX_00704() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        ByteArrayInputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *200711*0100*^*00704*000000001*0*T*:~"
                + "ISX*\\~"
                + "GS*FA*ReceiverDept*SenderDept*20200711*010015*1*X*005010~"
                + "ST*997*0001*005010X230~"
                + "SE*2*0001~"
                + "GE*1*1~"
                + "IEA*1*000000001~").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        Map<String, Character> expected = new HashMap<>(5);
        expected.put(Delimiters.SEGMENT, '~');
        expected.put(Delimiters.DATA_ELEMENT, '*');
        expected.put(Delimiters.COMPONENT_ELEMENT, ':');
        expected.put(Delimiters.REPETITION, '^');
        expected.put(Delimiters.RELEASE, '\\');
        expected.put(Delimiters.DECIMAL, '.');

        Map<String, Character> delimiters = null;
        int delimiterExceptions = 0;
        List<EDIStreamValidationError> errors = new ArrayList<>();

        while (reader.hasNext()) {
            try {
                reader.getDelimiters();
            } catch (IllegalStateException e) {
                delimiterExceptions++;
            }

            switch (reader.next()) {
            case START_GROUP:
                delimiters = reader.getDelimiters();
                break;
            case SEGMENT_ERROR:
            case ELEMENT_OCCURRENCE_ERROR:
            case ELEMENT_DATA_ERROR:
                errors.add(reader.getErrorType());
                break;
            default:
                break;
            }
        }

        assertEquals(expected, delimiters, "Unexpected delimiters");
        assertEquals(0, errors.size(), "Unexpected errors");
        assertEquals(1, delimiterExceptions, "Unexpected exceptions");
    }

    @Test
    void testGetDelimitersX12_WithISX_CharTooLong_00704() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        ByteArrayInputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *200711*0100*^*00704*000000001*0*T*:~"
                + "ISX*##~"
                + "GS*FA*ReceiverDept*SenderDept*20200711*010015*1*X*005010~"
                + "ST*997*0001*005010X230~"
                + "SE*2*0001~"
                + "GE*1*1~"
                + "IEA*1*000000001~").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        Map<String, Character> expected = new HashMap<>(5);
        expected.put(Delimiters.SEGMENT, '~');
        expected.put(Delimiters.DATA_ELEMENT, '*');
        expected.put(Delimiters.COMPONENT_ELEMENT, ':');
        expected.put(Delimiters.REPETITION, '^');
        //expected.put(Delimiters.RELEASE, '\\');
        expected.put(Delimiters.DECIMAL, '.');

        Map<String, Character> delimiters = null;
        int delimiterExceptions = 0;
        List<EDIStreamValidationError> errors = new ArrayList<>();

        while (reader.hasNext()) {
            try {
                reader.getDelimiters();
            } catch (IllegalStateException e) {
                delimiterExceptions++;
            }

            switch (reader.next()) {
            case START_GROUP:
                delimiters = reader.getDelimiters();
                break;
            case SEGMENT_ERROR:
            case ELEMENT_OCCURRENCE_ERROR:
            case ELEMENT_DATA_ERROR:
                errors.add(reader.getErrorType());
                break;
            default:
                break;
            }
        }

        assertEquals(expected, delimiters, "Unexpected delimiters");
        assertArrayEquals(new EDIStreamValidationError[] { EDIStreamValidationError.DATA_ELEMENT_TOO_LONG },
                          errors.toArray(new EDIStreamValidationError[errors.size()]));
        assertEquals(1, delimiterExceptions, "Unexpected exceptions");
    }

    @Test
    void testX12_ReleaseCharacter() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        ByteArrayInputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *200711*0100*^*00704*000000001*0*T*:~"
                + "ISX*\\~"
                + "GS*FA*Receiver\\*Dept*Sender\\*Dept*20200711*010015*1*X*005010~"
                + "ST*997*0001*005010X230~"
                + "SE*2*0001~"
                + "GE*1*1~"
                + "IEA*1*000000001~").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        List<EDIStreamValidationError> errors = new ArrayList<>();
        String senderId = null;
        String receiverId = null;

        while (reader.hasNext()) {
            switch (reader.next()) {
            case ELEMENT_DATA:
                if ("GS".equals(reader.getLocation().getSegmentTag())) {
                    switch (reader.getLocation().getElementPosition()) {
                    case 2:
                        receiverId = reader.getText();
                        break;
                    case 3:
                        senderId = reader.getText();
                        break;
                    default:
                        break;
                    }
                }
                break;
            case SEGMENT_ERROR:
            case ELEMENT_OCCURRENCE_ERROR:
            case ELEMENT_DATA_ERROR:
                errors.add(reader.getErrorType());
                break;
            default:
                break;
            }
        }

        assertEquals(0, errors.size(), "Unexpected errors");
        assertEquals("Receiver*Dept", receiverId);
        assertEquals("Sender*Dept", senderId);
    }

    @Test
    void testNext() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        EDIStreamEvent event;

        do {
            try {
                event = reader.next();
                assertTrue(possible.contains(event), "Unknown event " + event);
            } catch (NoSuchElementException e) {
                event = null;
            }
        } while (event != null);
    }

    @Test
    void testNextTag() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        factory.setProperty(EDIInputFactory.EDI_VALIDATE_CONTROL_STRUCTURE, "false");
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        int s = 0;
        EDIStreamEvent event;
        String tag = null;

        while (reader.hasNext()) {
            try {
                event = reader.nextTag();
            } catch (NoSuchElementException e) {
                break;
            }

            if (event != EDIStreamEvent.START_SEGMENT) {
                fail("Unexpected event: " + event);
            }

            tag = reader.getText();
            assertEquals(simple997tags[s++], tag, "Unexpected segment");
        }

        String last = simple997tags[simple997tags.length - 1];
        assertEquals(last, tag, "Unexpected last segment");
    }

    @Test
    void testHasNext() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        assertTrue(reader.hasNext(), "Does not have next after create");

        EDIStreamEvent event;

        while (reader.hasNext()) {
            event = reader.next();
            assertTrue(possible.contains(event), "Unknown event");
        }
    }

    @Test
    void testClose() throws Exception {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        reader.close();
        try {
            assertNotEquals(0, stream.available(), "Stream was closed");
        } catch (IOException e) {
            fail("IO Exception: " + e);
        }
        assertThrows(IllegalStateException.class, () -> reader.getEventType());
    }

    @Test
    void testGetEventType() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        EDIStreamEvent event;

        while (reader.hasNext()) {
            event = reader.next();
            assertEquals(event, reader.getEventType(), "Event not equal");
        }
    }

    @Test
    void testGetStandard() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        String standard = null;
        int events = 0;
        assertThrows(IllegalStateException.class, () -> reader.getStandard());

        while (reader.hasNext()) {
            if (reader.next() != EDIStreamEvent.END_INTERCHANGE) {
                standard = reader.getStandard();
                events++;
            }
        }

        assertEquals("X12", standard, "Unexpected version");
        assertTrue(events > 2, "Unexpected number of events");
    }

    @Test
    void testGetVersion() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        String version[] = null;
        int events = 0;
        assertThrows(IllegalStateException.class, () -> reader.getVersion());

        while (reader.hasNext()) {
            if (reader.next() != EDIStreamEvent.END_INTERCHANGE) {
                version = reader.getVersion();
                events++;
            }
        }

        assertArrayEquals(new String[] { "00501" }, version, "Unexpected version");
        assertTrue(events > 2, "Unexpected number of events");
    }

    @Test
    void testSetSchema() throws EDIStreamException, EDISchemaException, IOException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/invalid997.edi");
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema transaction = schemaFactory.createSchema(getClass().getResource("/x12/EDISchema997.xml"));
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        EDIStreamEvent event;
        int events = 0;
        String segment = null;
        Map<String, Set<EDIStreamValidationError>> errors = new HashMap<>(2);

        while (reader.hasNext()) {
            event = reader.next();

            if (event == EDIStreamEvent.START_INTERCHANGE) {
                String standard = reader.getStandard();
                String[] version = reader.getVersion();
                Schema control = SchemaUtils.getControlSchema(standard, version);
                reader.setControlSchema(control);
            } else {
                if (event == EDIStreamEvent.START_TRANSACTION) {
                    reader.setTransactionSchema(transaction);
                } else if (event == EDIStreamEvent.START_SEGMENT) {
                    segment = reader.getText();
                } else if (event == EDIStreamEvent.ELEMENT_DATA_ERROR) {
                    Location l = reader.getLocation();
                    String key = String.format(
                                               "%s%02d",
                                               segment,
                                               l.getElementPosition());

                    if (!errors.containsKey(key)) {
                        errors.put(key, new HashSet<EDIStreamValidationError>(2));
                    }

                    errors.get(key).add(reader.getErrorType());
                } else if (event == EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR) {
                    fail("Unexpected error: " + event + " => " + reader.getErrorType() + ", " + reader.getText());
                } else if (event == EDIStreamEvent.SEGMENT_ERROR) {
                    fail("Unexpected error: " + event + " => " + reader.getErrorType() + ", " + reader.getText());
                }
            }
            events++;
        }

        assertTrue(errors.get("AK402").contains(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG));
        assertTrue(errors.get("AK402").contains(EDIStreamValidationError.INVALID_CHARACTER_DATA));

    }

    @Test
    void testAddSchema() throws EDIStreamException, EDISchemaException, IOException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        factory.setProperty(EDIInputFactory.EDI_VALIDATE_CONTROL_STRUCTURE, "false");
        InputStream stream = getClass().getResourceAsStream("/x12/invalid997.edi");

        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema transaction = schemaFactory.createSchema(getClass().getResource("/x12/EDISchema997.xml"));
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        EDIStreamEvent event;
        int events = 0;
        String segment = null;
        Map<String, Set<EDIStreamValidationError>> errors = new HashMap<>(2);

        while (reader.hasNext()) {
            event = reader.next();
            events++;

            switch (event) {
            case START_INTERCHANGE: {
                String standard = reader.getStandard();
                String version[] = reader.getVersion();
                Schema control = SchemaUtils.getControlSchema(standard, version);
                IllegalStateException illegal = null;

                try {
                    reader.setTransactionSchema(control);
                } catch (IllegalStateException e) {
                    illegal = e;
                }
                assertNotNull(illegal);

                reader.setControlSchema(control);
                continue;
            }
            case START_SEGMENT: {
                segment = reader.getText();
                IllegalStateException illegal = null;

                try {
                    reader.setTransactionSchema(transaction);
                } catch (IllegalStateException e) {
                    illegal = e;
                }
                if ("ST".equals(segment)) {
                    assertNull(illegal);
                } else {
                    assertNotNull(illegal);
                }

                break;
            }
            case END_SEGMENT:
                segment = reader.getText();
                IllegalStateException illegal = null;

                try {
                    reader.setTransactionSchema(transaction);
                } catch (IllegalStateException e) {
                    illegal = e;
                }
                if ("ST".equals(segment)) {
                    assertNull(illegal);
                } else {
                    assertNotNull(illegal);
                }

                break;
            case START_TRANSACTION:
                reader.setTransactionSchema(transaction);
                break;
            case ELEMENT_DATA_ERROR:
                Location l = reader.getLocation();
                String key = String.format("%s%02d", segment, l.getElementPosition());
                if (!errors.containsKey(key)) {
                    errors.put(key, new HashSet<>(2));
                }

                errors.get(key).add(reader.getErrorType());
                break;
            case ELEMENT_OCCURRENCE_ERROR:
            case SEGMENT_ERROR:
                fail("Unexpected error: " + event + " => " + reader.getErrorType() + ", " + reader.getText());
                break;
            default:
                break;
            }
        }

        assertTrue(errors.get("AK402").contains(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG));
        assertTrue(errors.get("AK402").contains(EDIStreamValidationError.INVALID_CHARACTER_DATA));
    }

    @Test
    void testGetErrorType()
            throws EDIStreamException,
            EDISchemaException,
            IOException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/invalid997.edi");
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema schema = schemaFactory.createSchema(getClass().getResource("/x12/EDISchema997.xml"));

        EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);

        EDIStreamEvent event;
        String segment = null;
        Map<String, Set<EDIStreamValidationError>> errors = new HashMap<>(2);

        while (reader.hasNext()) {
            event = reader.next();
            if (event == EDIStreamEvent.START_SEGMENT) {
                segment = reader.getText();
            } else if (event == EDIStreamEvent.ELEMENT_DATA_ERROR) {
                Location l = reader.getLocation();
                String key = String.format("%s%02d", segment, l.getElementPosition());

                if (!errors.containsKey(key)) {
                    errors.put(key, new HashSet<>(2));
                }

                errors.get(key).add(reader.getErrorType());
            }
        }

        assertTrue(errors.get("AK402").contains(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG));
        assertTrue(errors.get("AK402").contains(EDIStreamValidationError.INVALID_CHARACTER_DATA));
    }

    @Test
    void testGetText() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        assertThrows(IllegalStateException.class, () -> reader.getText());
        assertTrue(reader.hasNext());
        reader.next();
        assertThrows(IllegalStateException.class, () -> reader.getText());

        int s = 0;

        while (reader.hasNext()) {
            if (EDIStreamEvent.START_SEGMENT == reader.next()) {
                String tag = reader.getText();
                assertEquals(simple997tags[s++], tag, "Unexpected segment");
            }
        }
    }

    @Test
    void testGetTextCharacters() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        int s = 0;

        while (reader.hasNext()) {
            if (EDIStreamEvent.START_SEGMENT == reader.next()) {
                char[] tag = reader.getTextCharacters();
                int start = reader.getTextStart();
                int length = reader.getTextLength();
                char[] expected = simple997tags[s++].toCharArray();
                assertArrayEquals(expected, Arrays.copyOfRange(tag, start, start + length), "Unexpected segment");
            }
        }
    }

    @Test
    void testGetTextCharactersIntCharArrayIntInt() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        int s = 0;
        char[] tag = new char[20];

        while (reader.hasNext()) {
            if (EDIStreamEvent.START_SEGMENT == reader.next()) {
                char[] expected = simple997tags[s++].toCharArray();
                int start = reader.getTextStart();
                int length = reader.getTextLength();
                assertThrows(NullPointerException.class, () -> reader.getTextCharacters(start, null, 0, length));
                assertThrows(IndexOutOfBoundsException.class, () -> reader.getTextCharacters(start, tag, -1, length));
                assertThrows(IndexOutOfBoundsException.class, () -> reader.getTextCharacters(start, tag, 21, length));
                assertThrows(IndexOutOfBoundsException.class, () -> reader.getTextCharacters(start, tag, 0, -1));
                assertThrows(IndexOutOfBoundsException.class, () -> reader.getTextCharacters(start, tag, 0, 21));
                assertThrows(IndexOutOfBoundsException.class, () -> reader.getTextCharacters(-1, tag, 0, length));
                assertThrows(IndexOutOfBoundsException.class, () -> reader.getTextCharacters(4, tag, 0, length));

                int number = reader.getTextCharacters(start, tag, 0, length);
                assertEquals(expected.length, number, "Invalid length read");
                assertArrayEquals(expected, Arrays.copyOfRange(tag, 0, length), "Unexpected segment");
            }
        }
    }

    @Test
    void testGetTextStart() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        String tag = null;
        int e = 0;

        String value = null;
        int start = -1;
        int length = -1;

        while (reader.hasNext()) {
            switch (reader.next()) {
            case START_SEGMENT:
                tag = reader.getText();
                e = 0;
                break;
            case ELEMENT_DATA:
                if ("ISA".equals(tag) && ++e == 13) {
                    start = reader.getTextStart();
                    length = reader.getTextLength();
                    value = new String(reader.getTextCharacters(),
                                       start,
                                       length);
                }
                break;
            default:
                break;
            }
        }

        assertEquals("508121953", value);
    }

    @Test
    void testGetTextLength() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        String tag = null;
        int e = 0;

        String value = null;
        int start = -1;
        int length = -1;

        while (reader.hasNext()) {
            switch (reader.next()) {
            case START_SEGMENT:
                tag = reader.getText();
                e = 0;
                break;
            case ELEMENT_DATA:
                if ("AK4".equals(tag) && ++e == 3) {
                    start = reader.getTextStart();
                    length = reader.getTextLength();
                    value = new String(reader.getTextCharacters(),
                                       start,
                                       length);
                }
                break;
            default:
                break;
            }
        }

        assertEquals("7", value);
    }

    @Test
    void testGetLocation() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/extraDelimiter997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        int s = -1;
        String tag = null;
        int conditions = 0;

        assertLocation(reader, -1, -1, -1, -1);

        while (reader.hasNext()) {
            switch (reader.next()) {
            case START_SEGMENT:
                s += s == -1 ? 2 : 1;
                assertLocation(reader, s, -1, -1, -1);
                tag = reader.getText();
                String startSegmentLocation = reader.getLocation().toString();
                assertThat(startSegmentLocation, containsString("in segment " + tag));
                break;
            case END_SEGMENT:
                String endSegmentLocation = reader.getLocation().toString();
                assertThat(endSegmentLocation, containsString("after segment " + tag));
                break;
            case ELEMENT_DATA:
                Location l = reader.getLocation();
                if ("AK3".equals(tag)) {
                    if (l.getElementPosition() == 2) {
                        switch (l.getElementOccurrence()) {
                        case 1:
                            assertTextLocation(reader, "AK302-R1", 6, 2, 1, -1);
                            conditions++;
                            break;
                        case 2:
                            assertTextLocation(reader, "AK302-R2", 6, 2, 2, -1);
                            conditions++;
                            break;
                        case 3:
                            switch (l.getComponentPosition()) {
                            case 1:
                                assertTextLocation(reader, "AK302-R3-COMP1", 6, 2, 3, 1);
                                conditions++;
                                break;
                            case 2:
                                assertTextLocation(reader, "AK302-R3-COMP2", 6, 2, 3, 2);
                                conditions++;
                                break;
                            default:
                                fail();
                            }
                            break;
                        }
                    } else if (l.getElementPosition() == 4) {
                        switch (l.getElementOccurrence()) {
                        case 1:
                            assertTextLocation(reader, "AK304-R1", 6, 4, 1, -1);
                            conditions++;
                            break;
                        case 2:
                            assertTextLocation(reader, "AK304-R2", 6, 4, 2, -1);
                            conditions++;
                            break;
                        case 3:
                            assertTextLocation(reader, "AK304-R3", 6, 4, 3, -1);
                            conditions++;
                            break;
                        }
                    } else {
                        assertLocation(reader, 6, l.getElementPosition(), 1, -1);
                    }
                } else if ("AK4".equals(tag)) {
                    if (l.getElementPosition() == 4) {
                        switch (l.getElementOccurrence()) {
                        case 1:
                            switch (l.getComponentPosition()) {
                            case 1:
                                assertEquals("AK404-R1-COMP1", reader.getText());
                                conditions++;
                                break;
                            case 2:
                                assertEquals("AK404-R1-COMP2", reader.getText());
                                conditions++;
                                break;
                            case 3:
                                assertEquals("AK404-R1-COMP3", reader.getText());
                                conditions++;
                                break;
                            default:
                                fail();
                            }
                            break;
                        case 2:
                            switch (l.getComponentPosition()) {
                            case 1:
                                assertEquals("AK404-R2-COMP1", reader.getText());
                                conditions++;
                                break;
                            case 2:
                                assertEquals("AK404-R2-COMP2", reader.getText());
                                conditions++;
                                break;
                            default:
                                fail();
                            }
                            break;
                        }
                    } else {
                        assertEquals(-1,
                                     l.getComponentPosition(),
                                     String.format("AK4%02d should have had no components",
                                                   l.getElementPosition()));
                        assertEquals(1, l.getElementOccurrence());
                    }
                } else {
                    assertEquals(-1, l.getComponentPosition());
                    assertEquals(1, l.getElementOccurrence());
                }

                break;
            default:
                break;
            }
        }

        assertEquals(12, conditions);
    }

    @Test
    void testGetLocationSingleComponent() throws EDIStreamException, EDISchemaException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema schema = schemaFactory.createSchema(getClass().getResource("/x12/EDISchema997.xml"));

        EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);

        int s = -1;
        String tag = null;
        int conditions = 0;

        assertEquals(-1, reader.getLocation().getSegmentPosition());
        assertEquals(-1, reader.getLocation().getElementPosition());
        assertEquals(-1, reader.getLocation().getComponentPosition());
        assertEquals(-1, reader.getLocation().getElementOccurrence());

        while (reader.hasNext()) {
            switch (reader.next()) {
            case START_SEGMENT: {
                Location l = reader.getLocation();
                s += s == -1 ? 2 : 1;
                assertEquals(s, l.getSegmentPosition());
                assertEquals(-1, l.getElementPosition());
                assertEquals(-1, l.getComponentPosition());
                assertEquals(-1, l.getElementOccurrence());
                tag = reader.getText();
                break;
            }
            case ELEMENT_DATA: {
                Location l = reader.getLocation();

                if ("AK4".equals(tag) && l.getElementPosition() == 1) {
                    assertEquals("8", reader.getText());
                    assertEquals(1, l.getElementOccurrence());
                    assertEquals(1, l.getComponentPosition());
                    conditions++;
                }

                break;
            }
            case START_COMPOSITE: {
                conditions++;
                break;
            }
            case END_COMPOSITE: {
                conditions++;
                break;
            }
            default:
                break;
            }
        }

        assertEquals(3, conditions);
    }

    @Test
    void testGetBinaryDataInvalid() throws Exception {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/sample275_with_HL7_invalid_BIN01.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        EDIStreamEvent event;
        EDIStreamException thrown = null;

        try {
            while (reader.hasNext()) {
                try {
                    reader.nextTag();
                } catch (NoSuchElementException e) {
                    break;
                }

                if (reader.getEventType() == EDIStreamEvent.START_SEGMENT && "BIN".equals(reader.getText())) {
                    reader.next();
                    long binaryDataLength = Long.parseLong(reader.getText());
                    assertEquals(1839, binaryDataLength);
                    reader.setBinaryDataLength(binaryDataLength);
                    // Exception for invalid delimiter will occur next
                    event = reader.next();
                    assertEquals(EDIStreamEvent.ELEMENT_DATA_BINARY, event);
                    thrown = assertThrows(EDIStreamException.class, () -> reader.next());
                    break;
                }
            }
        } finally {
            reader.close();
        }
        assertNotNull(thrown);
    }

    @Test
    void testGetBinaryDataInvalidLength() throws Exception {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/nonnumeric_BIN01.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema schema = schemaFactory.createSchema(getClass().getResource("/x12/EDISchemaBinarySegment.xml"));

        EDIStreamEvent event;
        EDIStreamException bin01ParseException = null;
        EDIStreamException inconsistentParser = null;

        try {
            while (reader.hasNext()) {
                try {
                    event = reader.nextTag();
                } catch (NoSuchElementException e) {
                    break;
                }

                if (event == EDIStreamEvent.START_TRANSACTION) {
                    reader.setTransactionSchema(schema);
                    assertThrows(IllegalStateException.class, () -> reader.setBinaryDataLength(1L));
                } else if (event == EDIStreamEvent.START_SEGMENT && "BIN".equals(reader.getText())) {
                    // BIN01 is non-numeric
                    bin01ParseException = assertThrows(EDIStreamException.class, () -> reader.nextTag());
                    inconsistentParser = assertThrows(EDIStreamException.class, () -> reader.nextTag());
                    break;
                }
            }
        } finally {
            reader.close();
        }

        assertNotNull(bin01ParseException);
        assertNotNull(inconsistentParser);
    }

    @Test
    void testGetBinaryDataValid()
            throws EDIStreamException,
            IOException,
            ParserConfigurationException,
            SAXException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/sample275_with_HL7_valid_BIN01.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        EDIStreamEvent event;

        try {
            while (reader.hasNext()) {
                try {
                    event = reader.nextTag();
                } catch (NoSuchElementException e) {
                    break;
                }

                if (reader.getEventType() == EDIStreamEvent.START_SEGMENT && "BIN".equals(reader.getText())) {
                    reader.next();
                    long binaryDataLength = Long.parseLong(reader.getText());
                    assertEquals(2768, binaryDataLength);
                    reader.setBinaryDataLength(binaryDataLength);
                    event = reader.next();
                    assertEquals(EDIStreamEvent.ELEMENT_DATA_BINARY, event);
                    Document document;
                    try (InputStream binary = reader.getBinaryData()) {
                        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
                        domFactory.setValidating(false);
                        DocumentBuilder builder = domFactory.newDocumentBuilder();
                        document = builder.parse(binary);
                    }
                    assertEquals("levelone", document.getDocumentElement().getNodeName());
                    event = reader.next();
                    assertEquals(EDIStreamEvent.END_SEGMENT, event);
                }
            }
        } finally {
            reader.close();
        }
    }

    @Test
    void testEmptySegmentValidation() throws Exception {

        EDIInputFactory factory = EDIInputFactory.newFactory();
        factory.setProperty(EDIInputFactory.EDI_VALIDATE_CONTROL_STRUCTURE, true);
        factory.setProperty(EDIInputFactory.EDI_VALIDATE_CONTROL_CODE_VALUES, false);

        Schema transSchema = SchemaFactory.newFactory()
                                          .createSchema(getClass().getResourceAsStream("/EDIFACT/empty-segment-schema.xml"));

        EDIStreamReader reader = factory.createEDIStreamReader(getClass().getResourceAsStream("/EDIFACT/empty-segment-example.edi"));
        String segmentName = null;

        try {
            while (reader.hasNext()) {
                switch (reader.next()) {
                case START_TRANSACTION:
                    reader.setTransactionSchema(transSchema);
                    break;

                case START_SEGMENT:
                    segmentName = reader.getText();
                    break;

                case END_SEGMENT:
                    segmentName = null;
                    break;

                case ELEMENT_DATA:
                    break;

                case SEGMENT_ERROR: {
                    Location loc = reader.getLocation();
                    EDIStreamValidationError error = reader.getErrorType();
                    fail(String.format("%s: %s (seg=%s)",
                                       error.getCategory(),
                                       error,
                                       segmentName));
                    break;
                }
                case ELEMENT_DATA_ERROR:
                case ELEMENT_OCCURRENCE_ERROR: {
                    Location loc = reader.getLocation();
                    EDIStreamValidationError error = reader.getErrorType();

                    fail(String.format("%s: %s (seg=%s, elemPos=%d, compoPos=%d, textOnError=%s)",
                                       error.getCategory(),
                                       error,
                                       segmentName,
                                       loc.getElementPosition(),
                                       loc.getComponentPosition(),
                                       reader.getText()));
                    break;
                }
                default:
                    break;
                }
            }
        } finally {
            reader.close();
        }
    }

    @Test
    void testUnresolvedControlSchema() throws IOException, EDIStreamException {
        ByteArrayInputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*U*00000*508121953*0*P*:~"
                + "IEA*1*508121953~").getBytes());

        EDIInputFactory factory = EDIInputFactory.newFactory();
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        Exception thrown = null;

        try {
            while (reader.hasNext()) {
                assertNull(reader.getControlSchema());

                try {
                    reader.next();
                } catch (Exception e) {
                    thrown = e;
                    break;
                }
            }
        } finally {
            reader.close();
        }

        assertNull(thrown);
    }

    @Test
    void testControlSchemaParseError() throws IOException, EDIStreamException {
        ByteArrayInputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*U*00001*508121953*0*P*:~"
                + "IEA*1*508121953~").getBytes());

        EDIInputFactory factory = EDIInputFactory.newFactory();
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        Exception thrown = null;

        try {
            while (reader.hasNext()) {
                assertNull(reader.getControlSchema());

                try {
                    reader.next();
                } catch (Exception e) {
                    thrown = e;
                    break;
                }
            }
        } finally {
            reader.close();
        }

        assertNull(thrown);
    }

    @Test
    void testInvalidControlValidation() throws IOException, EDIStreamException {
        ByteArrayInputStream stream = new ByteArrayInputStream((""
                + "ISA*YY*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*`*00501*508121953*0*P*:~"
                + "IEA*1y*508121953~").getBytes());

        EDIInputFactory factory = EDIInputFactory.newFactory();
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        Exception thrown = null;
        List<EDIReference> invalidReferences = new ArrayList<>();

        try {
            while (reader.hasNext()) {
                try {
                    switch (reader.next()) {
                    case ELEMENT_DATA_ERROR:
                        invalidReferences.add(reader.getSchemaTypeReference());
                        break;
                    default:
                        break;
                    }
                } catch (Exception e) {
                    thrown = e;
                    break;
                }
            }
        } finally {
            reader.close();
        }

        assertNull(thrown);
        assertEquals(2, invalidReferences.size());
        assertEquals("I01", invalidReferences.get(0).getReferencedType().getId());
        assertEquals("I16", invalidReferences.get(1).getReferencedType().getId());
    }

    @Test
    void testInvalidControlValidationCodesDisabled() throws IOException, EDIStreamException {
        ByteArrayInputStream stream = new ByteArrayInputStream((""
                + "ISA*YY*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*`*00501*508121953*0*P*:~"
                + "IEA*1y*508121953~").getBytes());

        EDIInputFactory factory = EDIInputFactory.newFactory();
        factory.setProperty(EDIInputFactory.EDI_VALIDATE_CONTROL_CODE_VALUES, "false");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        Exception thrown = null;
        List<EDIReference> invalidReferences = new ArrayList<>();

        try {
            while (reader.hasNext()) {
                try {
                    switch (reader.next()) {
                    case ELEMENT_DATA_ERROR:
                        invalidReferences.add(reader.getSchemaTypeReference());
                        break;
                    default:
                        break;
                    }
                } catch (Exception e) {
                    thrown = e;
                    break;
                }
            }
        } finally {
            reader.close();
        }

        assertNull(thrown);
        assertEquals(1, invalidReferences.size());
        assertEquals("I16", invalidReferences.get(0).getReferencedType().getId());
    }

    @Test
    void testX12InterchangeServiceRequests() throws IOException, EDIStreamException {
        InputStream stream = getClass().getResourceAsStream("/x12/optionalInterchangeServices.edi");
        EDIInputFactory factory = EDIInputFactory.newFactory();
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        Exception thrown = null;

        try {
            while (reader.hasNext()) {
                try {
                    reader.next();
                } catch (Exception e) {
                    e.printStackTrace();
                    thrown = e;
                    break;
                }
            }
        } finally {
            reader.close();
        }

        assertNull(thrown);
    }

    @ParameterizedTest
    @CsvSource({
                 "/x12/empty997.edi, GS, IEA, ST, X.005010, X.005010X230, 997",
                 "/EDIFACT/orders-with-group.edi, UNG, UNZ, UNH, UN.D.96A.EAN008A, UN.D.96B.EAN008B, ORDERS",
                 "/TRADACOMS/order.edi, , END, MHD, , 9|9|9, ORDHDR|ORDERS|ORDTLR"
    })
    void testTransactionVersionAndTypeRetrieval(String resourceName,
                                                String groupStart,
                                                String interchangeEnd,
                                                String transactionStart,
                                                String expectedGroupVersion,
                                                String expectedTxVersion,
                                                String expectedTxType)
            throws EDIStreamException, IOException {

        InputStream stream = getClass().getResourceAsStream(resourceName);
        EDIInputFactory factory = EDIInputFactory.newFactory();
        EDIStreamReader rawReader = factory.createEDIStreamReader(stream);
        EDIStreamReader reader = factory.createFilteredReader(rawReader, r -> true); // Accept all events
        Exception thrown = null;

        Map<String, List<String[]>> segmentEndVersions = new HashMap<>(2);
        Map<String, List<String>> segmentEndVersionStrings = new HashMap<>(2);
        List<String> transactionTypeStrings = new ArrayList<>();
        Exception initThrown = null;
        Exception groupStartThrown = null;
        Exception interchangeEndThrown = null;

        try {
            initThrown = assertThrows(IllegalStateException.class, () -> reader.getTransactionVersionString());

            while (reader.hasNext()) {
                try {
                    switch (reader.next()) {
                    case START_SEGMENT:
                        if (Objects.equals(groupStart, reader.getText())) {
                            groupStartThrown = assertThrows(IllegalStateException.class, () -> reader.getTransactionVersionString());
                        }
                        if (interchangeEnd.equals(reader.getText())) {
                            interchangeEndThrown = assertThrows(IllegalStateException.class, () -> reader.getTransactionVersionString());
                        }

                        break;

                    case END_SEGMENT:
                        if (Objects.equals(groupStart, reader.getText()) || transactionStart.equals(reader.getText())) {
                            segmentEndVersions.computeIfAbsent(reader.getText(), k -> new ArrayList<>())
                                              .add(reader.getTransactionVersion());
                            segmentEndVersionStrings.computeIfAbsent(reader.getText(), k -> new ArrayList<>())
                                                    .add(reader.getTransactionVersionString());
                            if (transactionStart.equals(reader.getText())) {
                                transactionTypeStrings.add(reader.getTransactionType());
                            }
                        }
                        break;
                    default:
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    thrown = e;
                    break;
                }
            }
        } finally {
            reader.close();
        }

        Throwable finalThrown = thrown;
        assertNull(thrown, () -> "Unexpected exception: " + finalThrown);

        assertNotNull(initThrown);
        assertEquals("transaction version not accessible", initThrown.getMessage());
        if (groupStart != null) {
            assertNotNull(groupStartThrown);
            assertEquals("transaction version not accessible", groupStartThrown.getMessage());
        }
        assertNotNull(interchangeEndThrown);
        assertEquals("transaction version not accessible", interchangeEndThrown.getMessage());

        assertEquals(groupStart != null ? 2 : 1, segmentEndVersions.size());

        if (expectedGroupVersion != null) {
            assertArrayEquals(expectedGroupVersion.split("\\."), segmentEndVersions.get(groupStart).get(0));
        }

        int i = 0;

        for (String expected : expectedTxVersion.split("\\|")) {
            assertArrayEquals(expected.split("\\."), segmentEndVersions.get(transactionStart).get(i++));
        }

        assertEquals(groupStart != null ? 2 : 1, segmentEndVersionStrings.size());

        if (expectedGroupVersion != null) {
            assertEquals(expectedGroupVersion, segmentEndVersionStrings.get(groupStart).get(0));
        }

        i = 0;
        for (String expected : expectedTxVersion.split("\\|")) {
            assertEquals(expected, segmentEndVersionStrings.get(transactionStart).get(i++));
        }

        i = 0;
        for (String expected : expectedTxType.split("\\|")) {
            assertEquals(expected, transactionTypeStrings.get(i++));
        }
    }

    @Test
    void testTransactionVersionRetrievalTRADACOMS() throws EDIStreamException, IOException {
        InputStream stream = getClass().getResourceAsStream("/TRADACOMS/order.edi");
        EDIInputFactory factory = EDIInputFactory.newFactory();
        EDIStreamReader rawReader = factory.createEDIStreamReader(stream);
        EDIStreamReader reader = factory.createFilteredReader(rawReader, r -> true); // Accept all events
        Exception thrown = null;

        List<String[]> segmentEndVersions = new ArrayList<>(3);
        List<String> segmentEndVersionStrings = new ArrayList<>(3);
        Exception initThrown = null;
        Exception interchangeEndThrown = null;

        try {
            initThrown = assertThrows(IllegalStateException.class, () -> reader.getTransactionVersionString());

            while (reader.hasNext()) {
                try {
                    switch (reader.next()) {
                    case START_SEGMENT:
                        if ("END".equals(reader.getText())) {
                            interchangeEndThrown = assertThrows(IllegalStateException.class, () -> reader.getTransactionVersionString());
                        }

                        break;

                    case END_SEGMENT:
                        switch (reader.getText()) {
                        case "MHD":
                            segmentEndVersions.add(reader.getTransactionVersion());
                            segmentEndVersionStrings.add(reader.getTransactionVersionString());
                            break;
                        default:
                            break;
                        }

                        break;
                    default:
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    thrown = e;
                    break;
                }
            }
        } finally {
            reader.close();
        }

        assertNull(thrown);
        assertNotNull(initThrown);
        assertEquals("transaction version not accessible", initThrown.getMessage());
        assertNotNull(interchangeEndThrown);
        assertEquals("transaction version not accessible", interchangeEndThrown.getMessage());

        assertEquals(3, segmentEndVersions.size());
        assertArrayEquals(new String[] { "9" }, segmentEndVersions.get(0));
        assertArrayEquals(new String[] { "9" }, segmentEndVersions.get(1));
        assertArrayEquals(new String[] { "9" }, segmentEndVersions.get(2));

        assertEquals(3, segmentEndVersionStrings.size());
        assertEquals("9", segmentEndVersionStrings.get(0));
        assertEquals("9", segmentEndVersionStrings.get(1));
        assertEquals("9", segmentEndVersionStrings.get(2));
    }

    @Test
    void testX12FunctionalGroupDateVariableValidation() throws EDIStreamException, IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *200711*0100*U*00401*000000001*0*T*:~"
                + "GS*FA*ReceiverDept*SenderDept*20200711*010015*1*X*003040~"
                + "ST*997*0001*005010X230~"
                + "SE*2*0001~"
                + "GE*1*1~"
                + "IEA*1*000000001~").getBytes());

        EDIInputFactory factory = EDIInputFactory.newFactory();
        EDIStreamReader rawReader = factory.createEDIStreamReader(stream);
        EDIStreamReader reader = factory.createFilteredReader(rawReader, r -> true); // Accept all events
        Exception thrown = null;
        long gsDateMinInitial = -1;
        long gsDateMaxInitial = -1;
        long gsDateMinVersion003040 = -1;
        long gsDateMaxVersion003040 = -1;
        List<EDIReference> errorReferences = new ArrayList<>();
        List<EDIStreamValidationError> errors = new ArrayList<>();

        assertNull(reader.getReferenceCode()); // Null before start of input
        assertNull(reader.getSchemaTypeReference()); // Null before start of input

        try {
            while (reader.hasNext()) {
                try {
                    switch (reader.next()) {
                    case ELEMENT_DATA:
                        if ("GS".equals(reader.getLocation().getSegmentTag()) && reader.getLocation().getElementPosition() == 4) {
                            gsDateMinInitial = ((EDISimpleType) reader.getSchemaTypeReference().getReferencedType()).getMinLength();
                            gsDateMaxInitial = ((EDISimpleType) reader.getSchemaTypeReference().getReferencedType()).getMaxLength();
                        }
                        if ("GS".equals(reader.getLocation().getSegmentTag()) && reader.getLocation().getElementPosition() == 8) {
                            // Version has been determined
                            String version = reader.getTransactionVersionString();
                            EDIReference gsDateReference = ((EDIComplexType) reader.getControlSchema().getType("GS")).getReferences()
                                                                                                                     .get(3);
                            gsDateMinVersion003040 = ((EDISimpleType) gsDateReference.getReferencedType()).getMinLength(version);
                            gsDateMaxVersion003040 = ((EDISimpleType) gsDateReference.getReferencedType()).getMaxLength(version);
                        }

                        break;
                    case ELEMENT_DATA_ERROR:
                        errors.add(reader.getErrorType());
                        errorReferences.add(reader.getSchemaTypeReference());
                        break;
                    default:
                        break;
                    }
                } catch (Exception e) {
                    thrown = e;
                    break;
                }
            }
        } finally {
            reader.close();
        }

        assertNull(thrown);
        assertNull(reader.getReferenceCode()); // Null after end of input
        assertNull(reader.getSchemaTypeReference()); // Null after end of input

        assertEquals(6, gsDateMinInitial);
        assertEquals(8, gsDateMaxInitial);
        assertEquals(6, gsDateMinVersion003040);
        assertEquals(6, gsDateMaxVersion003040);

        assertEquals(2, errors.size());
        assertTrue(errors.contains(EDIStreamValidationError.INVALID_DATE));
        assertTrue(errors.contains(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG));

        assertEquals("373", errorReferences.get(0).getReferencedType().getCode());
        assertEquals("373", errorReferences.get(1).getReferencedType().getCode());
    }

    @Test
    void testEmptySegmentAtLoopStartValidation() throws Exception {

        EDIInputFactory factory = EDIInputFactory.newFactory();
        Schema transSchema = SchemaFactory.newFactory()
                                          .createSchema(getClass().getResourceAsStream("/EDIFACT/empty-segment-loop-schema.xml"));
        factory.setProperty(EDIInputFactory.EDI_VALIDATE_CONTROL_STRUCTURE, true);
        factory.setProperty(EDIInputFactory.EDI_VALIDATE_CONTROL_CODE_VALUES, false);

        EDIStreamReader reader = factory.createEDIStreamReader(getClass().getResourceAsStream("/EDIFACT/empty-segment-loop-example.edi"));
        String segmentName = null;

        try {
            while (reader.hasNext()) {
                switch (reader.next()) {
                case START_TRANSACTION:
                    reader.setTransactionSchema(transSchema);
                    break;

                case START_SEGMENT:
                    segmentName = reader.getText();
                    break;

                case END_SEGMENT:
                    segmentName = null;
                    break;

                case ELEMENT_DATA:
                    break;

                case SEGMENT_ERROR:
                case ELEMENT_DATA_ERROR:
                case ELEMENT_OCCURRENCE_ERROR:
                    String textOnError = reader.getText();
                    Location loc = reader.getLocation();
                    EDIStreamValidationError error = reader.getErrorType();

                    // Fails printing:
                    // java.lang.AssertionError: SEGMENT_ERROR: SEGMENT_EXCEEDS_MAXIMUM_USE (refCode=RCI, seg=null, elemPos=-1, compoPos=-1, textOnError=EQN)
                    Assertions.fail(String.format("%s: %s (refCode=%s, textOnError=%s, loc=%s)",
                                                  error.getCategory(),
                                                  error,
                                                  reader.getReferenceCode(),
                                                  textOnError,
                                                  loc));
                    break;
                default:
                    break;
                }
            }
        } finally {
            reader.close();
        }
    }

    /**
     * Original issue: https://github.com/xlate/staedi/issues/106
     *
     * @throws Exception
     */
    @Test
    void testValidatorResetElement_Issue106() throws Exception {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        Schema transSchema = SchemaFactory.newFactory()
                                          .createSchema(getClass().getResourceAsStream("/EDIFACT/issue106/null-pointer-impl-schema.xml"));
        factory.setProperty(EDIInputFactory.EDI_VALIDATE_CONTROL_STRUCTURE, true);
        factory.setProperty(EDIInputFactory.EDI_VALIDATE_CONTROL_CODE_VALUES, false);
        EDIStreamReader reader = factory.createEDIStreamReader(getClass().getResourceAsStream("/EDIFACT/issue106/null-pointer-example.edi"));
        List<Object> unexpected = new ArrayList<>();

        try {
            while (reader.hasNext()) {
                switch (reader.next()) {
                case START_TRANSACTION:
                    reader.setTransactionSchema(transSchema);
                    break;
                case SEGMENT_ERROR:
                case ELEMENT_OCCURRENCE_ERROR:
                case ELEMENT_DATA_ERROR:
                    unexpected.add(reader.getErrorType());
                    break;
                default:
                    break;
                }
            }
        } catch (Exception e) {
            unexpected.add(e);
        } finally {
            reader.close();
        }

        assertEquals(0, unexpected.size());
    }

    /**
     * Original issue: https://github.com/xlate/staedi/issues/122
     *
     * @throws Exception
     */
    @Test
    void testValidatorLookAhead_Issue122() throws Exception {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        Schema transSchema = SchemaFactory.newFactory().createSchema(getClass().getResourceAsStream("/EDIFACT/issue122/BAPLIE-d95b.xml"));
        EDIStreamReader reader = factory.createEDIStreamReader(getClass().getResourceAsStream("/EDIFACT/issue122/baplie-test.edi"));
        List<Object> unexpected = new ArrayList<>();

        try {
            while (reader.hasNext()) {
                switch (reader.next()) {
                case START_TRANSACTION:
                    reader.setTransactionSchema(transSchema);
                    break;
                case SEGMENT_ERROR:
                case ELEMENT_OCCURRENCE_ERROR:
                case ELEMENT_DATA_ERROR:
                    unexpected.add(reader.getErrorType());
                    break;
                default:
                    break;
                }
            }
        } catch (Exception e) {
            unexpected.add(e);
        } finally {
            reader.close();
        }

        assertEquals(0, unexpected.size());
    }

    @Test
    void testDecimalScaleAvailableFromSchema() throws EDISchemaException, IOException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        Schema transSchema = SchemaFactory.newFactory().createSchema(getClass().getResourceAsStream("/x12/EDISchema810.xml"));
        EDIStreamReader reader = factory.createEDIStreamReader(getClass().getResourceAsStream("/x12/invoice810_po850_dual.edi"));
        boolean startTx = false;
        BigDecimal tds01 = null;
        boolean inTransaction = false;

        try {
            while (reader.hasNext()) {
                switch (reader.next()) {
                case START_TRANSACTION:
                    inTransaction = true;
                    startTx = true;
                    break;
                case END_TRANSACTION:
                    inTransaction = false;
                    break;
                case ELEMENT_DATA:
                    Location l = reader.getLocation();

                    if (startTx && l.getElementPosition() == 1) {
                        if ("810".equals(reader.getText())) {
                            reader.setTransactionSchema(transSchema);
                        } else {
                            reader.setTransactionSchema(null);
                        }
                    } else if ("TDS".equals(l.getSegmentTag()) && l.getElementPosition() == 1) {
                        BigInteger unscaled = new BigInteger(reader.getText());
                        EDIType tds01Type = reader.getSchemaTypeReference().getReferencedType();
                        tds01 = new BigDecimal(unscaled, ((EDISimpleType) tds01Type).getScale());
                    } else if (inTransaction && !reader.getText().isEmpty()) {
                        // Only check for unexpected scaled numerics within the transaction
                        EDIReference ref = reader.getSchemaTypeReference();
                        if (ref != null && ref.getReferencedType().isType(Type.ELEMENT)) {
                            if (((EDISimpleType) ref.getReferencedType()).getScale() != null) {
                                // SE01 is an expected N0 element
                                if (!"SE".equals(l.getSegmentTag()) || l.getElementPosition() != 1) {
                                    fail("Unexpected non-null scale " + l);
                                }
                            }
                        }
                    }
                    break;
                case END_SEGMENT:
                    startTx = false;
                    break;
                case SEGMENT_ERROR:
                case ELEMENT_OCCURRENCE_ERROR:
                case ELEMENT_DATA_ERROR:
                    fail("Unexpected error condition: " + reader.getErrorType() + " " + reader.getLocation());
                    break;
                default:
                    break;
                }
            }
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage() + " " + reader.getLocation(), e);
        } finally {
            reader.close();
        }

        assertEquals(new BigDecimal("2554.38"), tds01);
    }

    /**
     * Original issue: https://github.com/xlate/staedi/issues/174
     *
     * @throws Exception
     */
    @Test
    void testOtherDialectTerminalSegmentsIgnored_Issue174() throws Exception {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        factory.setProperty(EDIInputFactory.EDI_VALIDATE_CONTROL_STRUCTURE, true);
        factory.setProperty(EDIInputFactory.EDI_VALIDATE_CONTROL_CODE_VALUES, false);
        EDIStreamReader reader = factory.createEDIStreamReader(getClass().getResourceAsStream("/EDIFACT/issue174/other_dialect_term_segments.edi"));
        List<Object> unexpected = new ArrayList<>();

        try {
            while (reader.hasNext()) {
                switch (reader.next()) {
                case SEGMENT_ERROR:
                case ELEMENT_OCCURRENCE_ERROR:
                case ELEMENT_DATA_ERROR:
                    unexpected.add(reader.getErrorType());
                    break;
                default:
                    break;
                }
            }
        } catch (Exception e) {
            unexpected.add(e);
        } finally {
            reader.close();
        }

        assertEquals(0, unexpected.size());
    }

    @Test
    void testTRADACOMS_IncorrectSegmentTagDelimiterIsInvalid() throws Exception {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        EDIStreamReader reader = factory.createEDIStreamReader(getClass().getResourceAsStream("/TRADACOMS/order-wrong-segment-tag-delimiter.edi"));
        List<Object> unexpected = new ArrayList<>();
        EDIStreamException thrown = assertThrows(EDIStreamException.class, () -> reader.next());
        assertTrue(thrown.getMessage().contains("Expected TRADACOMS segment tag delimiter"));
    }

    @Test
    void testHierarchicalLoopsNested() throws IOException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        EDIStreamReader reader = factory.createEDIStreamReader(getClass().getResourceAsStream("/x12/sample837-HL-nested.edi"));
        StringBuilder actual = new StringBuilder();
        List<Object> unexpected = new ArrayList<>();
        int depth = 0;

        try {
            while (reader.hasNext()) {
                switch (reader.next()) {
                case SEGMENT_ERROR:
                case ELEMENT_OCCURRENCE_ERROR:
                case ELEMENT_DATA_ERROR:
                    unexpected.add(reader.getErrorType());
                    break;
                case START_TRANSACTION:
                    SchemaFactory schemaFactory = SchemaFactory.newFactory();
                    URL schemaLocation = getClass().getResource("/x12/005010/837-hierarchical-level-enabled.xml");
                    Schema schema = schemaFactory.createSchema(schemaLocation);
                    reader.setTransactionSchema(schema);
                    // Fall through...
                case START_GROUP:
                case START_LOOP:
                    actual.append(new String(new char[depth]).replace("\0", "  "));
                    actual.append(reader.getText());
                    actual.append('\n');
                    depth++;
                    break;
                case END_LOOP:
                case END_TRANSACTION:
                case END_GROUP:
                    depth--;
                    break;
                case START_SEGMENT:
                    actual.append(new String(new char[depth]).replace("\0", "  "));
                    actual.append(reader.getText());
                    if ("HL".equals(reader.getLocation().getSegmentTag())) {
                        actual.append(":");
                    }
                    break;
                case END_SEGMENT:
                    actual.append('\n');
                    break;
                case ELEMENT_DATA:
                    Location loc = reader.getLocation();
                    if ("HL".equals(loc.getSegmentTag())) {
                        if (loc.getElementPosition() == 1) {
                            actual.append(" id: ");
                            actual.append(reader.getText());
                        } else if (loc.getElementPosition() == 2) {
                            if (reader.getText().isEmpty()) {
                                actual.append("; no parent");
                            } else {
                                actual.append("; parent: ");
                                actual.append(reader.getText());
                            }
                        }
                    }
                    break;
                default:
                    break;
                }
            }
        } catch (Exception e) {
            unexpected.add(e);
        } finally {
            reader.close();
        }

        System.out.println(actual.toString());

        // Expected 4 required elements missing from 2 empty HL segments
        assertEquals(4, unexpected.size(), () -> unexpected.toString());
        EDIStreamValidationError expectedErr = EDIStreamValidationError.REQUIRED_DATA_ELEMENT_MISSING;
        assertEquals(Arrays.asList(expectedErr, expectedErr, expectedErr, expectedErr), unexpected);

        String expected;

        try (InputStream stream = getClass().getResourceAsStream("/x12/sample837-HL-nested-expected.txt")) {
            expected = new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n"));
        }

        assertEquals(expected, actual.toString());
    }

    @Test
    void testMultipleInterchangesInStream() throws EDIStreamException, IOException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        EDIStreamReader reader = factory.createEDIStreamReader(getClass().getResourceAsStream("/x12/simple997-multiple-interchanges.edi"));
        int interchangeStart = 0;
        int interchangeEnd = 0;

        try {
            while (reader.hasNext()) {
                switch (reader.next()) {
                case START_INTERCHANGE:
                    interchangeStart++;
                    break;
                case END_INTERCHANGE:
                    interchangeEnd++;
                    break;
                case SEGMENT_ERROR:
                case ELEMENT_OCCURRENCE_ERROR:
                case ELEMENT_DATA_ERROR:
                    fail("Unexpected error: " + reader.getErrorType());
                default:
                    break;
                }
            }
        } finally {
            reader.close();
        }

        assertEquals(3, interchangeStart);
        assertEquals(3, interchangeEnd);
    }
}
