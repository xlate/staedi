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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import io.xlate.edi.internal.schema.SchemaUtils;
import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.Location;
import io.xlate.edi.stream.EDIStreamConstants.Delimiters;

@SuppressWarnings({ "unused" })
public class StaEDIStreamReaderTest implements ConstantsTest {

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
    public void testGetProperty() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getClassLoader().getResourceAsStream(
                                                                             "x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        Assert.assertNull("Property was not null", reader.getProperty("NONE"));
    }

    @Test
    public void testGetDelimitersX12() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getClassLoader().getResourceAsStream(
                                                                             "x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        Map<String, Character> expected = new HashMap<>(5);
        expected.put(Delimiters.SEGMENT, '~');
        expected.put(Delimiters.DATA_ELEMENT, '*');
        expected.put(Delimiters.COMPONENT_ELEMENT, ':');
        expected.put(Delimiters.REPETITION, '^');

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

        Assert.assertEquals("Unexpected delimiters", expected, delimiters);
        Assert.assertTrue("Unexpected exceptions", delimiterExceptions == 1);
    }

    @Test
    public void testGetDelimitersEDIFACTA() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getClassLoader().getResourceAsStream(
                                                                             "EDIFACT/invoic_d97b_una.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        Map<String, Character> expected = new HashMap<>(5);
        expected.put(Delimiters.SEGMENT, '~');
        expected.put(Delimiters.DATA_ELEMENT, '*');
        expected.put(Delimiters.COMPONENT_ELEMENT, '=');
        expected.put(Delimiters.REPETITION, '^');
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

        Assert.assertEquals("Unexpected delimiters", expected, delimiters);
        Assert.assertTrue("Unexpected exceptions", delimiterExceptions == 1);
    }

    @Test
    public void testGetDelimitersEDIFACTB() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getClassLoader().getResourceAsStream(
                                                                             "EDIFACT/invoic_d97b.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        Map<String, Character> expected = new HashMap<>(5);
        expected.put(Delimiters.SEGMENT, '\'');
        expected.put(Delimiters.DATA_ELEMENT, '+');
        expected.put(Delimiters.COMPONENT_ELEMENT, ':');
        expected.put(Delimiters.REPETITION, '*');
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

        Assert.assertEquals("Unexpected delimiters", expected, delimiters);
        Assert.assertTrue("Unexpected exceptions", delimiterExceptions == 1);
    }

    @Test
    public void testNext() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getClassLoader().getResourceAsStream("x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        EDIStreamEvent event;

        do {
            try {
                event = reader.next();
                Assert.assertTrue("Unknown event " + event, possible.contains(event));
            } catch (NoSuchElementException e) {
                event = null;
            }
        } while (event != null);
    }

    @Test
    public void testNextTag() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        factory.setProperty(EDIInputFactory.EDI_VALIDATE_CONTROL_STRUCTURE, "false");
        InputStream stream = getClass().getClassLoader().getResourceAsStream("x12/simple997.edi");
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
                Assert.fail("Unexpected event: " + event);
            }

            tag = reader.getText();
            Assert.assertEquals("Unexpected segment", simple997tags[s++], tag);
        }

        String last = simple997tags[simple997tags.length - 1];
        Assert.assertEquals("Unexpected last segment", last, tag);
    }

    @Test
    public void testHasNext() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getClassLoader().getResourceAsStream(
                                                                             "x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        Assert.assertTrue("Does not have next after create", reader.hasNext());

        EDIStreamEvent event;

        while (reader.hasNext()) {
            event = reader.next();
            Assert.assertTrue("Unknown event", possible.contains(event));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testClose() throws Exception {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getClassLoader().getResourceAsStream(
                                                                             "x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        reader.close();
        try {
            Assert.assertTrue("Stream was closed", stream.available() != 0);
        } catch (IOException e) {
            Assert.fail("IO Exception: " + e);
        }
        reader.getEventType();
    }

    @Test
    public void testGetEventType() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getClassLoader().getResourceAsStream("x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        EDIStreamEvent event;

        while (reader.hasNext()) {
            event = reader.next();
            Assert.assertEquals("Event not equal", event, reader.getEventType());
        }
    }

    @Test
    public void testGetStandard() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getClassLoader().getResourceAsStream("x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        String standard = null;
        int events = 0;
        int standardExceptions = 0;

        while (reader.hasNext()) {
            if (reader.next() == EDIStreamEvent.START_INTERCHANGE) {
                standard = reader.getStandard();
            } else {
                try {
                    reader.getVersion();
                } catch (IllegalStateException e) {
                    standardExceptions++;
                }
            }
            events++;
        }

        Assert.assertEquals("Unexpected version", "X12", standard);
        Assert.assertTrue(
                          "Unexpected number of exceptions",
                          events == standardExceptions + 1);
    }

    @Test
    public void testGetVersion() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getClassLoader().getResourceAsStream(
                                                                             "x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        String version[] = null;
        int events = 0;
        int versionExceptions = 0;

        while (reader.hasNext()) {
            if (reader.next() == EDIStreamEvent.START_INTERCHANGE) {
                version = reader.getVersion();
            } else {
                try {
                    reader.getVersion();
                } catch (IllegalStateException e) {
                    versionExceptions++;
                }
            }
            events++;
        }

        Assert.assertArrayEquals("Unexpected version", new String[] { "00501" }, version);
        Assert.assertTrue("Unexpected number of exceptions", events == versionExceptions + 1);
    }

    @Test
    public void testSetSchema() throws EDIStreamException, EDISchemaException, IOException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = SchemaUtils.getStreams("x12/invalid997.edi")
                                        .nextElement().openStream();

        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema transaction = schemaFactory.createSchema(SchemaUtils.getURL("x12/EDISchema997.xml"));
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        EDIStreamEvent event;
        int events = 0;
        int versionExceptions = 0;
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
                    Assert.fail("Unexpected error: " + event + " => " + reader.getErrorType() + ", " + reader.getText());
                } else if (event == EDIStreamEvent.SEGMENT_ERROR) {
                    Assert.fail("Unexpected error: " + event + " => " + reader.getErrorType() + ", " + reader.getText());
                }

                try {
                    reader.getVersion();
                } catch (IllegalStateException e) {
                    versionExceptions++;
                }
            }
            events++;
        }

        Assert.assertEquals("Unexpected number of exceptions", events, versionExceptions + 1);
        Assert.assertTrue(errors.get("AK402").contains(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG));
        Assert.assertTrue(errors.get("AK402").contains(EDIStreamValidationError.INVALID_CHARACTER_DATA));

    }

    @Test
    public void testAddSchema() throws EDIStreamException, EDISchemaException, IOException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        factory.setProperty(EDIInputFactory.EDI_VALIDATE_CONTROL_STRUCTURE, "false");
        InputStream stream = SchemaUtils.getStreams("x12/invalid997.edi")
                                        .nextElement().openStream();

        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema transaction = schemaFactory.createSchema(SchemaUtils.getURL("x12/EDISchema997.xml"));
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        EDIStreamEvent event;
        int events = 0;
        int versionExceptions = 0;
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
                Assert.assertNotNull(illegal);

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
                    Assert.assertNull(illegal);
                } else {
                    Assert.assertNotNull(illegal);
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
                Assert.assertNotNull(illegal);

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
                Assert.fail("Unexpected error: " + event + " => " + reader.getErrorType() + ", " + reader.getText());
                break;
            default:
                break;
            }

            try {
                reader.getVersion();
            } catch (IllegalStateException e) {
                versionExceptions++;
            }
        }

        Assert.assertTrue("Unexpected number of exceptions", events == versionExceptions + 1);
        Assert.assertTrue(errors.get("AK402").contains(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG));
        Assert.assertTrue(errors.get("AK402").contains(EDIStreamValidationError.INVALID_CHARACTER_DATA));
    }

    @Test
    public void testGetErrorType()
                                   throws EDIStreamException,
                                   EDISchemaException,
                                   IOException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = SchemaUtils.getStreams("x12/invalid997.edi")
                                        .nextElement().openStream();

        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema schema = schemaFactory.createSchema(SchemaUtils.getURL("x12/EDISchema997.xml"));

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

        Assert.assertTrue(errors.get("AK402").contains(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG));
        Assert.assertTrue(errors.get("AK402").contains(EDIStreamValidationError.INVALID_CHARACTER_DATA));
    }

    @Test
    public void testGetText() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getClassLoader().getResourceAsStream("x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        int s = 0;

        while (reader.hasNext()) {
            if (EDIStreamEvent.START_SEGMENT == reader.next()) {
                String tag = reader.getText();
                Assert.assertEquals(
                                    "Unexpected segment",
                                    simple997tags[s++],
                                    tag);
            }
        }
    }

    @Test
    public void testGetTextCharacters() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getClassLoader().getResourceAsStream("x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        int s = 0;

        while (reader.hasNext()) {
            if (EDIStreamEvent.START_SEGMENT == reader.next()) {
                char[] tag = reader.getTextCharacters();
                int start = reader.getTextStart();
                int length = reader.getTextLength();
                char[] expected = simple997tags[s++].toCharArray();
                Assert.assertArrayEquals(
                                         "Unexpected segment",
                                         expected,
                                         Arrays.copyOfRange(tag, start, start + length));
            }
        }
    }

    @Test
    public void testGetTextCharactersIntCharArrayIntInt() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getClassLoader().getResourceAsStream("x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        int s = 0;
        char[] tag = new char[20];

        while (reader.hasNext()) {
            if (EDIStreamEvent.START_SEGMENT == reader.next()) {
                char[] expected = simple997tags[s++].toCharArray();
                int start = reader.getTextStart();
                int length = reader.getTextLength();
                int number = reader.getTextCharacters(start, tag, 0, length);
                Assert.assertEquals("Invalid length read",
                                    expected.length,
                                    number);
                Assert.assertArrayEquals("Unexpected segment",
                                         expected,
                                         Arrays.copyOfRange(tag, 0, length));
            }
        }
    }

    @Test
    public void testGetTextStart() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getClassLoader().getResourceAsStream("x12/simple997.edi");
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

        Assert.assertEquals("508121953", value);
    }

    @Test
    public void testGetTextLength() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getClassLoader().getResourceAsStream("x12/simple997.edi");
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

        Assert.assertEquals("7", value);
    }

    @Test
    public void testGetLocation() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getClassLoader().getResourceAsStream(
                                                                             "x12/extraDelimiter997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        int s = -1;
        String tag = null;
        int conditions = 0;

        Assert.assertEquals(-1, reader.getLocation().getSegmentPosition());
        Assert.assertEquals(-1, reader.getLocation().getElementPosition());
        Assert.assertEquals(-1, reader.getLocation().getComponentPosition());
        Assert.assertEquals(-1, reader.getLocation().getElementOccurrence());

        while (reader.hasNext()) {
            switch (reader.next()) {
            case START_SEGMENT:
                s += s == -1 ? 2 : 1;
                Assert.assertEquals(s, reader.getLocation().getSegmentPosition());
                Assert.assertEquals(-1, reader.getLocation().getElementPosition());
                Assert.assertEquals(-1, reader.getLocation().getComponentPosition());
                Assert.assertEquals(-1, reader.getLocation().getElementOccurrence());
                tag = reader.getText();
                break;
            case ELEMENT_DATA:
                Location l = reader.getLocation();
                if ("AK3".equals(tag)) {
                    if (l.getElementPosition() == 2) {
                        switch (l.getElementOccurrence()) {
                        case 1:
                            Assert.assertEquals("AK302-R1", reader.getText());
                            Assert.assertEquals(-1, l.getComponentPosition());
                            conditions++;
                            break;
                        case 2:
                            Assert.assertEquals("AK302-R2", reader.getText());
                            Assert.assertEquals(-1, l.getComponentPosition());
                            conditions++;
                            break;
                        case 3:
                            switch (l.getComponentPosition()) {
                            case 1:
                                Assert.assertEquals("AK302-R3-COMP1", reader.getText());
                                conditions++;
                                break;
                            case 2:
                                Assert.assertEquals("AK302-R3-COMP2", reader.getText());
                                conditions++;
                                break;
                            default:
                                Assert.fail();
                            }
                            break;
                        }
                    } else if (l.getElementPosition() == 4) {
                        switch (l.getElementOccurrence()) {
                        case 1:
                            Assert.assertEquals("AK304-R1", reader.getText());
                            Assert.assertEquals(-1, l.getComponentPosition());
                            conditions++;
                            break;
                        case 2:
                            Assert.assertEquals("AK304-R2", reader.getText());
                            Assert.assertEquals(-1, l.getComponentPosition());
                            conditions++;
                            break;
                        case 3:
                            Assert.assertEquals("AK304-R3", reader.getText());
                            Assert.assertEquals(-1, l.getComponentPosition());
                            conditions++;
                            break;
                        }
                    } else {
                        Assert.assertEquals(-1, l.getComponentPosition());
                        Assert.assertEquals(1, l.getElementOccurrence());
                    }
                } else if ("AK4".equals(tag)) {
                    if (l.getElementPosition() == 4) {
                        switch (l.getElementOccurrence()) {
                        case 1:
                            switch (l.getComponentPosition()) {
                            case 1:
                                Assert.assertEquals("AK404-R1-COMP1", reader.getText());
                                conditions++;
                                break;
                            case 2:
                                Assert.assertEquals("AK404-R1-COMP2", reader.getText());
                                conditions++;
                                break;
                            case 3:
                                Assert.assertEquals("AK404-R1-COMP3", reader.getText());
                                conditions++;
                                break;
                            default:
                                Assert.fail();
                            }
                            break;
                        case 2:
                            switch (l.getComponentPosition()) {
                            case 1:
                                Assert.assertEquals("AK404-R2-COMP1", reader.getText());
                                conditions++;
                                break;
                            case 2:
                                Assert.assertEquals("AK404-R2-COMP2", reader.getText());
                                conditions++;
                                break;
                            default:
                                Assert.fail();
                            }
                            break;
                        }
                    } else {
                        Assert.assertEquals(
                                            String.format(
                                                          "AK4%02d should have had no components",
                                                          l.getElementPosition()),
                                            -1,
                                            l.getComponentPosition());
                        Assert.assertEquals(1, l.getElementOccurrence());
                    }
                } else {
                    Assert.assertEquals(-1, l.getComponentPosition());
                    Assert.assertEquals(1, l.getElementOccurrence());
                }

                break;
            default:
                break;
            }
        }

        Assert.assertEquals(12, conditions);
    }

    @Test
    public void testGetLocationSingleComponent() throws EDIStreamException, EDISchemaException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getClassLoader().getResourceAsStream(
                                                                             "x12/simple997.edi");
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema schema = schemaFactory.createSchema(SchemaUtils.getURL("x12/EDISchema997.xml"));

        EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);

        int s = -1;
        String tag = null;
        int conditions = 0;

        Assert.assertEquals(-1, reader.getLocation().getSegmentPosition());
        Assert.assertEquals(-1, reader.getLocation().getElementPosition());
        Assert.assertEquals(-1, reader.getLocation().getComponentPosition());
        Assert.assertEquals(-1, reader.getLocation().getElementOccurrence());

        while (reader.hasNext()) {
            switch (reader.next()) {
            case START_SEGMENT: {
                Location l = reader.getLocation();
                s += s == -1 ? 2 : 1;
                Assert.assertEquals(s, l.getSegmentPosition());
                Assert.assertEquals(-1, l.getElementPosition());
                Assert.assertEquals(-1, l.getComponentPosition());
                Assert.assertEquals(-1, l.getElementOccurrence());
                tag = reader.getText();
                break;
            }
            case ELEMENT_DATA: {
                Location l = reader.getLocation();

                if ("AK4".equals(tag) && l.getElementPosition() == 1) {
                    Assert.assertEquals("8", reader.getText());
                    Assert.assertEquals(1, l.getElementOccurrence());
                    Assert.assertEquals(1, l.getComponentPosition());
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

        Assert.assertEquals(3, conditions);
    }

    @Test(expected = EDIStreamException.class)
    public void testGetBinaryDataInvalid() throws Exception {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getClassLoader().getResourceAsStream(
                                                                             "x12/sample275_with_HL7_invalid_BIN01.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        EDIStreamEvent event;
        String tag = null;

        try {
            while (reader.hasNext()) {
                try {
                    reader.nextTag();
                } catch (NoSuchElementException e) {
                    break;
                }

                tag = reader.getText();

                if ("BIN".equals(tag)) {
                    reader.next();
                    long binaryDataLength = Long.parseLong(reader.getText());
                    Assert.assertEquals(1839, binaryDataLength);
                    reader.setBinaryDataLength(binaryDataLength);
                    // Exception for invalid delimiter will occur next
                    event = reader.next();
                    Assert.assertEquals(EDIStreamEvent.ELEMENT_DATA_BINARY, event);
                }
            }
        } finally {
            reader.close();
        }
    }

    @Test
    public void testGetBinaryDataValid()
                                         throws EDIStreamException,
                                         IOException,
                                         ParserConfigurationException,
                                         SAXException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getClassLoader().getResourceAsStream("x12/sample275_with_HL7_valid_BIN01.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);

        EDIStreamEvent event;
        String tag = null;

        try {
            while (reader.hasNext()) {
                try {
                    event = reader.nextTag();
                } catch (NoSuchElementException e) {
                    break;
                }

                tag = reader.getText();

                if ("BIN".equals(tag)) {
                    reader.next();
                    long binaryDataLength = Long.parseLong(reader.getText());
                    Assert.assertEquals(2768, binaryDataLength);
                    reader.setBinaryDataLength(binaryDataLength);
                    event = reader.next();
                    Assert.assertEquals(EDIStreamEvent.ELEMENT_DATA_BINARY, event);
                    Document document;
                    try (InputStream binary = reader.getBinaryData()) {
                        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
                        domFactory.setValidating(false);
                        DocumentBuilder builder = domFactory.newDocumentBuilder();
                        document = builder.parse(binary);
                    }
                    Assert.assertEquals("levelone", document.getDocumentElement().getNodeName());
                    event = reader.next();
                    Assert.assertEquals(EDIStreamEvent.END_SEGMENT, event);
                }
            }
        } finally {
            reader.close();
        }
    }
}
