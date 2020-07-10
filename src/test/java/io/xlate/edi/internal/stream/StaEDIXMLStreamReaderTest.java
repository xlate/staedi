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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.DefaultComparisonFormatter;
import org.xmlunit.diff.Diff;

import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDINamespaces;
import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamFilter;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.EDIStreamWriter;
import io.xlate.edi.stream.Location;

@SuppressWarnings("resource")
class StaEDIXMLStreamReaderTest {

    static byte[] DUMMY_X12 = ("ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
            + "S01*X~"
            + "S11*X~"
            + "S12*X~"
            + "S19*X~"
            + "S09*X~"
            + "IEA*1*508121953~").getBytes();

    static byte[] TINY_X12 = ("ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
            + "IEA*1*508121953~").getBytes();

    private EDIStreamReader ediReader;

    XMLStreamReader getXmlReader(String resource) throws XMLStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream(resource);
        ediReader = factory.createEDIStreamReader(stream);
        return new StaEDIXMLStreamReader(ediReader);
    }

    XMLStreamReader getXmlReader(byte[] bytes) throws XMLStreamException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        factory.setProperty(EDIInputFactory.EDI_VALIDATE_CONTROL_STRUCTURE, "false");
        InputStream stream = new ByteArrayInputStream(bytes);
        ediReader = factory.createEDIStreamReader(stream);
        assertEquals(EDIStreamEvent.START_INTERCHANGE, ediReader.next());
        return new StaEDIXMLStreamReader(ediReader);
    }

    static void skipEvents(XMLStreamReader reader, int eventCount) throws XMLStreamException {
        for (int i = 0; i < eventCount; i++) {
            reader.next();
        }
    }

    @Test
    void testCreateEDIXMLStreamReader() throws XMLStreamException {
        XMLStreamReader xmlReader = getXmlReader("/x12/simple997.edi");
        assertNotNull(xmlReader, "xmlReader was null");
    }

    @Test
    void testHasNext() throws Exception {
        XMLStreamReader xmlReader = getXmlReader(TINY_X12);
        assertTrue(xmlReader.hasNext());
        xmlReader.close();
        assertThrows(XMLStreamException.class, () -> xmlReader.hasNext());
        assertThrows(XMLStreamException.class, () -> xmlReader.next());
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
    void testSegmentSequence() throws Exception {
        XMLStreamReader xmlReader = getXmlReader(DUMMY_X12);

        assertEquals(XMLStreamConstants.START_DOCUMENT, xmlReader.getEventType());
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
    void testRequire() throws Exception {
        XMLStreamReader xmlReader = getXmlReader(DUMMY_X12);
        XMLStreamException thrown;

        assertEquals(XMLStreamConstants.START_DOCUMENT, xmlReader.getEventType());
        thrown = assertThrows(XMLStreamException.class,
                              () -> xmlReader.require(XMLStreamConstants.START_DOCUMENT, EDINamespaces.LOOPS, "INTERCHANGE"));
        assertTrue(thrown.getMessage().endsWith("does not have a corresponding name"));
        thrown = assertThrows(XMLStreamException.class,
                              () -> xmlReader.require(XMLStreamConstants.START_ELEMENT, EDINamespaces.LOOPS, "INTERCHANGE"));
        assertTrue(thrown.getMessage().contains("does not match required type"));
        xmlReader.next();
        // Happy Path
        xmlReader.require(XMLStreamConstants.START_ELEMENT, EDINamespaces.LOOPS, "INTERCHANGE");
        xmlReader.require(XMLStreamConstants.START_ELEMENT, EDINamespaces.LOOPS, null);
        xmlReader.require(XMLStreamConstants.START_ELEMENT, null, "INTERCHANGE");
        xmlReader.require(XMLStreamConstants.START_ELEMENT, null, null);

        thrown = assertThrows(XMLStreamException.class,
                              () -> xmlReader.require(XMLStreamConstants.START_ELEMENT, EDINamespaces.LOOPS, "GROUP"));
        assertTrue(thrown.getMessage().contains("does not match required localName"));

        thrown = assertThrows(XMLStreamException.class,
                              () -> xmlReader.require(XMLStreamConstants.START_ELEMENT, EDINamespaces.SEGMENTS, "INTERCHANGE"));
        assertTrue(thrown.getMessage().contains("does not match required namespaceURI"));

    }

    @Test
    void testGetElementText() throws Exception {
        XMLStreamReader xmlReader = getXmlReader(DUMMY_X12);
        XMLStreamException thrown;

        assertEquals(XMLStreamConstants.START_DOCUMENT, xmlReader.getEventType());
        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next());
        assertEquals("INTERCHANGE", xmlReader.getLocalName());

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next());
        assertEquals("ISA", xmlReader.getLocalName());
        thrown = assertThrows(XMLStreamException.class, () -> xmlReader.getElementText());
        assertEquals("Element text only available for simple element", thrown.getMessage());

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next()); // ISA01;
        assertEquals("00", xmlReader.getElementText());
        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next()); // ISA02;
        assertEquals("          ", xmlReader.getElementText());
        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next()); // ISA03;
        assertEquals("00", xmlReader.getElementText());

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next()); // ISA04;
        xmlReader.next(); // CHARACTERS
        thrown = assertThrows(XMLStreamException.class, () -> xmlReader.getElementText());
        assertEquals("Element text only available on START_ELEMENT", thrown.getMessage());

    }

    @Test
    void testNamespaces() throws Exception {
        XMLStreamReader xmlReader = getXmlReader(TINY_X12);

        assertEquals(XMLStreamConstants.START_DOCUMENT, xmlReader.getEventType());
        assertNull(xmlReader.getNamespaceURI());
        assertThrows(IllegalStateException.class, () -> xmlReader.getName());

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next());
        assertEquals(EDINamespaces.LOOPS, xmlReader.getNamespaceURI("l"));
        assertEquals("INTERCHANGE", xmlReader.getLocalName());
        assertEquals(EDINamespaces.LOOPS, xmlReader.getName().getNamespaceURI());
        assertEquals("l", xmlReader.getName().getPrefix());
        assertEquals(4, xmlReader.getNamespaceCount());
        assertEquals(EDINamespaces.LOOPS, xmlReader.getNamespaceURI());

        assertSegmentBoundaries(xmlReader, "ISA", 16);

        NamespaceContext context = xmlReader.getNamespaceContext();
        assertEquals("s", context.getPrefix(EDINamespaces.SEGMENTS));
        assertEquals("s", context.getPrefixes(EDINamespaces.SEGMENTS).next());
        assertEquals(EDINamespaces.SEGMENTS, context.getNamespaceURI("s"));
        assertNull(context.getNamespaceURI("x"));
        assertEquals(EDINamespaces.SEGMENTS, xmlReader.getNamespaceURI());

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next());
        assertEquals("IEA", xmlReader.getLocalName());
        assertEquals(EDINamespaces.SEGMENTS, xmlReader.getNamespaceURI());

        // No namespaces declared on the segment
        assertEquals(0, xmlReader.getNamespaceCount());
        assertNull(xmlReader.getNamespacePrefix(0));
        assertNull(xmlReader.getNamespaceURI(0));

        assertElement(xmlReader, "IEA01", "1");
        assertEquals(EDINamespaces.ELEMENTS, xmlReader.getNamespaceURI());

        // No namespaces declared on the element
        assertEquals(0, xmlReader.getNamespaceCount());
        assertNull(xmlReader.getNamespacePrefix(0));
        assertNull(xmlReader.getNamespaceURI(0));

        assertElement(xmlReader, "IEA02", "508121953");
        assertEquals(EDINamespaces.ELEMENTS, xmlReader.getNamespaceURI());

        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next());
        assertEquals("IEA", xmlReader.getLocalName());
        assertEquals(EDINamespaces.SEGMENTS, xmlReader.getNamespaceURI());

        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.next());
        assertEquals("INTERCHANGE", xmlReader.getLocalName());
        assertEquals(EDINamespaces.LOOPS, xmlReader.getName().getNamespaceURI());
        assertEquals("l", xmlReader.getName().getPrefix());
        assertEquals(4, xmlReader.getNamespaceCount());
        assertEquals(EDINamespaces.LOOPS, xmlReader.getNamespaceURI());

        assertEquals(XMLStreamConstants.END_DOCUMENT, xmlReader.next());
        assertNull(xmlReader.getNamespaceURI("l"));
    }

    private void assertElement(XMLStreamReader xmlReader, String tag, String value) throws Exception {
        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next());
        assertEquals(tag, xmlReader.getLocalName());
        assertEquals(value, xmlReader.getElementText());
        assertEquals(XMLStreamConstants.END_ELEMENT, xmlReader.getEventType());
        assertEquals(tag, xmlReader.getLocalName());
    }

    @Test
    void testElementEvents() throws Exception {
        XMLStreamReader xmlReader = getXmlReader(TINY_X12);
        assertEquals(XMLStreamConstants.START_DOCUMENT, xmlReader.getEventType());

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
    void testReadXml() throws Exception {
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
    void testReadXml_WithOptionalInterchangeServiceRequests_TransactionOnly() throws Exception {
        EDIInputFactory ediFactory = EDIInputFactory.newFactory();
        XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
        InputStream stream = getClass().getResourceAsStream("/x12/optionalInterchangeServices.edi");
        ediFactory.setProperty(EDIInputFactory.XML_DECLARE_TRANSACTION_XMLNS, Boolean.TRUE);
        EDIStreamReader reader = ediFactory.createEDIStreamReader(stream);
        EDIStreamReader filtered = ediFactory.createFilteredReader(reader, r -> true);
        XMLStreamReader xmlReader = ediFactory.createXMLStreamReader(filtered);
        xmlReader.next(); // Per StAXSource JavaDoc, put in START_DOCUMENT state
        XMLStreamReader xmlCursor = xmlFactory.createFilteredReader(xmlReader, r -> {
            boolean startTx = (r.getEventType() == XMLStreamConstants.START_ELEMENT && r.getName().getLocalPart().equals("TRANSACTION"));

            if (!startTx) {
                Logger.getGlobal().info("Skipping event: " + r.getEventType() + "; "
                        + (r.getEventType() == XMLStreamConstants.START_ELEMENT || r.getEventType() == XMLStreamConstants.END_ELEMENT
                                ? r.getName()
                                : ""));
            }

            return startTx;
        });

        xmlCursor.hasNext();

        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StringWriter result = new StringWriter();
        transformer.transform(new StAXSource(xmlReader), new StreamResult(result));
        String resultString = result.toString();
        System.out.println(resultString);
        Diff d = DiffBuilder.compare(Input.fromFile("src/test/resources/x12/optionalInterchangeServices_transactionOnly.xml"))
                            .withTest(resultString).build();
        assertTrue(!d.hasDifferences(), () -> "XML unexpectedly different:\n" + d.toString(new DefaultComparisonFormatter()));
    }

    @Test
    void testTransactionElementWithXmlns() throws Exception {
        EDIInputFactory ediFactory = EDIInputFactory.newFactory();
        ediFactory.setProperty(EDIInputFactory.XML_DECLARE_TRANSACTION_XMLNS, Boolean.TRUE);
        InputStream stream = getClass().getResourceAsStream("/x12/extraDelimiter997.edi");
        ediReader = ediFactory.createEDIStreamReader(stream);
        XMLStreamReader xmlReader = ediFactory.createXMLStreamReader(ediReader);

        xmlReader.next(); // Per StAXSource JavaDoc, put in START_DOCUMENT state
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StringWriter result = new StringWriter();
        transformer.transform(new StAXSource(xmlReader), new StreamResult(result));
        String resultString = result.toString();
        Diff d = DiffBuilder.compare(Input.fromFile("src/test/resources/x12/extraDelimiter997-transaction-xmlns.xml"))
                            .withTest(resultString).build();
        assertTrue(!d.hasDifferences(), () -> "XML unexpectedly different:\n" + d.toString(new DefaultComparisonFormatter()));
    }

    @Test
    void testXmlIOEquivalence() throws Exception {
        XMLStreamReader xmlReader = getXmlReader("/x12/extraDelimiter997.edi");
        xmlReader.next(); // Per StAXSource JavaDoc, put in START_DOCUMENT state
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StringWriter result = new StringWriter();
        transformer.transform(new StAXSource(xmlReader), new StreamResult(result));
        String resultString = result.toString();

        EDIOutputFactory ediOutFactory = EDIOutputFactory.newFactory();
        ediOutFactory.setProperty(EDIOutputFactory.PRETTY_PRINT, "true");
        ByteArrayOutputStream resultEDI = new ByteArrayOutputStream();
        EDIStreamWriter ediWriter = ediOutFactory.createEDIStreamWriter(resultEDI);
        XMLStreamWriter xmlWriter = new StaEDIXMLStreamWriter(ediWriter);
        transformer.transform(new StreamSource(new StringReader(resultString)), new StAXResult(xmlWriter));
        String expectedEDI = String.join("\n", Files.readAllLines(Paths.get("src/test/resources/x12/extraDelimiter997.edi")));
        assertEquals(expectedEDI, new String(resultEDI.toByteArray()).trim());
    }

    @Test
    void testSchemaValidatedInput() throws Exception {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema schema = schemaFactory.createSchema(getClass().getResource("/x12/EDISchema997.xml"));
        EDIStreamReader ediReader = factory.createEDIStreamReader(stream);
        EDIStreamFilter ediFilter = (reader) -> {
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
    void testUnsupportedOperations() throws Exception {
        EDIStreamReader ediReader = Mockito.mock(EDIStreamReader.class);
        XMLStreamReader xmlReader = new StaEDIXMLStreamReader(ediReader);
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
            xmlReader.getPITarget();
            fail("UnsupportedOperationExpected");
        } catch (UnsupportedOperationException e) {}
        try {
            xmlReader.getPIData();
            fail("UnsupportedOperationExpected");
        } catch (UnsupportedOperationException e) {}
    }

    @Test
    void testGetTextString() throws Exception {
        XMLStreamReader xmlReader = getXmlReader(DUMMY_X12);

        assertEquals(XMLStreamConstants.START_DOCUMENT, xmlReader.getEventType());
        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next());
        assertEquals("INTERCHANGE", xmlReader.getLocalName());

        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next());
        assertEquals("ISA", xmlReader.getLocalName());
        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next()); // ISA01;
        assertEquals(XMLStreamConstants.CHARACTERS, xmlReader.next()); // ISA01 content;
        assertNull(xmlReader.getNamespaceURI());
        assertThrows(IllegalStateException.class, () -> xmlReader.getName());

        String textString = xmlReader.getText();
        assertEquals("00", textString);
        char[] textArray = xmlReader.getTextCharacters();
        assertArrayEquals(new char[] { '0', '0' }, textArray);
        char[] textArray2 = xmlReader.getTextCharacters(); // 2nd call should be the same characters
        assertArrayEquals(textArray, textArray2);
        char[] textArrayLocal = new char[3];
        xmlReader.getTextCharacters(xmlReader.getTextStart(), textArrayLocal, 0, xmlReader.getTextLength());
        assertArrayEquals(new char[] { '0', '0', '\0' }, textArrayLocal);
    }

    @Test
    void testGetCdataBinary() throws Exception {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple_with_binary_segment.edi");
        EDIStreamReader ediReader = factory.createEDIStreamReader(stream);
        AtomicReference<String> segmentName = new AtomicReference<>();
        EDIStreamFilter ediFilter = (reader) -> {
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
        assertArrayEquals(expected2.toCharArray(), xmlReader.getTextCharacters()); // 2nd call should be the same characters

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
    void testGetCdataBinary_BoundsChecks() throws Exception {
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

    @Test
    void testGetProperty() throws Exception {
        XMLStreamReader xmlReader = getXmlReader(DUMMY_X12);
        assertNull(xmlReader.getProperty("DUMMY"));
        assertThrows(IllegalArgumentException.class, () -> xmlReader.getProperty(null));
    }

    @Test
    void testLocationParity() throws Exception {
        XMLStreamReader xmlReader = getXmlReader(DUMMY_X12);
        boolean hasEvents = false;

        while (xmlReader.hasNext()) {
            hasEvents = true;
            xmlReader.next();
            assertEquals(ediReader.getLocation().getLineNumber(), xmlReader.getLocation().getLineNumber());
            assertEquals(ediReader.getLocation().getColumnNumber(), xmlReader.getLocation().getColumnNumber());
            assertEquals(ediReader.getLocation().getCharacterOffset(), xmlReader.getLocation().getCharacterOffset());
        }

        assertTrue(hasEvents);
    }

    @Test
    void testEDIReporterSet() throws Exception {
        EDIInputFactory ediFactory = EDIInputFactory.newFactory();
        Map<String, Set<EDIStreamValidationError>> errors = new LinkedHashMap<>();
        ediFactory.setErrorReporter((errorEvent, reader) -> {
            Location location = reader.getLocation();
            String key;

            if (location.getElementPosition() > 0) {
                key = String.format("%s%02d@%d",
                                    location.getSegmentTag(),
                                    location.getElementPosition(),
                                    location.getSegmentPosition());
            } else {
                key = String.format("%s@%d",
                                    location.getSegmentTag(),
                                    location.getSegmentPosition());
            }

            if (!errors.containsKey(key)) {
                errors.put(key, new HashSet<EDIStreamValidationError>(2));
            }

            errors.get(key).add(errorEvent);
        });
        InputStream stream = getClass().getResourceAsStream("/x12/invalid999.edi");
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema schema = schemaFactory.createSchema(getClass().getResource("/x12/EDISchema999.xml"));

        EDIStreamReader ediReader = ediFactory.createEDIStreamReader(stream);
        ediReader = ediFactory.createFilteredReader(ediReader, (reader) -> {
            if (reader.getEventType() == EDIStreamEvent.START_TRANSACTION) {
                reader.setTransactionSchema(schema);
            }
            return true;
        });

        XMLStreamReader xmlReader = new StaEDIXMLStreamReader(ediReader);

        xmlReader.next(); // Per StAXSource JavaDoc, put in START_DOCUMENT state
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StringWriter result = new StringWriter();
        transformer.transform(new StAXSource(xmlReader), new StreamResult(result));
        String resultString = result.toString();
        System.out.println("Errors: " + errors);
        System.out.println(resultString);

        Iterator<Entry<String, Set<EDIStreamValidationError>>> errorSet = errors.entrySet().iterator();
        Entry<String, Set<EDIStreamValidationError>> error = errorSet.next();
        assertEquals("IK5@4", error.getKey());
        assertEquals(EDIStreamValidationError.UNEXPECTED_SEGMENT, error.getValue().iterator().next());
        error = errorSet.next();
        assertEquals("AK101@5", error.getKey());
        assertEquals(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG, error.getValue().iterator().next());
        error = errorSet.next();
        assertEquals("AK204@6", error.getKey());
        assertEquals(EDIStreamValidationError.TOO_MANY_DATA_ELEMENTS, error.getValue().iterator().next());
        error = errorSet.next();
        assertEquals("AK205@6", error.getKey());
        assertEquals(EDIStreamValidationError.TOO_MANY_DATA_ELEMENTS, error.getValue().iterator().next());
        error = errorSet.next();
        assertEquals("IK501@7", error.getKey());
        assertEquals(EDIStreamValidationError.INVALID_CODE_VALUE, error.getValue().iterator().next());
        error = errorSet.next();
        assertEquals("IK5@8", error.getKey());
        assertEquals(EDIStreamValidationError.SEGMENT_EXCEEDS_MAXIMUM_USE, error.getValue().iterator().next());

        Diff d = DiffBuilder.compare(Input.fromFile("src/test/resources/x12/invalid999_transformed.xml"))
                            .withTest(resultString).build();
        assertTrue(!d.hasDifferences(), () -> "XML unexpectedly different:\n" + d.toString(new DefaultComparisonFormatter()));
    }

    @Test
    void testEDIReporterUnset() throws Exception {
        EDIInputFactory ediFactory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/invalid999.edi");
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema schema = schemaFactory.createSchema(getClass().getResource("/x12/EDISchema999.xml"));

        EDIStreamReader ediReader = ediFactory.createEDIStreamReader(stream);
        ediReader = ediFactory.createFilteredReader(ediReader, (reader) -> {
            if (reader.getEventType() == EDIStreamEvent.START_TRANSACTION) {
                reader.setTransactionSchema(schema);
            }
            return true;
        });

        XMLStreamReader xmlReader = new StaEDIXMLStreamReader(ediReader);

        xmlReader.next(); // Per StAXSource JavaDoc, put in START_DOCUMENT state
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StringWriter result = new StringWriter();
        TransformerException thrown = assertThrows(TransformerException.class,
                                                   () -> transformer.transform(new StAXSource(xmlReader), new StreamResult(result)));
        Throwable cause = thrown.getCause();
        assertTrue(cause instanceof XMLStreamException);
        javax.xml.stream.Location l = ((XMLStreamException) cause).getLocation();
        assertEquals("ParseError at [row,col]:[" + l.getLineNumber() + "," +
                l.getColumnNumber() + "]\n" +
                "Message: " + "Segment IK5 has error UNEXPECTED_SEGMENT", cause.getMessage());
    }
}
