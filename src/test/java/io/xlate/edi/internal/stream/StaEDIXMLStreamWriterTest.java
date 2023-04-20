package io.xlate.edi.internal.stream;

import static io.xlate.edi.test.StaEDITestUtil.assertEqualsNormalizeLineSeparators;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.xlate.edi.stream.EDINamespaces;
import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamWriter;

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

    static void unconfirmedBufferEquals(String expected, EDIStreamWriter writer) {
        StaEDIStreamWriter writerImpl = (StaEDIStreamWriter) writer;
        if (writerImpl.outputBuffer.position() > 0 || writerImpl.outputBuffer.limit() == writerImpl.outputBuffer.capacity()) {
            writerImpl.outputBuffer.flip();
        }
        assertEquals(expected, writerImpl.outputBuffer.toString());
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
        unconfirmedBufferEquals("ISA", ediWriter);
    }

    @Test
    void testWriteStartElementString_DefaultNamespace() throws XMLStreamException {
        it.setPrefix("l", EDINamespaces.LOOPS);
        it.setDefaultNamespace(EDINamespaces.SEGMENTS);
        it.writeStartDocument();
        it.writeStartElement("l:INTERCHANGE");
        it.writeStartElement("ISA");
        it.flush();
        unconfirmedBufferEquals("ISA", ediWriter);
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
        unconfirmedBufferEquals("ISA", ediWriter);
    }

    @Test
    void testWriteStartElementStringStringString() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", EDINamespaces.LOOPS); // Test
        it.writeStartElement("s", "ISA", EDINamespaces.SEGMENTS); // Test
        it.writeStartElement("e", "ISA01", EDINamespaces.ELEMENTS); // Test
        it.writeCharacters("00");
        it.flush();
        unconfirmedBufferEquals("ISA*00", ediWriter);
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
        unconfirmedBufferEquals("ISA**          ", ediWriter);
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
        unconfirmedBufferEquals("ISA**          ", ediWriter);
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
        unconfirmedBufferEquals("ISA**          ", ediWriter);
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
        unconfirmedBufferEquals("ISA**          ", ediWriter);
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
        unconfirmedBufferEquals("ISA*00", ediWriter);
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
    void testWriteComment() throws XMLStreamException {
        it.writeComment("");
        it.flush();
        unconfirmedBufferEquals("", ediWriter);
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
        unconfirmedBufferEquals("ISA", ediWriter);
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
        unconfirmedBufferEquals("ISA", ediWriter);
    }

    @Test
    void testWriteCData() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", EDINamespaces.LOOPS);
        it.writeCData(" \t\n\r"); // Test
        it.writeStartElement("s", "ISA", EDINamespaces.SEGMENTS);
        it.flush();
        unconfirmedBufferEquals("ISA", ediWriter);
    }

    @Test
    void testWriteCharactersString_WhitespaceLegal() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", EDINamespaces.LOOPS);
        it.writeCharacters(" \t\n\r"); // Test
        it.writeStartElement("s", "ISA", EDINamespaces.SEGMENTS);
        it.flush();
        unconfirmedBufferEquals("ISA", ediWriter);
    }

    @Test
    void testWriteCharactersString_JunkIllegal() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", EDINamespaces.LOOPS);
        XMLStreamException thrown = assertThrows(XMLStreamException.class, () -> it.writeCharacters(" illegal non-whitespace characters "));
        assertEquals("Illegal non-whitespace characters", thrown.getMessage());
    }

    @Test
    void testWriteCharactersCharArrayIntInt_WhitespaceLegal() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", EDINamespaces.LOOPS);
        it.writeCharacters(" \t \n \r  OUT OF INDEX BOUNDS".toCharArray(), 2, 6); // Test
        it.writeStartElement("s", "ISA", EDINamespaces.SEGMENTS);
        it.flush();
        unconfirmedBufferEquals("ISA", ediWriter);
    }

    @Test
    void testWriteCharactersCharArrayIntInt_JunkIllegal() throws XMLStreamException {
        it.writeStartDocument();
        it.writeStartElement("l", "INTERCHANGE", EDINamespaces.LOOPS);
        XMLStreamException thrown = assertThrows(XMLStreamException.class,
                                                 () -> it.writeCharacters(" \t \n \r  OUT OF INDEX BOUNDS".toCharArray(), 2, 7)); // Test
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

    @Test
    void testSkippedElementsValid() throws Exception {
        String input = "" +
                "<l:INTERCHANGE xmlns:l=\"urn:xlate.io:staedi:names:loops\" xmlns:s=\"urn:xlate.io:staedi:names:segments\" xmlns:c=\"urn:xlate.io:staedi:names:composites\" xmlns:e=\"urn:xlate.io:staedi:names:elements\">\n" +
                "  <s:ISA>\n" +
                "    <e:ISA01>00</e:ISA01>\n" +
                "    <e:ISA02>          </e:ISA02>\n" +
                "    <e:ISA03>00</e:ISA03>\n" +
                "    <e:ISA04>          </e:ISA04>\n" +
                "    <e:ISA05>ZZ</e:ISA05>\n" +
                "    <e:ISA06>ReceiverID     </e:ISA06>\n" +
                "    <e:ISA07>ZZ</e:ISA07>\n" +
                "    <e:ISA08>Sender         </e:ISA08>\n" +
                "    <e:ISA09>050812</e:ISA09>\n" +
                "    <e:ISA10>1953</e:ISA10>\n" +
                "    <e:ISA11>^</e:ISA11>\n" +
                "    <e:ISA12>00501</e:ISA12>\n" +
                "    <e:ISA13>000000001</e:ISA13>\n" +
                "    <e:ISA14>0</e:ISA14>\n" +
                "    <e:ISA15>P</e:ISA15>\n" +
                "    <e:ISA16>:</e:ISA16>\n" +
                "  </s:ISA>"
                + "<l:GROUP>\n" +
                "    <s:GS>\n" +
                // GS01 skipped
                "      <e:GS02>Receiver</e:GS02>\n" +
                "      <e:GS03>Sender</e:GS03>\n" +
                "      <e:GS04>20050812</e:GS04>\n" +
                "      <e:GS05>195335</e:GS05>\n" +
                "      <e:GS06>1</e:GS06>\n" +
                "      <e:GS07>X</e:GS07>\n" +
                "      <e:GS08>005010X230</e:GS08>\n" +
                "    </s:GS>"
                + "  <s:GE>\n" +
                "      <e:GE01>1</e:GE01>\n" +
                "      <e:GE02>1</e:GE02>\n" +
                "    </s:GE>\n" +
                "  </l:GROUP>"
                + "<s:IEA>\n" +
                "    <e:IEA01>1</e:IEA01>\n" +
                "    <e:IEA02>000000001</e:IEA02>\n" +
                "  </s:IEA>\n" +
                "</l:INTERCHANGE>";

        StreamSource source = new StreamSource(new ByteArrayInputStream(input.getBytes()));
        StAXResult result = new StAXResult(it);
        TransformerFactory.newInstance().newTransformer().transform(source, result);
        it.close();
        assertEqualsNormalizeLineSeparators("" +
                "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*000000001*0*P*:~\n" +
                "GS**Receiver*Sender*20050812*195335*1*X*005010X230~\n" +
                "GE*1*1~\n" +
                "IEA*1*000000001~\n" +
                "", new String(stream.toByteArray()));
    }

    @Test
    void testSkippedCompositesValid() throws Exception {
        String input = "" +
                "<l:INTERCHANGE xmlns:l=\"urn:xlate.io:staedi:names:loops\" xmlns:s=\"urn:xlate.io:staedi:names:segments\" xmlns:c=\"urn:xlate.io:staedi:names:composites\" xmlns:e=\"urn:xlate.io:staedi:names:elements\">\n" +
                "  <s:UNB>\n" +
                "    <c:UNB01>"
                + "    <e:UNB01-01>UNOA</e:UNB01-01>"
                + "    <e:UNB01-02>3</e:UNB01-02>"
                + "  </c:UNB01>\n" +
                "    <c:UNB04>"
                + "    <e:UNB04-01>200914</e:UNB04-01>"
                + "    <e:UNB04-02>1945</e:UNB04-02>"
                + "  </c:UNB04>"
                + "  <e:UNB05>1</e:UNB05>" +
                "  </s:UNB>"
                + "<s:UNZ>\n" +
                "    <e:UNZ01>0</e:UNZ01>\n" +
                "    <e:UNZ02>1</e:UNZ02>\n" +
                "  </s:UNZ>\n" +
                "</l:INTERCHANGE>";

        StreamSource source = new StreamSource(new ByteArrayInputStream(input.getBytes()));
        StAXResult result = new StAXResult(it);
        TransformerFactory.newInstance().newTransformer().transform(source, result);
        it.close();
        assertEqualsNormalizeLineSeparators("" +
                "UNB+UNOA:3+++200914:1945+1'\n" +
                "UNZ+0+1'\n", new String(stream.toByteArray()));
    }

    @Test
    void testSkippedComponentValid() throws Exception {
        String input = "" +
                "<l:INTERCHANGE xmlns:l=\"urn:xlate.io:staedi:names:loops\" xmlns:s=\"urn:xlate.io:staedi:names:segments\" xmlns:c=\"urn:xlate.io:staedi:names:composites\" xmlns:e=\"urn:xlate.io:staedi:names:elements\">\n" +
                "  <s:UNB>\n" +
                "    <c:UNB01>"
                + "    <e:UNB01-01>UNOA</e:UNB01-01>"
                + "    <e:UNB01-02>3</e:UNB01-02>"
                + "  </c:UNB01>\n" +
                "    <c:UNB04>"
                + "    <e:UNB04-02>1945</e:UNB04-02>"
                + "  </c:UNB04>"
                + "  <e:UNB05>1</e:UNB05>" +
                "  </s:UNB>"
                + "<s:UNZ>\n" +
                "    <e:UNZ01>0</e:UNZ01>\n" +
                "    <e:UNZ02>1</e:UNZ02>\n" +
                "  </s:UNZ>\n" +
                "</l:INTERCHANGE>";

        StreamSource source = new StreamSource(new ByteArrayInputStream(input.getBytes()));
        StAXResult result = new StAXResult(it);
        TransformerFactory.newInstance().newTransformer().transform(source, result);
        it.close();
        assertEqualsNormalizeLineSeparators("" +
                "UNB+UNOA:3+++:1945+1'\n" +
                "UNZ+0+1'\n", new String(stream.toByteArray()));
    }

    @Test
    void testInvalidCompositeNameThrowsException() throws Exception {
        String input = "" +
                "<l:INTERCHANGE xmlns:l=\"urn:xlate.io:staedi:names:loops\" xmlns:s=\"urn:xlate.io:staedi:names:segments\" xmlns:c=\"urn:xlate.io:staedi:names:composites\" xmlns:e=\"urn:xlate.io:staedi:names:elements\">\n" +
                "  <s:UNB>\n" +
                "    <c:UNB01>"
                + "    <e:B-01>UNOA</e:B-01>"
                + "    <e:UNB01-02>3</e:UNB01-02>"
                + "  </c:UNB01>\n" +
                "  </s:UNB>"
                + "<s:UNZ>\n" +
                "    <e:UNZ01>0</e:UNZ01>\n" +
                "    <e:UNZ02>1</e:UNZ02>\n" +
                "  </s:UNZ>\n" +
                "</l:INTERCHANGE>";

        StreamSource source = new StreamSource(new ByteArrayInputStream(input.getBytes()));
        StAXResult result = new StAXResult(it);
        Transformer tx = TransformerFactory.newInstance().newTransformer();
        Exception thrown;
        thrown = assertThrows(TransformerException.class, () -> tx.transform(source, result));
        Throwable cause = thrown;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        assertEquals(String.format(StaEDIXMLStreamWriter.MSG_INVALID_COMPONENT_NAME, "{urn:xlate.io:staedi:names:elements}B-01"),
                     cause.getMessage());
    }

    @Test
    void testInvalidCompositePositionThrowsException() throws Exception {
        String input = "" +
                "<l:INTERCHANGE xmlns:l=\"urn:xlate.io:staedi:names:loops\" xmlns:s=\"urn:xlate.io:staedi:names:segments\" xmlns:c=\"urn:xlate.io:staedi:names:composites\" xmlns:e=\"urn:xlate.io:staedi:names:elements\">\n" +
                "  <s:UNB>\n" +
                "    <c:UNB01>"
                + "    <e:UNB01-AB>UNOA</e:UNB01-AB>"
                + "    <e:UNB01-02>3</e:UNB01-02>"
                + "  </c:UNB01>\n" +
                "  </s:UNB>"
                + "<s:UNZ>\n" +
                "    <e:UNZ01>0</e:UNZ01>\n" +
                "    <e:UNZ02>1</e:UNZ02>\n" +
                "  </s:UNZ>\n" +
                "</l:INTERCHANGE>";

        StreamSource source = new StreamSource(new ByteArrayInputStream(input.getBytes()));
        StAXResult result = new StAXResult(it);
        Transformer tx = TransformerFactory.newInstance().newTransformer();
        Exception thrown;
        thrown = assertThrows(TransformerException.class, () -> tx.transform(source, result));
        Throwable cause = thrown;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        assertEquals(String.format(StaEDIXMLStreamWriter.MSG_INVALID_COMPONENT_POSITION, "{urn:xlate.io:staedi:names:elements}UNB01-AB"),
                     cause.getMessage());
    }

    @Test
    void testInvalidElementPositionThrowsException() throws Exception {
        String input = "" +
                "<l:INTERCHANGE xmlns:l=\"urn:xlate.io:staedi:names:loops\" xmlns:s=\"urn:xlate.io:staedi:names:segments\" xmlns:c=\"urn:xlate.io:staedi:names:composites\" xmlns:e=\"urn:xlate.io:staedi:names:elements\">\n" +
                "  <s:UNB>\n" +
                "    <c:UNBAA>"
                + "    <e:UNB01-01>UNOA</e:UNB01-01>"
                + "    <e:UNB01-02>3</e:UNB01-02>"
                + "  </c:UNBAA>\n" +
                "  </s:UNB>"
                + "<s:UNZ>\n" +
                "    <e:UNZ01>0</e:UNZ01>\n" +
                "    <e:UNZ02>1</e:UNZ02>\n" +
                "  </s:UNZ>\n" +
                "</l:INTERCHANGE>";

        StreamSource source = new StreamSource(new ByteArrayInputStream(input.getBytes()));
        StAXResult result = new StAXResult(it);
        Transformer tx = TransformerFactory.newInstance().newTransformer();
        Exception thrown;
        thrown = assertThrows(TransformerException.class, () -> tx.transform(source, result));
        Throwable cause = thrown;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        assertEquals(String.format(StaEDIXMLStreamWriter.MSG_INVALID_ELEMENT_NAME, "{urn:xlate.io:staedi:names:composites}UNBAA"),
                     cause.getMessage());
    }

    @Test
    void testRepeatedSegmentClearsPreviousElement() throws Exception {
        StreamSource source = new StreamSource(getClass().getResourceAsStream("/x12/issue134/repeated-ctx-ctx01.xml"));
        StAXResult result = new StAXResult(it);
        Transformer tx = TransformerFactory.newInstance().newTransformer();
        assertDoesNotThrow(() -> tx.transform(source, result));
    }
}
