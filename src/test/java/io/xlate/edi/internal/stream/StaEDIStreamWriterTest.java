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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamConstants;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIStreamWriter;

@SuppressWarnings("resource")
public class StaEDIStreamWriterTest {

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
    public void testGetProperty() {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        factory.setProperty(EDIStreamConstants.Delimiters.SEGMENT, '~');
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        Object segmentTerminator = writer.getProperty(EDIStreamConstants.Delimiters.SEGMENT);
        assertEquals(Character.valueOf('~'), segmentTerminator);
    }

    @Test
    public void testStartInterchange() {
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
    public void testStartInterchangeIllegalX12() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        assertThrows(IllegalStateException.class, () -> writer.startInterchange());
    }

    @Test
    public void testStartInterchangeIllegalEDIFACTA() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("UNA");
        assertThrows(IllegalStateException.class, () -> writer.startInterchange());
    }

    @Test
    public void testStartInterchangeIllegalEDIFACTB() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("UNB");
        assertThrows(IllegalStateException.class, () -> writer.startInterchange());
    }

    @Test
    public void testEndInterchange() {
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
    public void testEndInterchangeIllegal() {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        assertThrows(IllegalStateException.class, () -> writer.endInterchange());
    }

    @Test
    public void testWriteStartSegment() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        assertEquals("ISA", stream.toString());
    }

    @Test
    public void testWriteStartSegmentIllegal() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        writer.writeStartElement();
        assertThrows(IllegalStateException.class, () -> writer.writeStartSegment("GS"));
    }

    @Test
    public void testWriteEndSegment() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        writer.writeStartElement().writeElementData("E1").endElement();
        writer.writeEndSegment();
        assertEquals("ISA*E1~", stream.toString());
    }

    @Test
    public void testWriteEndSegmentIllegal() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        assertThrows(IllegalStateException.class, () -> writer.writeEndSegment());
    }

    @Test
    public void testWriteStartElement() throws EDIStreamException {
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
        assertEquals("ISA****", stream.toString());
    }

    @Test
    public void testWriteStartElementIllegal() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        assertThrows(IllegalStateException.class, () -> writer.writeStartElement()
                     .startComponent()
                     .writeStartElement());
    }

    @Test
    public void testWriteStartElementBinary() throws IllegalStateException, EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        ByteArrayOutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writeHeader(writer);
        stream.reset();
        writer.writeStartSegment("BIN");
        writer.writeStartElementBinary().writeEndSegment();
        assertEquals("BIN*~", stream.toString());
    }

    @Test
    public void testWriteStartElementBinaryIllegal() throws IllegalStateException, EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        assertThrows(IllegalStateException.class, () -> writer.writeStartElement().writeStartElementBinary().writeEndSegment());
    }

    @Test
    public void testComponent() throws IllegalStateException, EDIStreamException {
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
        assertEquals("ISA*:~", stream.toString());
    }

    @Test
    public void testComponentIllegal() throws IllegalStateException, EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        assertThrows(IllegalStateException.class, () -> writer.writeStartElement()
              .startComponent()
              .startComponent() // Double
              .writeEndSegment());
    }

    @Test
    public void testWriteRepeatElement() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        ByteArrayOutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writeHeader(writer);
        stream.reset();
        writer.writeStartSegment("SEG");
        writer.writeStartElement()
              .writeElementData("R1")
              .writeRepeatElement()
              .writeElementData("R2")
              .writeEndSegment();
        assertEquals("SEG*R1^R2~", stream.toString());
    }

    @Test
    public void testWriteEmptyElement() throws IllegalStateException, EDIStreamException {
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
        assertEquals("ISA****~", stream.toString());
    }

    @Test
    public void testWriteEmptyComponent() throws IllegalStateException, EDIStreamException {
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
        assertEquals("ISA*:::~", stream.toString());
    }

    @Test
    public void testWriteElementDataCharSequence() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        writer.writeStartElement();
        writer.writeElementData("TEST-ELEMENT");
        writer.writeEndSegment();
        assertEquals("ISA*TEST-ELEMENT~", stream.toString());
    }

    @Test
    public void testWriteElementDataCharSequenceIllegal() throws EDIStreamException {
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
    public void testWriteElementDataCharArray() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        writer.startInterchange();
        writer.writeStartSegment("ISA");
        writer.writeStartElement();
        writer.writeElementData(new char[] { 'C', 'H', 'A', 'R', 'S' }, 0, 5);
        writer.writeEndSegment();
        assertEquals("ISA*CHARS~", stream.toString());
    }

    @Test
    public void testWriteElementDataCharArrayIllegal() throws EDIStreamException {
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
    public void testWriteBinaryDataInputStream() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        ByteArrayOutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        byte[] binary = { '\n', 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, '\t' };
        InputStream binaryStream = new ByteArrayInputStream(binary);
        writer.startInterchange();
        writeHeader(writer);
        stream.reset();
        writer.writeStartSegment("BIN");
        writer.writeStartElement();
        writer.writeElementData("8");
        writer.endElement();
        writer.writeStartElementBinary();
        writer.writeBinaryData(binaryStream);
        writer.endElement();
        writer.writeEndSegment();
        assertEquals("BIN*8*\n\u0000\u0001\u0002\u0003\u0004\u0005\t~", stream.toString());
    }

    @Test
    public void testWriteBinaryDataByteArray() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        ByteArrayOutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        byte[] binary = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A' };
        writer.startInterchange();
        writeHeader(writer);
        stream.reset();
        writer.writeStartSegment("BIN");
        writer.writeStartElement();
        writer.writeElementData("11");
        writer.endElement();
        writer.writeStartElementBinary();
        writer.writeBinaryData(binary, 0, binary.length);
        writer.endElement();
        writer.writeEndSegment();
        assertEquals("BIN*11*0123456789A~", stream.toString());
    }

    @Test
    public void testWriteBinaryDataByteBuffer() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        ByteArrayOutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        byte[] binary = { 'B', 'U', 'S', 'T', 'M', 'Y', 'B', 'U', 'F', 'F', 'E', 'R', 'S', '\n' };
        ByteBuffer buffer = ByteBuffer.wrap(binary);
        writer.startInterchange();
        writeHeader(writer);
        stream.reset();
        writer.writeStartSegment("BIN");
        writer.writeStartElement();
        writer.writeElementData("14");
        writer.endElement();
        writer.writeStartElementBinary();
        writer.writeBinaryData(buffer);
        writer.endElement();
        writer.writeEndSegment();
        assertEquals("BIN*14*BUSTMYBUFFERS\n~", stream.toString());
    }

    @Test
    public void testInputEquivalenceX12() throws Exception {
        EDIInputFactory inputFactory = EDIInputFactory.newFactory();
        final ByteArrayOutputStream expected = new ByteArrayOutputStream(16384);

        InputStream source = new InputStream() {
            final InputStream delegate;
            {
                delegate = getClass().getResourceAsStream("/x12/sample275_with_HL7_valid_BIN01.edi");
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
        outputFactory.setProperty(StaEDIOutputFactory.PRETTY_PRINT, true);
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
                default:
                    break;
                }
            }
        } finally {
            reader.close();
        }

        assertEquals(expected.toString().trim(), result.toString().trim());
    }

    @Test
    public void testInputEquivalenceEDIFACTA() throws Exception {
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
        outputFactory.setProperty(StaEDIOutputFactory.PRETTY_PRINT, true);
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
                default:
                    break;
                }
            }
        } finally {
            reader.close();
        }

        assertEquals(expected.toString().trim(), result.toString().trim());
    }

    @Test
    public void testInputEquivalenceEDIFACTB() throws Exception {
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
        outputFactory.setProperty(StaEDIOutputFactory.PRETTY_PRINT, true);
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
                default:
                    break;
                }
            }
        } finally {
            reader.close();
        }

        assertEquals(expected.toString().trim(), result.toString().trim());
    }

}
