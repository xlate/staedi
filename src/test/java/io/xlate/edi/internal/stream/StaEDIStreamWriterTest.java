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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.xlate.edi.internal.schema.SchemaUtils;
import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamConstants;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.EDIStreamWriter;
import io.xlate.edi.stream.EDIValidationException;
import io.xlate.edi.stream.Location;

@SuppressWarnings("resource")
class StaEDIStreamWriterTest {

    private void writeHeader(EDIStreamWriter writer) throws EDIStreamException {
        //ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~
        writer.writeStartSegment("ISA");
        writer.writeElement("00").writeElement("          ");
        writer.writeElement("00").writeElement("          ");
        writer.writeElement("ZZ").writeElement("ReceiverID     ");
        writer.writeElement("ZZ").writeElement("Sender         ");
        writer.writeElement("050812");
        writer.writeElement("1953");
        writer.writeElement("^");
        writer.writeElement("00501");
        writer.writeElement("508121953");
        writer.writeElement("0");
        writer.writeElement("P");
        writer.writeElement(":");
        writer.writeEndSegment();
    }

    @Test
    void testGetProperty() {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        factory.setProperty(EDIStreamConstants.Delimiters.SEGMENT, '~');
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        Object segmentTerminator = writer.getProperty(EDIStreamConstants.Delimiters.SEGMENT);
        assertEquals(Character.valueOf('~'), segmentTerminator);
    }

    @Test
    void testGetNullProperty() {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(1);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        assertThrows(IllegalArgumentException.class, () -> writer.getProperty(null));
    }

    @Test
    void testStartInterchange() {
        try {
            EDIOutputFactory factory = EDIOutputFactory.newFactory();
            OutputStream stream = new ByteArrayOutputStream(4096);
            EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
            writer.startInterchange();
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    void testStartInterchangeIllegalX12() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        assertThrows(IllegalStateException.class, () -> writer.startInterchange());
    }

    @Test
    void testStartInterchangeIllegalEDIFACTA() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("UNA");
        assertThrows(IllegalStateException.class, () -> writer.startInterchange());
    }

    @Test
    void testStartInterchangeIllegalEDIFACTB() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("UNB");
        assertThrows(IllegalStateException.class, () -> writer.startInterchange());
    }

    @Test
    void testEndInterchange() {
        try {
            EDIOutputFactory factory = EDIOutputFactory.newFactory();
            OutputStream stream = new ByteArrayOutputStream(4096);
            EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
            writer.startInterchange();
            writer.endInterchange();
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    void testEndInterchangeIllegal() {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        assertThrows(IllegalStateException.class, () -> writer.endInterchange());
    }

    @Test
    void testWriteStartSegment() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        writer.flush();
        assertEquals("ISA", stream.toString());
    }

    @Test
    void testWriteInvalidHeaderElement() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        writer.writeElement("00").writeElement("           "); // Too long
        writer.writeElement("00").writeElement("           "); // Too long
        writer.writeElement("ZZ").writeElement("ReceiverID     ");
        writer.writeElement("ZZ").writeElement("Sender         ");
        writer.writeElement("050812");
        writer.writeElement("1953");
        writer.writeElement("^");
        writer.writeElement("00501");
        writer.writeElement("508121953");
        writer.writeElement("0");
        writer.writeElement("P");
        EDIStreamException thrown = assertThrows(EDIStreamException.class, () -> writer.writeElement(":"));
        assertEquals("Unexpected header character: 0x002A [*] in segment ISA at position 1, element 15", thrown.getMessage());
    }

    @Test
    void testWriteStartSegmentIllegal() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        writer.writeStartElement();
        assertThrows(IllegalStateException.class, () -> writer.writeStartSegment("GS"));
    }

    @Test
    void testWriteEndSegment() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        writer.writeStartElement().writeElementData("E1").endElement();
        writer.writeEndSegment();
        writer.flush();
        assertEquals("ISA*E1~", stream.toString());
    }

    @Test
    void testWriteEndSegmentIllegal() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        assertThrows(IllegalStateException.class, () -> writer.writeEndSegment());
    }

    @Test
    void testWriteStartElement() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        writer.writeStartElement()
              .endElement()
              .writeStartElement()
              .endElement()
              .writeStartElement()
              .endElement()
              .writeStartElement()
              .endElement();
        writer.flush();
        assertEquals("ISA****", stream.toString());
    }

    @Test
    void testWriteStartElementIllegal() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        writer.writeStartElement();
        writer.startComponent();
        assertThrows(IllegalStateException.class, () -> writer.writeStartElement());
    }

    @Test
    void testWriteInvalidCharacter() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        writer.writeStartElement();
        EDIStreamException thrown = assertThrows(EDIStreamException.class,
                                                 () -> writer.writeElementData("\u0008\u0010"));
        assertEquals("Invalid character: 0x0008 in segment ISA at position 1, element 1", thrown.getMessage());
    }

    @Test
    void testWriteInvalidCharacterRepeatedComposite() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writeHeader(writer);
        writer.writeStartSegment("FOO");
        writer.writeElement("BAR1");
        writer.writeRepeatElement(); // starts new element
        writer.writeComponent("BAR2");
        writer.writeComponent("BAR3");
        EDIStreamException thrown = assertThrows(EDIStreamException.class,
                                                 () -> writer.writeComponent("\u0008\u0010"));
        assertEquals("Invalid character: 0x0008 in segment FOO at position 2, element 1 (occurrence 2), component 3", thrown.getMessage());
        Location l = thrown.getLocation();
        assertEquals("FOO", l.getSegmentTag());
        assertEquals(2, l.getSegmentPosition());
        assertEquals(1, l.getElementPosition());
        assertEquals(2, l.getElementOccurrence());
        assertEquals(3, l.getComponentPosition());
    }

    @Test
    void testWriteInvalidSegmentTag() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writeHeader(writer);
        writer.writeStartSegment("G");
        EDIStreamException thrown = assertThrows(EDIStreamException.class,
                                                 () -> writer.writeElement("FOO"));
        assertEquals("Invalid state: INVALID; output 0x002A", thrown.getMessage());
    }

    @Test
    void testWriteStartElementBinary() throws IllegalStateException, EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        ByteArrayOutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writeHeader(writer);
        writer.flush();
        stream.reset();
        writer.writeStartSegment("BIN");
        writer.writeStartElementBinary().writeEndSegment();
        writer.flush();
        assertEquals("BIN*~", stream.toString());
    }

    @Test
    void testWriteStartElementBinaryIllegal() throws IllegalStateException, EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        writer.writeStartElement();
        assertThrows(IllegalStateException.class, () -> writer.writeStartElementBinary());
    }

    @Test
    void testComponent() throws IllegalStateException, EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        writer.writeStartElement()
              .startComponent()
              .endComponent()
              .startComponent()
              .writeEndSegment();
        writer.flush();
        assertEquals("ISA*:~", stream.toString());
    }

    @Test
    void testComponentIllegal() throws IllegalStateException, EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        writer.writeStartElement();
        writer.startComponent();
        assertThrows(IllegalStateException.class, () -> writer.startComponent()); // Double
    }

    @Test
    void testWriteRepeatElement() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        ByteArrayOutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writeHeader(writer);
        writer.flush();
        stream.reset();
        writer.writeStartSegment("SEG");
        writer.writeStartElement()
              .writeElementData("R1")
              .writeRepeatElement()
              .writeElementData("R2")
              .writeEndSegment();
        writer.flush();
        assertEquals("SEG*R1^R2~", stream.toString());
    }

    @Test
    void testWriteEmptyElement() throws IllegalStateException, EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        writer.writeEmptyElement();
        writer.writeEmptyElement();
        writer.writeEmptyElement();
        writer.writeEmptyElement();
        writer.writeEndSegment();
        writer.flush();
        assertEquals("ISA****~", stream.toString());
    }

    @Test
    void testWriteEmptyComponent() throws IllegalStateException, EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        writer.writeStartElement();
        writer.writeEmptyComponent();
        writer.writeEmptyComponent();
        writer.writeEmptyComponent();
        writer.writeEmptyComponent();
        writer.writeEndSegment();
        writer.flush();
        assertEquals("ISA*:::~", stream.toString());
    }

    @Test
    void testWriteElementDataCharSequence() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        writer.writeStartElement();
        writer.writeElementData("TEST-ELEMENT");
        writer.writeEndSegment();
        writer.flush();
        assertEquals("ISA*TEST-ELEMENT~", stream.toString());
    }

    @Test
    void testWriteElementDataCharSequenceIllegal() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writeHeader(writer);
        writer.writeStartSegment("GS");
        writer.writeStartElement();
        assertThrows(IllegalArgumentException.class, () -> writer.writeElementData("** BAD^ELEMENT **"));
    }

    @Test
    void testWriteElementDataCharArray() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        writer.writeStartElement();
        writer.writeElementData(new char[] { 'C', 'H', 'A', 'R', 'S' }, 0, 5);
        writer.writeEndSegment();
        writer.flush();
        assertEquals("ISA*CHARS~", stream.toString());
    }

    @Test
    void testWriteElementDataCharInvalidBoundaries() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writeHeader(writer);
        writer.writeStartSegment("GS");
        writer.writeStartElement();
        assertThrows(IndexOutOfBoundsException.class, () -> writer.writeElementData(new char[] { 'F', 'A' }, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> writer.writeElementData(new char[] { 'F', 'A' }, 2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> writer.writeElementData(new char[] { 'F', 'A' }, 0, 3));
        assertThrows(IllegalArgumentException.class, () -> writer.writeElementData(new char[] { 'F', 'A' }, 0, -1));
    }

    @Test
    void testWriteElementDataCharArrayIllegal() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writeHeader(writer);
        writer.writeStartSegment("GS");
        writer.writeStartElement();
        assertThrows(IllegalArgumentException.class, () -> writer.writeElementData(new char[] { 'C', 'H', '~', 'R', 'S' }, 0, 5));
    }

    @Test
    void testWriteBinaryDataInputStream() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        ByteArrayOutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        byte[] binary = { '\n', 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, '\t' };
        InputStream binaryStream = new ByteArrayInputStream(binary);
        writer.startInterchange();
        writeHeader(writer);
        writer.flush();
        stream.reset();
        writer.writeStartSegment("BIN");
        writer.writeStartElement();
        writer.writeElementData("8");
        writer.endElement();
        writer.writeStartElementBinary();
        writer.writeBinaryData(binaryStream);
        writer.endElement();
        writer.writeEndSegment();
        writer.flush();
        assertEquals("BIN*8*\n\u0000\u0001\u0002\u0003\u0004\u0005\t~", stream.toString());
    }

    @Test
    void testWriteBinaryDataInputStreamIOException() throws Exception {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        ByteArrayOutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        InputStream binaryStream = Mockito.mock(InputStream.class);
        IOException ioException = new IOException();
        Mockito.when(binaryStream.read()).thenThrow(ioException);
        writer.startInterchange();
        writeHeader(writer);
        stream.reset();
        writer.writeStartSegment("BIN");
        writer.writeStartElement();
        writer.writeElementData("4");
        writer.endElement();
        writer.writeStartElementBinary();
        EDIStreamException thrown = assertThrows(EDIStreamException.class,
                                                 () -> writer.writeBinaryData(binaryStream));
        assertEquals("Exception writing binary element data in segment BIN at position 2, element 2",
                     thrown.getMessage());
        assertSame(ioException, thrown.getCause());
    }

    @Test
    void testWriteBinaryDataByteArray() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        ByteArrayOutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        byte[] binary = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A' };
        writer.startInterchange();
        writeHeader(writer);
        writer.flush();
        stream.reset();
        writer.writeStartSegment("BIN");
        writer.writeStartElement();
        writer.writeElementData("11");
        writer.endElement();
        writer.writeStartElementBinary();
        writer.writeBinaryData(binary, 0, binary.length);
        writer.endElement();
        writer.writeEndSegment();
        writer.flush();
        assertEquals("BIN*11*0123456789A~", stream.toString());
    }

    @Test
    void testWriteBinaryDataByteBuffer() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        ByteArrayOutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        byte[] binary = { 'B', 'U', 'S', 'T', 'M', 'Y', 'B', 'U', 'F', 'F', 'E', 'R', 'S', '\n' };
        ByteBuffer buffer = ByteBuffer.wrap(binary);
        writer.startInterchange();
        writeHeader(writer);
        writer.flush();
        stream.reset();
        writer.writeStartSegment("BIN");
        writer.writeStartElement();
        writer.writeElementData("14");
        writer.endElement();
        writer.writeStartElementBinary();
        writer.writeBinaryData(buffer);
        writer.endElement();
        writer.writeEndSegment();
        writer.flush();
        assertEquals("BIN*14*BUSTMYBUFFERS\n~", stream.toString());
    }

    @Test
    void testInputEquivalenceX12() throws Exception {
        EDIInputFactory inputFactory = EDIInputFactory.newFactory();
        final ByteArrayOutputStream expected = new ByteArrayOutputStream(16384);
        final InputStream delegate = getClass().getResourceAsStream("/x12/sample275_with_HL7_valid_BIN01.edi");

        InputStream source = new InputStream() {
            @Override
            public int read() throws IOException {
                int value = delegate.read();

                if (value != -1) {
                    expected.write(value);
                    System.out.write(value);
                    System.out.flush();
                    return value;
                }

                return -1;
            }
        };
        EDIStreamReader reader = inputFactory.createEDIStreamReader(source);

        EDIOutputFactory outputFactory = EDIOutputFactory.newFactory();
        outputFactory.setProperty(EDIOutputFactory.PRETTY_PRINT, true);
        ByteArrayOutputStream result = new ByteArrayOutputStream(16384);
        EDIStreamWriter writer = outputFactory.createEDIStreamWriter(result);

        EDIStreamEvent event;
        String tag = null;
        boolean composite = false;

        try {
            while (reader.hasNext()) {
                event = reader.next();

                switch (event) {
                case START_INTERCHANGE:
                    writer.startInterchange();
                    break;
                case END_INTERCHANGE:
                    writer.endInterchange();
                    break;
                case START_SEGMENT:
                    tag = reader.getText();
                    writer.writeStartSegment(tag);
                    break;
                case END_SEGMENT:
                    writer.writeEndSegment();
                    break;
                case START_COMPOSITE:
                    writer.writeStartElement();
                    composite = true;
                    break;
                case END_COMPOSITE:
                    writer.endElement();
                    composite = false;
                    break;
                case ELEMENT_DATA:
                    String text = reader.getText();

                    if ("BIN".equals(tag)) {
                        long binaryDataLength = Long.parseLong(text);
                        assertEquals(2768, binaryDataLength);
                        reader.setBinaryDataLength(binaryDataLength);
                    }

                    if (composite) {
                        writer.startComponent();
                        writer.writeElementData(text);
                        writer.endComponent();
                    } else {
                        if (reader.getLocation().getElementOccurrence() > 1) {
                            writer.writeRepeatElement();
                        } else {
                            writer.writeStartElement();
                        }
                        writer.writeElementData(text);
                        writer.endElement();
                    }
                    break;
                case ELEMENT_DATA_BINARY:
                    writer.writeStartElementBinary();
                    writer.writeBinaryData(reader.getBinaryData());
                    writer.endElement();
                    break;
                case START_TRANSACTION:
                case START_GROUP:
                case START_LOOP:
                case END_LOOP:
                case END_GROUP:
                case END_TRANSACTION:
                    // Ignored
                    break;
                default:
                    fail("Unexpected event type: " + event);
                    break;
                }
            }
        } finally {
            reader.close();
        }

        assertEquals(expected.toString().trim(), result.toString().trim());
    }

    @Test
    void testInputEquivalenceEDIFACTA() throws Exception {
        EDIInputFactory inputFactory = EDIInputFactory.newFactory();
        final ByteArrayOutputStream expected = new ByteArrayOutputStream(16384);

        InputStream source = new InputStream() {
            final InputStream delegate;
            {
                delegate = getClass().getResourceAsStream("/EDIFACT/invoic_d97b_una.edi");
            }

            @Override
            public int read() throws IOException {
                int value = delegate.read();

                if (value != -1) {
                    expected.write(value);
                    return value;
                }

                return -1;
            }
        };
        EDIStreamReader reader = inputFactory.createEDIStreamReader(source);

        EDIOutputFactory outputFactory = EDIOutputFactory.newFactory();
        outputFactory.setProperty(EDIOutputFactory.PRETTY_PRINT, true);
        ByteArrayOutputStream result = new ByteArrayOutputStream(16384);
        EDIStreamWriter writer = outputFactory.createEDIStreamWriter(result);

        EDIStreamEvent event;
        String tag = null;
        boolean composite = false;

        try {
            while (reader.hasNext()) {
                event = reader.next();

                switch (event) {
                case START_INTERCHANGE:
                    for (Map.Entry<String, Character> delim : reader.getDelimiters().entrySet()) {
                        outputFactory.setProperty(delim.getKey(), delim.getValue());
                    }
                    writer = outputFactory.createEDIStreamWriter(result);
                    writer.startInterchange();
                    break;
                case END_INTERCHANGE:
                    writer.endInterchange();
                    break;
                case START_SEGMENT:
                    tag = reader.getText();
                    writer.writeStartSegment(tag);
                    break;
                case END_SEGMENT:
                    writer.writeEndSegment();
                    break;
                case START_COMPOSITE:
                    writer.writeStartElement();
                    composite = true;
                    break;
                case END_COMPOSITE:
                    writer.endElement();
                    composite = false;
                    break;
                case ELEMENT_DATA:
                    String text = reader.getText();

                    if ("UNA".equals(tag)) {
                        continue;
                    }

                    if (composite) {
                        writer.startComponent();
                        writer.writeElementData(text);
                        writer.endComponent();
                    } else {
                        if (reader.getLocation().getElementOccurrence() > 1) {
                            writer.writeRepeatElement();
                        } else {
                            writer.writeStartElement();
                        }
                        writer.writeElementData(text);
                        writer.endElement();
                    }
                    break;
                case ELEMENT_DATA_BINARY:
                    writer.writeStartElementBinary();
                    writer.writeBinaryData(reader.getBinaryData());
                    writer.endElement();
                    break;
                case START_TRANSACTION:
                case START_GROUP:
                case START_LOOP:
                case END_LOOP:
                case END_GROUP:
                case END_TRANSACTION:
                    // Ignored
                    break;
                default:
                    fail("Unexpected event type: " + event);
                    break;
                }
            }
        } finally {
            reader.close();
        }

        assertEquals(expected.toString().trim(), result.toString().trim());
    }

    @Test
    void testInputEquivalenceEDIFACT_IATA_PNRGOV() throws Exception {
        EDIInputFactory inputFactory = EDIInputFactory.newFactory();
        inputFactory.setProperty(EDIInputFactory.EDI_VALIDATE_CONTROL_CODE_VALUES, false);
        final ByteArrayOutputStream expected = new ByteArrayOutputStream(16384);

        InputStream source = new InputStream() {
            final InputStream delegate;
            {
                delegate = getClass().getResourceAsStream("/EDIFACT/pnrgov.edi");
            }

            @Override
            public int read() throws IOException {
                int value = delegate.read();

                if (value != -1) {
                    expected.write(value);
                    return value;
                }

                return -1;
            }
        };
        EDIStreamReader reader = inputFactory.createEDIStreamReader(source);

        EDIOutputFactory outputFactory = EDIOutputFactory.newFactory();
        outputFactory.setProperty(EDIOutputFactory.PRETTY_PRINT, true);
        ByteArrayOutputStream result = new ByteArrayOutputStream(16384);
        EDIStreamWriter writer = null;

        EDIStreamEvent event;
        String tag = null;
        boolean composite = false;

        try {
            while (reader.hasNext()) {
                event = reader.next();

                switch (event) {
                case START_INTERCHANGE:
                    for (Map.Entry<String, Character> delim : reader.getDelimiters().entrySet()) {
                        outputFactory.setProperty(delim.getKey(), delim.getValue());
                    }
                    writer = outputFactory.createEDIStreamWriter(result);
                    writer.startInterchange();
                    break;
                case END_INTERCHANGE:
                    writer.endInterchange();
                    break;
                case START_SEGMENT:
                    tag = reader.getText();
                    writer.writeStartSegment(tag);
                    break;
                case END_SEGMENT:
                    writer.writeEndSegment();
                    break;
                case START_COMPOSITE:
                    writer.writeStartElement();
                    composite = true;
                    break;
                case END_COMPOSITE:
                    writer.endElement();
                    composite = false;
                    break;
                case ELEMENT_DATA:
                    String text = reader.getText();

                    if ("UNA".equals(tag)) {
                        continue;
                    }

                    if (composite) {
                        writer.startComponent();
                        writer.writeElementData(text);
                        writer.endComponent();
                    } else {
                        if (reader.getLocation().getElementOccurrence() > 1) {
                            writer.writeRepeatElement();
                        } else {
                            writer.writeStartElement();
                        }
                        writer.writeElementData(text);
                        writer.endElement();
                    }
                    break;
                case ELEMENT_DATA_BINARY:
                    writer.writeStartElementBinary();
                    writer.writeBinaryData(reader.getBinaryData());
                    writer.endElement();
                    break;
                case START_TRANSACTION:
                case START_GROUP:
                case START_LOOP:
                case END_LOOP:
                case END_GROUP:
                case END_TRANSACTION:
                    // Ignored
                    break;
                default:
                    fail("Unexpected event type: " + event);
                    break;
                }
            }
        } finally {
            reader.close();
        }

        assertEquals(expected.toString().trim(), result.toString().trim());
    }

    @Test
    void testInputEquivalenceEDIFACTB() throws Exception {
        EDIInputFactory inputFactory = EDIInputFactory.newFactory();
        final ByteArrayOutputStream expected = new ByteArrayOutputStream(16384);

        InputStream source = new InputStream() {
            final InputStream delegate;
            {
                delegate = getClass().getResourceAsStream("/EDIFACT/invoic_d97b.edi");
            }

            @Override
            public int read() throws IOException {
                int value = delegate.read();

                if (value != -1) {
                    expected.write(value);
                    return value;
                }

                return -1;
            }
        };
        EDIStreamReader reader = inputFactory.createEDIStreamReader(source);

        EDIOutputFactory outputFactory = EDIOutputFactory.newFactory();
        outputFactory.setProperty(EDIOutputFactory.PRETTY_PRINT, true);
        ByteArrayOutputStream result = new ByteArrayOutputStream(16384);
        EDIStreamWriter writer = outputFactory.createEDIStreamWriter(result);

        EDIStreamEvent event;
        String tag = null;
        boolean composite = false;

        try {
            while (reader.hasNext()) {
                event = reader.next();

                switch (event) {
                case START_INTERCHANGE:
                    writer.startInterchange();
                    break;
                case END_INTERCHANGE:
                    writer.endInterchange();
                    break;
                case START_SEGMENT:
                    tag = reader.getText();
                    writer.writeStartSegment(tag);
                    break;
                case END_SEGMENT:
                    writer.writeEndSegment();
                    break;
                case START_COMPOSITE:
                    writer.writeStartElement();
                    composite = true;
                    break;
                case END_COMPOSITE:
                    writer.endElement();
                    composite = false;
                    break;
                case ELEMENT_DATA:
                    String text = reader.getText();

                    if (composite) {
                        writer.startComponent();
                        writer.writeElementData(text);
                        writer.endComponent();
                    } else {
                        if (reader.getLocation().getElementOccurrence() > 1) {
                            writer.writeRepeatElement();
                        } else {
                            writer.writeStartElement();
                        }
                        writer.writeElementData(text);
                        writer.endElement();
                    }
                    break;
                case ELEMENT_DATA_BINARY:
                    writer.writeStartElementBinary();
                    writer.writeBinaryData(reader.getBinaryData());
                    writer.endElement();
                    break;
                case START_TRANSACTION:
                case START_GROUP:
                case START_LOOP:
                case END_LOOP:
                case END_GROUP:
                case END_TRANSACTION:
                    // Ignored
                    break;
                default:
                    fail("Unexpected event type: " + event);
                    break;
                }
            }
        } finally {
            reader.close();
        }

        System.out.println(expected.toString());
        assertEquals(expected.toString().trim(), result.toString().trim());
    }

    @Test
    void testValidatedSegmentTags() throws EDISchemaException, EDIStreamException {
        EDIOutputFactory outputFactory = EDIOutputFactory.newFactory();
        outputFactory.setProperty(EDIOutputFactory.PRETTY_PRINT, true);
        ByteArrayOutputStream result = new ByteArrayOutputStream(16384);
        EDIStreamWriter writer = outputFactory.createEDIStreamWriter(result);

        Schema control = SchemaUtils.getControlSchema("X12", new String[] { "00501" });
        writer.setControlSchema(control);

        /*SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema transaction = schemaFactory.createSchema(getClass().getResource("/x12/EDISchema997.xml"));
        writer.setTransactionSchema(transaction);*/

        writer.startInterchange();
        writeHeader(writer);
        EDIValidationException e = assertThrows(EDIValidationException.class, () -> writer.writeStartSegment("ST"));
        assertEquals(EDIStreamEvent.SEGMENT_ERROR, e.getEvent());
        assertEquals(EDIStreamValidationError.UNEXPECTED_SEGMENT, e.getError());
        assertEquals("ST", e.getData().toString());
        assertEquals(2, e.getLocation().getSegmentPosition());
    }

    @Test
    void testElementValidationThrown() throws EDISchemaException, EDIStreamException {
        EDIOutputFactory outputFactory = EDIOutputFactory.newFactory();
        outputFactory.setProperty(EDIOutputFactory.PRETTY_PRINT, true);
        ByteArrayOutputStream result = new ByteArrayOutputStream(16384);
        EDIStreamWriter writer = outputFactory.createEDIStreamWriter(result);

        Schema control = SchemaUtils.getControlSchema("X12", new String[] { "00501" });
        writer.setControlSchema(control);

        /*SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema transaction = schemaFactory.createSchema(getClass().getResource("/x12/EDISchema997.xml"));
        writer.setTransactionSchema(transaction);*/

        writer.startInterchange();
        writeHeader(writer);
        writer.writeStartSegment("GS");
        EDIValidationException e = assertThrows(EDIValidationException.class, () -> writer.writeElement("AAA"));

        assertEquals(EDIStreamEvent.ELEMENT_DATA_ERROR, e.getEvent());
        assertEquals(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG, e.getError());
        //assertEquals("AAA", e.getData().toString());
        assertEquals(2, e.getLocation().getSegmentPosition());
        assertEquals(1, e.getLocation().getElementPosition());

        assertNotNull(e = e.getNextException());
        assertEquals(EDIStreamEvent.ELEMENT_DATA_ERROR, e.getEvent());
        assertEquals(EDIStreamValidationError.INVALID_CODE_VALUE, e.getError());
        //assertEquals("AAA", e.getData().toString());
        assertEquals(2, e.getLocation().getSegmentPosition());
        assertEquals(1, e.getLocation().getElementPosition());
    }

    @Test
    void testInputEquivalenceValidatedX12() throws Exception {
        EDIInputFactory inputFactory = EDIInputFactory.newFactory();
        final ByteArrayOutputStream expected = new ByteArrayOutputStream(16384);
        Schema control = SchemaUtils.getControlSchema("X12", new String[] { "00501" });

        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema transaction = schemaFactory.createSchema(getClass().getResource("/x12/EDISchema999.xml"));
        final InputStream delegate = new BufferedInputStream(getClass().getResourceAsStream("/x12/simple999.edi"));

        InputStream source = new InputStream() {
            @Override
            public int read() throws IOException {
                int value = delegate.read();

                if (value != -1) {
                    expected.write(value);
                    System.out.write(value);
                    System.out.flush();
                    return value;
                }

                return -1;
            }
        };
        EDIStreamReader reader = inputFactory.createEDIStreamReader(source);

        EDIOutputFactory outputFactory = EDIOutputFactory.newFactory();
        outputFactory.setProperty(EDIOutputFactory.PRETTY_PRINT, true);
        ByteArrayOutputStream result = new ByteArrayOutputStream(16384);
        EDIStreamWriter writer = outputFactory.createEDIStreamWriter(result);
        writer.setControlSchema(control);
        writer.setTransactionSchema(transaction);

        EDIStreamEvent event;
        String tag = null;
        boolean composite = false;
        int componentMod = 0;
        int elementMod = 0;

        try {
            while (reader.hasNext()) {
                event = reader.next();

                switch (event) {
                case START_INTERCHANGE:
                    writer.startInterchange();
                    break;
                case END_INTERCHANGE:
                    writer.endInterchange();
                    break;
                case START_TRANSACTION:
                    reader.setTransactionSchema(transaction);
                    // Continue the loop to avoid segment position comparison for this event type
                    continue;
                case START_SEGMENT:
                    tag = reader.getText();
                    writer.writeStartSegment(tag);
                    break;
                case END_SEGMENT:
                    writer.writeEndSegment();
                    break;
                case START_COMPOSITE:
                    if (reader.getLocation().getElementOccurrence() > 1) {
                        writer.writeRepeatElement();
                    } else {
                        writer.writeStartElement();
                    }
                    composite = true;
                    break;
                case END_COMPOSITE:
                    writer.endElement();
                    composite = false;
                    break;
                case ELEMENT_DATA:
                    String text = reader.getText();

                    if (composite) {
                        if (text == null || text.isEmpty()) {
                            writer.writeEmptyComponent();
                        } else {
                            switch (++componentMod % 3) {
                            case 0:
                                writer.startComponent();
                                writer.writeElementData(text);
                                writer.endComponent();
                                break;
                            case 1:
                                writer.writeComponent(text);
                                break;
                            case 2:
                                writer.writeComponent(text.toCharArray(), 0, text.length());
                                break;
                            }
                        }
                    } else {
                        if (reader.getLocation().getElementOccurrence() > 1) {
                            writer.writeRepeatElement();
                            writer.writeElementData(text);
                            writer.endElement();
                        } else {
                            if (text == null || text.isEmpty()) {
                                writer.writeEmptyElement();
                            } else {
                                switch (++elementMod % 3) {
                                case 0:
                                    writer.writeStartElement();
                                    writer.writeElementData(text);
                                    writer.endElement();
                                    break;
                                case 1:
                                    writer.writeElement(text);
                                    break;
                                case 2:
                                    writer.writeElement(text.toCharArray(), 0, text.length());
                                    break;
                                }
                            }
                        }
                    }
                    break;
                case ELEMENT_DATA_BINARY:
                    writer.writeStartElementBinary();
                    writer.writeBinaryData(reader.getBinaryData());
                    writer.endElement();
                    break;
                case START_GROUP:
                case START_LOOP:
                case END_LOOP:
                case END_TRANSACTION:
                case END_GROUP:
                    // Ignore control loops
                    continue;
                default:
                    break;
                }

                assertEquals(reader.getLocation().getSegmentPosition(),
                             writer.getLocation().getSegmentPosition(),
                             () -> "Segment position mismatch");
                assertEquals(reader.getLocation().getElementPosition(),
                             writer.getLocation().getElementPosition(),
                             () -> "Element position mismatch");
                assertEquals(reader.getLocation().getElementOccurrence(),
                             writer.getLocation().getElementOccurrence(),
                             () -> "Element occurrence mismatch");
                assertEquals(reader.getLocation().getComponentPosition(),
                             writer.getLocation().getComponentPosition(),
                             () -> "Component position mismatch");
            }
        } finally {
            reader.close();
        }

        assertEquals(expected.toString().trim(), result.toString().trim());
    }

    @Test
    void testGetStandardNullDialect() {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(1);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> writer.getStandard());
        assertEquals("standard not accessible", thrown.getMessage());
    }

    @Test
    void testGetStandardX12() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(1);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        assertEquals(EDIStreamConstants.Standards.X12, writer.getStandard());
    }

    @Test
    void testWriteAlternateEncodedElement() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        ByteArrayOutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);

        writer.startInterchange();
        writeHeader(writer);
        writer.flush();
        stream.reset();

        writer.writeStartSegment("SEG");
        writer.writeElement("BÜTTNER")
              .writeEndSegment();
        writer.flush();
        assertEquals("SEG*BÜTTNER~", stream.toString());
    }
}
