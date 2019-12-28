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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.DefaultComparisonFormatter;
import org.xmlunit.diff.Diff;

import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamFilter;
import io.xlate.edi.stream.EDIStreamReader;

@SuppressWarnings("resource")
public class StaEDIXMLStreamReaderTest {

    static byte[] DUMMY_X12 = ("ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
            + "S01*X~"
            + "S11*X~"
            + "S12*X~"
            + "S19*X~"
            + "S09*X~"
            + "IEA*1*508121953~").getBytes();

    static byte[] TINY_X12 = ("ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
            + "IEA*1*508121953~").getBytes();

    XMLStreamReader getXmlReader(String resource) throws EDIStreamException, XMLStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream(resource);
        EDIStreamReader ediReader = factory.createEDIStreamReader(stream);
        return new StaEDIXMLStreamReader(ediReader);
    }

    XMLStreamReader getXmlReader(byte[] bytes) throws EDIStreamException, XMLStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        factory.setProperty(EDIInputFactory.EDI_VALIDATE_CONTROL_STRUCTURE, "false");
        InputStream stream = new ByteArrayInputStream(bytes);
        EDIStreamReader ediReader = factory.createEDIStreamReader(stream);
        return new StaEDIXMLStreamReader(ediReader);
    }

    static void skipEvents(XMLStreamReader reader, int eventCount) throws XMLStreamException {
        for (int i = 0; i < eventCount; i++) {
            reader.next();
        }
    }

    @Test
    public void testCreateEDIXMLStreamReader() throws EDIStreamException, XMLStreamException {
        XMLStreamReader xmlReader = getXmlReader("/x12/simple997.edi");
        assertNotNull(xmlReader, "xmlReader was null");
    }

    @Test
    public void testHasNext() throws Exception {
        XMLStreamReader xmlReader = getXmlReader(DUMMY_X12);
        assertTrue(xmlReader.hasNext());
    }

    private static void assertSegmentBoundaries(XMLStreamReader xmlReader, String tag, int elementCount)
                                                                                                         throws XMLStreamException {
        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next());
        assertEquals(tag, xmlReader.getLocalName());
        xmlReader.require(XMLStreamConstants.START_ELEMENT, null, tag);
        skipEvents(xmlReader, 3 * elementCount);
        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next());
        assertEquals(tag, xmlReader.getLocalName());
        xmlReader.require(XMLStreamConstants.END_ELEMENT, null, tag);
    }

    @Test
    public void testSegmentSequence() throws Exception {
        XMLStreamReader xmlReader = getXmlReader(DUMMY_X12);

        assertEquals(XMLStreamConstants.START_DOCUMENT, xmlReader.next());
        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next());
        assertEquals("INTERCHANGE", xmlReader.getLocalName());

        assertSegmentBoundaries(xmlReader, "ISA", 16);
        assertSegmentBoundaries(xmlReader, "S01", 1);
        assertSegmentBoundaries(xmlReader, "S11", 1);
        assertSegmentBoundaries(xmlReader, "S12", 1);
        assertSegmentBoundaries(xmlReader, "S19", 1);
        assertSegmentBoundaries(xmlReader, "S09", 1);
        assertSegmentBoundaries(xmlReader, "IEA", 2);

        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next());
        assertEquals("INTERCHANGE", xmlReader.getLocalName());
        assertEquals(XMLStreamConstants.END_DOCUMENT, xmlReader.next());
    }

    @Test
    public void testGetElementText() throws Exception {
        XMLStreamReader xmlReader = getXmlReader(DUMMY_X12);

        assertEquals(XMLStreamConstants.START_DOCUMENT, xmlReader.next());
        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next());
        assertEquals("INTERCHANGE", xmlReader.getLocalName());

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next());
        assertEquals("ISA", xmlReader.getLocalName());
        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next()); // ISA01;
        assertEquals("00", xmlReader.getElementText());
        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next()); // ISA02;
        assertEquals("          ", xmlReader.getElementText());
        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next()); // ISA03;
        assertEquals("00", xmlReader.getElementText());
    }

    private void assertElement(XMLStreamReader xmlReader, String tag, String value) throws Exception {
        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next());
        assertEquals(tag, xmlReader.getLocalName());
        assertEquals(value, xmlReader.getElementText());
        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.getEventType());
        assertEquals(tag, xmlReader.getLocalName());
    }

    @Test
    public void testElementEvents() throws Exception {
        XMLStreamReader xmlReader = getXmlReader(TINY_X12);
        assertEquals(XMLStreamConstants.START_DOCUMENT, xmlReader.next());

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next());
        assertEquals("INTERCHANGE", xmlReader.getLocalName());

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next());
        assertEquals("ISA", xmlReader.getLocalName());
        assertElement(xmlReader, "ISA01", "00");
        assertElement(xmlReader, "ISA02", "          ");
        assertElement(xmlReader, "ISA03", "00");
        assertElement(xmlReader, "ISA04", "          ");
        assertElement(xmlReader, "ISA05", "ZZ");
        assertElement(xmlReader, "ISA06", "ReceiverID     ");
        assertElement(xmlReader, "ISA07", "ZZ");
        assertElement(xmlReader, "ISA08", "Sender         ");
        assertElement(xmlReader, "ISA09", "050812");
        assertElement(xmlReader, "ISA10", "1953");
        assertElement(xmlReader, "ISA11", "^");
        assertElement(xmlReader, "ISA12", "00501");
        assertElement(xmlReader, "ISA13", "508121953");
        assertElement(xmlReader, "ISA14", "0");
        assertElement(xmlReader, "ISA15", "P");
        assertElement(xmlReader, "ISA16", ":");
        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next());
        assertEquals("ISA", xmlReader.getLocalName());

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next());
        assertEquals("IEA", xmlReader.getLocalName());
        assertElement(xmlReader, "IEA01", "1");
        assertElement(xmlReader, "IEA02", "508121953");
        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next());
        assertEquals("IEA", xmlReader.getLocalName());

        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next());
        assertEquals("INTERCHANGE", xmlReader.getLocalName());
        assertEquals(XMLStreamConstants.END_DOCUMENT, xmlReader.next());

        assertFalse(xmlReader.hasNext());
        xmlReader.close();
    }

    @Test
    public void testWriteXml() throws Exception {
        XMLStreamReader xmlReader = getXmlReader("/x12/extraDelimiter997.edi");
        xmlReader.next(); // Per StAXSource JavaDoc, put in START_DOCUMENT state
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StringWriter result = new StringWriter();
        transformer.transform(new StAXSource(xmlReader), new StreamResult(result));
        String resultString = result.toString();
        Diff d = DiffBuilder.compare(Input.fromFile("src/test/resources/x12/extraDelimiter997.xml"))
                   .withTest(resultString).build();
        assertTrue(!d.hasDifferences(), () -> "XML unexpectedly different:\n" + d.toString(new DefaultComparisonFormatter()));
    }

    @Test
    public void testSchemaValidatedInput() throws Exception {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema schema = schemaFactory.createSchema(getClass().getResource("/x12/EDISchema997.xml"));
        EDIStreamReader ediReader = factory.createEDIStreamReader(stream);
        EDIStreamFilter ediFilter = new EDIStreamFilter() {
            @Override
            public boolean accept(EDIStreamReader reader) {
                switch (reader.getEventType()) {
                case START_INTERCHANGE:
                case START_GROUP:
                case START_TRANSACTION:
                case START_LOOP:
                case START_SEGMENT:
                case END_SEGMENT:
                case END_LOOP:
                case END_TRANSACTION:
                case END_GROUP:
                case END_INTERCHANGE:
                    return true;
                default:
                    return false;
                }
            }
        };
        ediReader = factory.createFilteredReader(ediReader, ediFilter);
        XMLStreamReader xmlReader = new StaEDIXMLStreamReader(ediReader);

        assertEquals(XMLStreamConstants.START_DOCUMENT, xmlReader.next());
        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next());
        assertEquals("INTERCHANGE", xmlReader.getLocalName());

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next());
        assertEquals("ISA", xmlReader.getLocalName());
        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next());
        assertEquals("ISA", xmlReader.getLocalName());

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.nextTag());
        assertEquals("GROUP", xmlReader.getLocalName());

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.nextTag());
        assertEquals("GS", xmlReader.getLocalName());
        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next());
        assertEquals("GS", xmlReader.getLocalName());

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.nextTag());
        assertEquals("TRANSACTION", xmlReader.getLocalName());

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.nextTag());
        assertEquals("ST", xmlReader.getLocalName());

        ediReader.setTransactionSchema(schema);

        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next());
        assertEquals("ST", xmlReader.getLocalName());

    }

    @Test
    @SuppressWarnings("unused")
    public void testUnsupportedOperations() throws Exception {
        EDIStreamReader ediReader = Mockito.mock(EDIStreamReader.class);
        XMLStreamReader xmlReader = new StaEDIXMLStreamReader(ediReader);
        try {
            xmlReader.getNamespaceURI();
            fail("UnsupportedOperationExpected");
        } catch (UnsupportedOperationException e) {}
        try {
            xmlReader.getAttributeValue("", "");
            fail("UnsupportedOperationExpected");
        } catch (UnsupportedOperationException e) {}
        try {
            xmlReader.getAttributeName(0);
            fail("UnsupportedOperationExpected");
        } catch (UnsupportedOperationException e) {}
        try {
            xmlReader.getAttributeNamespace(0);
            fail("UnsupportedOperationExpected");
        } catch (UnsupportedOperationException e) {}
        try {
            xmlReader.getAttributeLocalName(0);
            fail("UnsupportedOperationExpected");
        } catch (UnsupportedOperationException e) {}
        try {
            xmlReader.getAttributePrefix(0);
            fail("UnsupportedOperationExpected");
        } catch (UnsupportedOperationException e) {}
        try {
            xmlReader.getAttributeType(0);
            fail("UnsupportedOperationExpected");
        } catch (UnsupportedOperationException e) {}
        try {
            xmlReader.getAttributeValue(0);
            fail("UnsupportedOperationExpected");
        } catch (UnsupportedOperationException e) {}
        try {
            xmlReader.isAttributeSpecified(0);
            fail("UnsupportedOperationExpected");
        } catch (UnsupportedOperationException e) {}
        try {
            xmlReader.getNamespacePrefix(0);
            fail("UnsupportedOperationExpected");
        } catch (UnsupportedOperationException e) {}
        try {
            xmlReader.getNamespaceURI(0);
            fail("UnsupportedOperationExpected");
        } catch (UnsupportedOperationException e) {}
        try {
            xmlReader.getNamespaceContext();
            fail("UnsupportedOperationExpected");
        } catch (UnsupportedOperationException e) {}
        try {
            xmlReader.getNamespaceURI("");
            fail("UnsupportedOperationExpected");
        } catch (UnsupportedOperationException e) {}
        try {
            xmlReader.getPITarget();
            fail("UnsupportedOperationExpected");
        } catch (UnsupportedOperationException e) {}
        try {
            xmlReader.getPIData();
            fail("UnsupportedOperationExpected");
        } catch (UnsupportedOperationException e) {}
    }

    @Test
    public void testGetTextString() throws Exception {
        XMLStreamReader xmlReader = getXmlReader(DUMMY_X12);

        assertEquals(XMLStreamConstants.START_DOCUMENT, xmlReader.next());
        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next());
        assertEquals("INTERCHANGE", xmlReader.getLocalName());

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next());
        assertEquals("ISA", xmlReader.getLocalName());
        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next()); // ISA01;
        assertEquals(XMLStreamConstants.CHARACTERS, xmlReader.next()); // ISA01 content;

        String textString = xmlReader.getText();
        assertEquals("00", textString);
        char[] textArray = xmlReader.getTextCharacters();
        assertArrayEquals(new char[] {'0', '0'}, textArray);
        char[] textArrayLocal = new char[3];
        xmlReader.getTextCharacters(xmlReader.getTextStart(), textArrayLocal, 0, xmlReader.getTextLength());
        assertArrayEquals(new char[] {'0', '0', '\0'}, textArrayLocal);
    }

    @Test
    public void testGetCdataBinary() throws Exception {
    	EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple_with_binary_segment.edi");
        EDIStreamReader ediReader = factory.createEDIStreamReader(stream);
        AtomicReference<String> segmentName = new AtomicReference<>();
        EDIStreamFilter ediFilter = new EDIStreamFilter() {
            @Override
            public boolean accept(EDIStreamReader reader) {
                switch (reader.getEventType()) {
                case START_SEGMENT:
                	segmentName.set(reader.getText());
                	return true;
                case START_INTERCHANGE:
                case START_GROUP:
                case START_TRANSACTION:
                case START_LOOP:
                case ELEMENT_DATA_BINARY:
                case END_SEGMENT:
                case END_LOOP:
                case END_TRANSACTION:
                case END_GROUP:
                case END_INTERCHANGE:
                    return true;
                default:
                    return "BIN".equals(segmentName.get());
                }
            }
        };
        ediReader = factory.createFilteredReader(ediReader, ediFilter);
        XMLStreamReader xmlReader = new StaEDIXMLStreamReader(ediReader);

        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema schema = schemaFactory.createSchema(getClass().getResource("/x12/EDISchemaBinarySegment.xml"));

        assertEquals(XMLStreamConstants.START_DOCUMENT, xmlReader.next());
        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next());
        assertEquals("INTERCHANGE", xmlReader.getLocalName());

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next());
        assertEquals("ISA", xmlReader.getLocalName());
        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next());
        assertEquals("ISA", xmlReader.getLocalName());

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.nextTag());
        assertEquals("GROUP", xmlReader.getLocalName());

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.nextTag());
        assertEquals("GS", xmlReader.getLocalName());
        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next());
        assertEquals("GS", xmlReader.getLocalName());

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.nextTag());
        assertEquals("TRANSACTION", xmlReader.getLocalName());

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.nextTag());
        assertEquals("ST", xmlReader.getLocalName());

        ediReader.setTransactionSchema(schema);

        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next());
        assertEquals("ST", xmlReader.getLocalName());

        /* BIN #1 ********************************************************************/
        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.nextTag());
        assertEquals("BIN", xmlReader.getLocalName());

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next()); // BIN01;
        assertEquals(XMLStreamConstants.CHARACTERS, xmlReader.next()); // BIN01 content;
        assertEquals("25", xmlReader.getText());
        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next()); // BIN01;

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next()); // BIN02;
        assertEquals(XMLStreamConstants.CDATA, xmlReader.next()); // BIN02 content;
        assertEquals(Base64.getEncoder().encodeToString("1234567890123456789012345".getBytes()), xmlReader.getText());
        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next()); // BIN02;

        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next());
        assertEquals("BIN", xmlReader.getLocalName());

        /* BIN #2 ********************************************************************/
        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.nextTag());
        assertEquals("BIN", xmlReader.getLocalName());

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next()); // BIN01;
        assertEquals(XMLStreamConstants.CHARACTERS, xmlReader.next()); // BIN01 content;
        assertEquals("25", xmlReader.getText());
        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next()); // BIN01;

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next()); // BIN02;
        assertEquals(XMLStreamConstants.CDATA, xmlReader.next()); // BIN02 content;
        String expected2 = Base64.getEncoder().encodeToString("12345678901234567890\n1234".getBytes());
        assertArrayEquals(expected2.toCharArray(), xmlReader.getTextCharacters());
        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next()); // BIN02;

        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next());
        assertEquals("BIN", xmlReader.getLocalName());

        /* BIN #3 ********************************************************************/
        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.nextTag());
        assertEquals("BIN", xmlReader.getLocalName());

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next()); // BIN01;
        assertEquals(XMLStreamConstants.CHARACTERS, xmlReader.next()); // BIN01 content;
        assertEquals("25", xmlReader.getText());
        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next()); // BIN01;

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next()); // BIN02;
        assertEquals(XMLStreamConstants.CDATA, xmlReader.next()); // BIN02 content;
        String expected3 = Base64.getEncoder().encodeToString("1234567890\n1234567890\n12\n".getBytes());
        char[] target3 = new char[100];
        xmlReader.getTextCharacters(xmlReader.getTextStart(), target3, 0, xmlReader.getTextLength());
        char[] result3 = Arrays.copyOf(target3, xmlReader.getTextLength());
        assertArrayEquals(expected3.toCharArray(), result3);
        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next()); // BIN02;

        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next());
        assertEquals("BIN", xmlReader.getLocalName());
    }

    @Test
    public void testGetCdataBinary_BoundsChecks() throws Exception {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple_with_binary_segment.edi");
        EDIStreamReader ediReader = factory.createEDIStreamReader(stream);
        XMLStreamReader xmlReader = new StaEDIXMLStreamReader(ediReader);
        XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
        xmlReader = xmlFactory.createFilteredReader(xmlReader, reader -> {
            if (reader.getEventType() == XMLStreamConstants.START_DOCUMENT) {
                return true;
            }

            if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
                if ("TRANSACTION".equals(reader.getLocalName())) {
                    return true;
                }
                if ("BIN02".equals(reader.getLocalName())) {
                    return true;
                }
            }
            if (reader.getEventType() == XMLStreamConstants.END_ELEMENT) {
                if ("BIN02".equals(reader.getLocalName())) {
                    return true;
                }
            }
            if (reader.getEventType() == XMLStreamConstants.CDATA) {
                return true;
            }

            return false;
        });

        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema schema = schemaFactory.createSchema(getClass().getResource("/x12/EDISchemaBinarySegment.xml"));
        char[] target = new char[100];

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next());
        assertEquals("TRANSACTION", xmlReader.getLocalName());
        ediReader.setTransactionSchema(schema);

        /* BIN #1 ********************************************************************/
        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next()); // BIN02;
        assertEquals(XMLStreamConstants.CDATA, xmlReader.next()); // BIN02 content;
        assertTrue(xmlReader.hasText());
        assertNull(xmlReader.getEncoding());

        try {
            xmlReader.getTextCharacters(xmlReader.getTextStart(), target, -1, xmlReader.getTextLength());
            fail("Exception expected");
        } catch (Exception e) {
            assertTrue(e instanceof IndexOutOfBoundsException);
        }

        try {
            xmlReader.getTextCharacters(xmlReader.getTextStart(), target, 101, xmlReader.getTextLength());
            fail("Exception expected");
        } catch (Exception e) {
            assertTrue(e instanceof IndexOutOfBoundsException);
        }

        try {
            xmlReader.getTextCharacters(xmlReader.getTextStart(), target, 0, -1);
            fail("Exception expected");
        } catch (Exception e) {
            assertTrue(e instanceof IndexOutOfBoundsException);
        }

        try {
            xmlReader.getTextCharacters(xmlReader.getTextStart(), target, 0, 101);
            fail("Exception expected");
        } catch (Exception e) {
            assertTrue(e instanceof IndexOutOfBoundsException);
        }

        assertEquals(Base64.getEncoder().encodeToString("1234567890123456789012345".getBytes()), xmlReader.getText());
        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next()); // BIN02;

        /* BIN #2 ********************************************************************/
        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next()); // BIN02;
        assertEquals(XMLStreamConstants.CDATA, xmlReader.next()); // BIN02 content;
        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next()); // BIN02;

        /* BIN #3 ********************************************************************/
        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next()); // BIN02;
        assertEquals(XMLStreamConstants.CDATA, xmlReader.next()); // BIN02 content;
        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next()); // BIN02;
    }
}
