package io.xlate.edi.internal.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamConstants.Namespaces;
import io.xlate.edi.stream.EDIStreamWriter;

class StaEDIXMLStreamWriterTest {

    static final Class<UnsupportedOperationException> UNSUPPORTED = UnsupportedOperationException.class;

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
        assertFalse(it.repeatedElement(new QName(Namespaces.ELEMENTS, "SG101", "e"), null));
        assertTrue(it.repeatedElement(new QName(Namespaces.ELEMENTS, "SG101", "e"),
                                      new QName(Namespaces.ELEMENTS, "SG101", "e2")));
        assertFalse(it.repeatedElement(new QName(Namespaces.ELEMENTS, "SG101", "e"),
                                       new QName(Namespaces.ELEMENTS, "SG102", "e2")));

        assertTrue(it.repeatedElement(new QName(Namespaces.ELEMENTS, "SG101", "e"),
                                      new QName(Namespaces.COMPOSITES, "SG101", "e2")));
        assertFalse(it.repeatedElement(new QName(Namespaces.ELEMENTS, "SG101", "e"),
                                       new QName(Namespaces.COMPOSITES, "SG102", "e2")));

    }

    @Test
    void testWriteStartElementString() throws XMLStreamException {
        it.setPrefix("l", Namespaces.LOOPS);
        it.setPrefix("s", Namespaces.SEGMENTS);
        it.writeStartDocument();
        it.writeStartElement("l:INTERCHANGE");
        it.writeStartElement("s:ISA");
        it.flush();
        it.close();
        assertArrayEquals("ISA".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteStartElementString_DefaultNamespace() throws XMLStreamException {
        it.setPrefix("l", Namespaces.LOOPS);
        it.setDefaultNamespace(Namespaces.SEGMENTS);
        it.writeStartDocument();
        it.writeStartElement("l:INTERCHANGE");
        it.writeStartElement("ISA");
        it.flush();
        assertArrayEquals("ISA".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteStartElementString_UndefinedNamespace() throws XMLStreamException {
        XMLStreamException thrown;

        it.setPrefix("l", Namespaces.LOOPS);
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
        it.writeStartElement(Namespaces.LOOPS, "INTERCHANGE");
        it.writeStartElement(Namespaces.SEGMENTS, "ISA");
        it.flush();
        assertArrayEquals("ISA".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteStartElementStringStringString() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", Namespaces.LOOPS); // Test
        it.writeStartElement("s", "ISA", Namespaces.SEGMENTS); // Test
        it.writeStartElement("e", "ISA01", Namespaces.ELEMENTS); // Test
        it.writeCharacters("00");
        it.flush();
        assertArrayEquals("ISA*00".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteEmptyElementStringString() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", Namespaces.LOOPS);
        it.writeStartElement("s", "ISA", Namespaces.SEGMENTS);
        it.writeEmptyElement(Namespaces.ELEMENTS, "ISA01"); // Test
        it.writeStartElement("e", "ISA02", Namespaces.ELEMENTS);
        it.writeCharacters("          ");
        it.flush();
        assertArrayEquals("ISA**          ".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteEmptyElementStringStringString() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", Namespaces.LOOPS);
        it.writeStartElement("s", "ISA", Namespaces.SEGMENTS);
        it.writeEmptyElement("e", "ISA01", Namespaces.ELEMENTS); // Test
        it.writeStartElement("e", "ISA02", Namespaces.ELEMENTS);
        it.writeCharacters("          ");
        it.flush();
        assertArrayEquals("ISA**          ".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteEmptyElementString() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", Namespaces.LOOPS);
        it.writeStartElement("s", "ISA", Namespaces.SEGMENTS);
        it.writeEmptyElement(Namespaces.ELEMENTS, "ISA01"); // Test
        it.writeStartElement("e", "ISA02", Namespaces.ELEMENTS);
        it.writeCharacters("          ");
        it.flush();
        assertArrayEquals("ISA**          ".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteEmptyElement() throws XMLStreamException {
        it.setPrefix("e", Namespaces.ELEMENTS);
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", Namespaces.LOOPS);
        it.writeStartElement("s", "ISA", Namespaces.SEGMENTS);
        it.writeEmptyElement("e:ISA01"); // Test
        it.writeStartElement("e", "ISA02", Namespaces.ELEMENTS);
        it.writeCharacters("          ");
        it.flush();
        assertArrayEquals("ISA**          ".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteEndElement() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", Namespaces.LOOPS);
        it.writeStartElement("s", "ISA", Namespaces.SEGMENTS);
        it.writeStartElement(Namespaces.ELEMENTS, "ISA01");
        it.writeCharacters("00");
        it.writeEndElement(); // Test
        it.flush();
        assertArrayEquals("ISA*00".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteEndDocument() {
        fail("Not yet implemented");
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
    void testWriteNamespace() {
        fail("Not yet implemented");
    }

    @Test
    void testWriteDefaultNamespace() {
        fail("Not yet implemented");
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
    void testWriteCData() {
        fail("Not yet implemented");
    }

    @Test
    void testWriteDTD() {
        assertThrows(UNSUPPORTED, () -> it.writeDTD(""));
    }

    @Test
    void testWriteEntityRef() {
        fail("Not yet implemented");
    }

    @Test
    void testWriteStartDocumentString() {
        fail("Not yet implemented");
    }

    @Test
    void testWriteStartDocumentStringString() {
        fail("Not yet implemented");
    }

    @Test
    void testWriteCharactersString_WhitespaceLegal() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", Namespaces.LOOPS);
        it.writeCharacters(" \t\n\r"); // Test
        it.writeStartElement("s", "ISA", Namespaces.SEGMENTS);
        it.flush();
        assertArrayEquals("ISA".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteCharactersString_JunkIllegal() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", Namespaces.LOOPS);
        XMLStreamException thrown = assertThrows(XMLStreamException.class, ()-> it.writeCharacters(" illegal non-whitespace characters "));
        assertEquals("Illegal non-whitespace characters", thrown.getMessage());
    }

    @Test
    void testWriteCharactersCharArrayIntInt_WhitespaceLegal() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", Namespaces.LOOPS);
        it.writeCharacters(" \t \n \r  OUT OF INDEX BOUNDS".toCharArray(), 2, 6); // Test
        it.writeStartElement("s", "ISA", Namespaces.SEGMENTS);
        it.flush();
        assertArrayEquals("ISA".getBytes(), stream.toByteArray());
    }

    @Test
    void testWriteCharactersCharArrayIntInt_JunkIllegal() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", Namespaces.LOOPS);
        XMLStreamException thrown = assertThrows(XMLStreamException.class, ()-> it.writeCharacters(" \t \n \r  OUT OF INDEX BOUNDS".toCharArray(), 2, 7)); // Test
        assertEquals("Illegal non-whitespace characters", thrown.getMessage());
    }

    @Test
    void testPrefixConfiguration() throws XMLStreamException {
        it.setPrefix("e", Namespaces.SEGMENTS);
        it.setPrefix("s", Namespaces.ELEMENTS);
        assertEquals("e", it.getPrefix(Namespaces.SEGMENTS));
        assertNull(it.getPrefix(Namespaces.LOOPS));
    }

    @Test
    void testSetDefaultNamespace() {
        fail("Not yet implemented");
    }

    @Test
    void testSetNamespaceContext() {
        fail("Not yet implemented");
    }

    @Test
    void testGetNamespaceContext() {
        fail("Not yet implemented");
    }

    @Test
    void testGetProperty() {
        fail("Not yet implemented");
    }
}
