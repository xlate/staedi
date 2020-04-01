package io.xlate.edi.internal.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamWriter;
import io.xlate.edi.stream.EDINamespaces;

class StaEDIXMLStreamWriterTest {

    static final Class<UnsupportedOperationException> UNSUPPORTED = UnsupportedOperationException.class;
    static final String NS_URI_TEST = "urn:names:test";
    static final String NS_PFX_TEST = "t";

    ByteArrayOutputStream stream;
    EDIStreamWriter ediWriter;
    StaEDIXMLStreamWriter it;

    @BeforeEach
    void setUp() {
        EDIOutputFactory ediOutFactory = EDIOutputFactory.newFactory();
        ediOutFactory.setProperty(EDIOutputFactory.PRETTY_PRINT, "true");

        stream = new ByteArrayOutputStream();
        ediWriter = ediOutFactory.createEDIStreamWriter(stream);
        it = new StaEDIXMLStreamWriter(ediWriter);
    }

    @Test
    void testRepeatedElement() {
        assertFalse(it.repeatedElement(new QName(EDINamespaces.ELEMENTS, "SG101", "e"), null));
        assertTrue(it.repeatedElement(new QName(EDINamespaces.ELEMENTS, "SG101", "e"),
                                      new QName(EDINamespaces.ELEMENTS, "SG101", "e2")));
        assertFalse(it.repeatedElement(new QName(EDINamespaces.ELEMENTS, "SG101", "e"),
                                       new QName(EDINamespaces.ELEMENTS, "SG102", "e2")));

        assertTrue(it.repeatedElement(new QName(EDINamespaces.ELEMENTS, "SG101", "e"),
                                      new QName(EDINamespaces.COMPOSITES, "SG101", "e2")));
        assertFalse(it.repeatedElement(new QName(EDINamespaces.ELEMENTS, "SG101", "e"),
                                       new QName(EDINamespaces.COMPOSITES, "SG102", "e2")));

    }

    @Test
    void testWriteStartElementString() throws XMLStreamException {
        it.setPrefix("l", EDINamespaces.LOOPS);
        it.setPrefix("s", EDINamespaces.SEGMENTS);
        it.writeStartDocument();
        it.writeStartElement("l:INTERCHANGE");
        it.writeStartElement("s:ISA");
        it.flush();
        it.close();
        assertArrayEquals("ISA".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteStartElementString_DefaultNamespace() throws XMLStreamException {
        it.setPrefix("l", EDINamespaces.LOOPS);
        it.setDefaultNamespace(EDINamespaces.SEGMENTS);
        it.writeStartDocument();
        it.writeStartElement("l:INTERCHANGE");
        it.writeStartElement("ISA");
        it.flush();
        assertArrayEquals("ISA".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteStartElementString_UndefinedNamespace() throws XMLStreamException {
        XMLStreamException thrown;

        it.setPrefix("l", EDINamespaces.LOOPS);
        it.writeStartDocument();
        it.writeStartElement("l:INTERCHANGE");

        it.setDefaultNamespace(null);
        thrown = assertThrows(XMLStreamException.class, () -> it.writeStartElement("ISA"));
        assertEquals("Element ISA has an undefined namespace", thrown.getMessage());

        it.setDefaultNamespace("");
        thrown = assertThrows(XMLStreamException.class, () -> it.writeStartElement("ISA"));
        assertEquals("Element ISA has an undefined namespace", thrown.getMessage());
    }

    @Test
    void testWriteStartElementStringString() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement(EDINamespaces.LOOPS, "INTERCHANGE");
        it.writeStartElement(EDINamespaces.SEGMENTS, "ISA");
        it.flush();
        assertArrayEquals("ISA".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteStartElementStringStringString() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", EDINamespaces.LOOPS); // Test
        it.writeStartElement("s", "ISA", EDINamespaces.SEGMENTS); // Test
        it.writeStartElement("e", "ISA01", EDINamespaces.ELEMENTS); // Test
        it.writeCharacters("00");
        it.flush();
        assertArrayEquals("ISA*00".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteEmptyElementStringString() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", EDINamespaces.LOOPS);
        it.writeStartElement("s", "ISA", EDINamespaces.SEGMENTS);
        it.writeEmptyElement(EDINamespaces.ELEMENTS, "ISA01"); // Test
        it.writeStartElement("e", "ISA02", EDINamespaces.ELEMENTS);
        it.writeCharacters("          ");
        it.flush();
        assertArrayEquals("ISA**          ".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteEmptyElementStringStringString() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", EDINamespaces.LOOPS);
        it.writeStartElement("s", "ISA", EDINamespaces.SEGMENTS);
        it.writeEmptyElement("e", "ISA01", EDINamespaces.ELEMENTS); // Test
        it.writeStartElement("e", "ISA02", EDINamespaces.ELEMENTS);
        it.writeCharacters("          ");
        it.flush();
        assertArrayEquals("ISA**          ".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteEmptyElementString() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", EDINamespaces.LOOPS);
        it.writeStartElement("s", "ISA", EDINamespaces.SEGMENTS);
        it.writeEmptyElement(EDINamespaces.ELEMENTS, "ISA01"); // Test
        it.writeStartElement("e", "ISA02", EDINamespaces.ELEMENTS);
        it.writeCharacters("          ");
        it.flush();
        assertArrayEquals("ISA**          ".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteEmptyElement() throws XMLStreamException {
        it.setPrefix("e", EDINamespaces.ELEMENTS);
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", EDINamespaces.LOOPS);
        it.writeStartElement("s", "ISA", EDINamespaces.SEGMENTS);
        it.writeEmptyElement("e:ISA01"); // Test
        it.writeStartElement("e", "ISA02", EDINamespaces.ELEMENTS);
        it.writeCharacters("          ");
        it.flush();
        assertArrayEquals("ISA**          ".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteEmptyElement_OutOfSequence() throws XMLStreamException {
        XMLStreamException thrown;

        it.setPrefix("e", EDINamespaces.ELEMENTS);
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", EDINamespaces.LOOPS);
        thrown = assertThrows(XMLStreamException.class, () -> it.writeEmptyElement("e:ISA01"));
        assertEquals(IllegalStateException.class, thrown.getCause().getClass());
    }

    @Test
    void testWriteEndElement() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", EDINamespaces.LOOPS);
        it.writeStartElement("s", "ISA", EDINamespaces.SEGMENTS);
        it.writeStartElement(EDINamespaces.ELEMENTS, "ISA01");
        it.writeCharacters("00");
        it.writeEndElement(); // Test
        it.flush();
        assertArrayEquals("ISA*00".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteAttributeStringString() {
        assertThrows(UNSUPPORTED, () -> it.writeAttribute("", ""));
    }

    @Test
    void testWriteAttributeStringStringStringString() {
        assertThrows(UNSUPPORTED, () -> it.writeAttribute("", "", "", ""));
    }

    @Test
    void testWriteAttributeStringStringString() {
        assertThrows(UNSUPPORTED, () -> it.writeAttribute("", "", ""));
    }

    @Test
    void testWriteComment() {
        assertThrows(UNSUPPORTED, () -> it.writeComment(""));
    }

    @Test
    void testWriteProcessingInstructionString() {
        assertThrows(UNSUPPORTED, () -> it.writeProcessingInstruction(""));
    }

    @Test
    void testWriteProcessingInstructionStringString() {
        assertThrows(UNSUPPORTED, () -> it.writeProcessingInstruction("", ""));
    }

    @Test
    void testWriteDTD() {
        assertThrows(UNSUPPORTED, () -> it.writeDTD(""));
    }

    @Test
    void testWriteEntityRef() {
        assertThrows(UNSUPPORTED, () -> it.writeEntityRef(""));
    }

    @Test
    void testWriteStartDocumentString() throws XMLStreamException {
        it.setPrefix("l", EDINamespaces.LOOPS);
        it.setPrefix("s", EDINamespaces.SEGMENTS);
        it.writeStartDocument("1.0"); // Test
        it.writeStartElement("l:INTERCHANGE");
        it.writeStartElement("s:ISA");
        it.flush();
        it.close();
        assertArrayEquals("ISA".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteStartDocumentStringString() throws XMLStreamException {
        it.setPrefix("l", EDINamespaces.LOOPS);
        it.setPrefix("s", EDINamespaces.SEGMENTS);
        it.writeStartDocument("UTF-8", "1.0"); // Test
        it.writeStartElement("l:INTERCHANGE");
        it.writeStartElement("s:ISA");
        it.flush();
        it.close();
        assertArrayEquals("ISA".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteCData() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", EDINamespaces.LOOPS);
        it.writeCData(" \t\n\r"); // Test
        it.writeStartElement("s", "ISA", EDINamespaces.SEGMENTS);
        it.flush();
        assertArrayEquals("ISA".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteCharactersString_WhitespaceLegal() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", EDINamespaces.LOOPS);
        it.writeCharacters(" \t\n\r"); // Test
        it.writeStartElement("s", "ISA", EDINamespaces.SEGMENTS);
        it.flush();
        assertArrayEquals("ISA".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteCharactersString_JunkIllegal() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", EDINamespaces.LOOPS);
        XMLStreamException thrown = assertThrows(XMLStreamException.class, ()-> it.writeCharacters(" illegal non-whitespace characters "));
        assertEquals("Illegal non-whitespace characters", thrown.getMessage());
    }

    @Test
    void testWriteCharactersCharArrayIntInt_WhitespaceLegal() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", EDINamespaces.LOOPS);
        it.writeCharacters(" \t \n \r  OUT OF INDEX BOUNDS".toCharArray(), 2, 6); // Test
        it.writeStartElement("s", "ISA", EDINamespaces.SEGMENTS);
        it.flush();
        assertArrayEquals("ISA".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteCharactersCharArrayIntInt_JunkIllegal() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", EDINamespaces.LOOPS);
        XMLStreamException thrown = assertThrows(XMLStreamException.class, ()-> it.writeCharacters(" \t \n \r  OUT OF INDEX BOUNDS".toCharArray(), 2, 7)); // Test
        assertEquals("Illegal non-whitespace characters", thrown.getMessage());
    }

    @Test
    void testPrefixConfiguration() throws XMLStreamException {
        it.setPrefix("e", EDINamespaces.SEGMENTS);
        it.setPrefix("s", EDINamespaces.ELEMENTS);
        assertEquals("e", it.getPrefix(EDINamespaces.SEGMENTS));
        assertNull(it.getPrefix(EDINamespaces.LOOPS));
    }

    @Test
    void testSetNamespaceContext() throws XMLStreamException {
        NamespaceContext ctx = Mockito.mock(NamespaceContext.class);
        Mockito.when(ctx.getNamespaceURI(NS_PFX_TEST)).thenReturn(NS_URI_TEST);
        Mockito.when(ctx.getPrefix(NS_URI_TEST)).thenReturn(NS_PFX_TEST);
        it.setNamespaceContext(ctx);
        assertEquals(NS_PFX_TEST, it.getContextPrefix(NS_URI_TEST));
        assertEquals(NS_URI_TEST, it.getContextNamespaceURI(NS_PFX_TEST));
        assertNull(it.getContextNamespaceURI("m"));
        assertNull(it.getContextPrefix("urn:names:missing"));
    }

    @Test
    void testSetNamespaceContext_MultipleCalls() throws XMLStreamException {
        NamespaceContext ctx = Mockito.mock(NamespaceContext.class);
        Mockito.when(ctx.getNamespaceURI(NS_PFX_TEST)).thenReturn(NS_URI_TEST);
        Mockito.when(ctx.getPrefix(NS_URI_TEST)).thenReturn(NS_PFX_TEST);
        it.setNamespaceContext(ctx);
        Throwable thrown = assertThrows(XMLStreamException.class, () -> it.setNamespaceContext(ctx));
        assertEquals("NamespaceContext has already been set", thrown.getMessage());
    }

    @Test
    void testSetNamespaceContext_AfterDocumentStart() throws XMLStreamException {
        NamespaceContext ctx = Mockito.mock(NamespaceContext.class);
        Mockito.when(ctx.getNamespaceURI(NS_PFX_TEST)).thenReturn(NS_URI_TEST);
        Mockito.when(ctx.getPrefix(NS_URI_TEST)).thenReturn(NS_PFX_TEST);
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", EDINamespaces.LOOPS);
        Throwable thrown = assertThrows(XMLStreamException.class, () -> it.setNamespaceContext(ctx));
        assertEquals("NamespaceContext must only be called at the start of the document", thrown.getMessage());
    }

    @Test
    void testGetNamespaceContext() throws XMLStreamException {
        NamespaceContext ctx = Mockito.mock(NamespaceContext.class);
        it.setNamespaceContext(ctx);

        assertEquals(ctx, it.getNamespaceContext());
    }

    @Test
    void testGetProperty() {
        assertThrows(IllegalArgumentException.class, () -> it.getProperty("anything"));
    }
}
