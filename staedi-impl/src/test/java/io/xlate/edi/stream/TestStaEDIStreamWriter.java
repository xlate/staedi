package io.xlate.edi.stream;

import io.xlate.edi.stream.EDIStreamConstants.Events;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Map;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("static-method")
public class TestStaEDIStreamWriter {

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
	public void testGetProperty() throws EDIStreamException {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		factory.setProperty(EDIStreamConstants.Delimiters.SEGMENT, '~');
		OutputStream stream = new ByteArrayOutputStream(4096);
		EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
		Object segmentTerminator = writer.getProperty(EDIStreamConstants.Delimiters.SEGMENT);
		Assert.assertEquals(Character.valueOf('~'), segmentTerminator);
	}

	@Test
	public void testStartInterchange() throws EDIStreamException {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		OutputStream stream = new ByteArrayOutputStream(4096);
		EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
		writer.startInterchange();
	}

	@Test(expected = IllegalStateException.class)
	public void testStartInterchangeIllegalX12() throws EDIStreamException {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		OutputStream stream = new ByteArrayOutputStream(4096);
		EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
		writer.startInterchange();
		writer.writeStartSegment("ISA");
		writer.startInterchange();
	}

	@Test(expected = IllegalStateException.class)
	public void testStartInterchangeIllegalEDIFACTA() throws EDIStreamException {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		OutputStream stream = new ByteArrayOutputStream(4096);
		EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
		writer.startInterchange();
		writer.writeStartSegment("UNA");
		writer.startInterchange();
	}

	@Test(expected = IllegalStateException.class)
	public void testStartInterchangeIllegalEDIFACTB() throws EDIStreamException {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		OutputStream stream = new ByteArrayOutputStream(4096);
		EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
		writer.startInterchange();
		writer.writeStartSegment("UNB");
		writer.startInterchange();
	}

	@Test
	public void testEndInterchange() throws EDIStreamException {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		OutputStream stream = new ByteArrayOutputStream(4096);
		EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
		writer.startInterchange();
		writer.endInterchange();
	}

	@Test(expected = IllegalStateException.class)
	public void testEndInterchangeIllegal() throws EDIStreamException {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		OutputStream stream = new ByteArrayOutputStream(4096);
		EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
		writer.endInterchange();
	}

	@Test
	public void testWriteStartSegment() throws EDIStreamException {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		OutputStream stream = new ByteArrayOutputStream(4096);
		EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
		writer.startInterchange();
		writer.writeStartSegment("ISA");
		Assert.assertEquals("ISA", stream.toString());
	}

	@Test(expected = IllegalStateException.class)
	public void testWriteStartSegmentIllegal() throws EDIStreamException {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		OutputStream stream = new ByteArrayOutputStream(4096);
		EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
		writer.startInterchange();
		writer.writeStartSegment("ISA");
		writer.writeStartElement();
		writer.writeStartSegment("GS");
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
		Assert.assertEquals("ISA*E1~", stream.toString());
	}

	@Test(expected = IllegalStateException.class)
	public void testWriteEndSegmentIllegal() throws EDIStreamException {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		OutputStream stream = new ByteArrayOutputStream(4096);
		EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
		writer.startInterchange();
		writer.writeEndSegment();
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
		Assert.assertEquals("ISA****", stream.toString());
	}

	@Test(expected = IllegalStateException.class)
	public void testWriteStartElementIllegal() throws EDIStreamException {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		OutputStream stream = new ByteArrayOutputStream(4096);
		EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
		writer.startInterchange();
		writer.writeStartSegment("ISA");
		writer.writeStartElement()
			.startComponent()
			.writeStartElement();
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
		Assert.assertEquals("BIN*~", stream.toString());
	}

	@Test(expected = IllegalStateException.class)
	public void testWriteStartElementBinaryIllegal() throws IllegalStateException, EDIStreamException {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		OutputStream stream = new ByteArrayOutputStream(4096);
		EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
		writer.startInterchange();
		writer.writeStartSegment("ISA");
		writer.writeStartElement().writeStartElementBinary().writeEndSegment();
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
		Assert.assertEquals("ISA*:~", stream.toString());
	}

	@Test(expected = IllegalStateException.class)
	public void testComponentIllegal() throws IllegalStateException, EDIStreamException {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		OutputStream stream = new ByteArrayOutputStream(4096);
		EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
		writer.startInterchange();
		writer.writeStartSegment("ISA");
		writer.writeStartElement()
			.startComponent()
			.startComponent() // Double
			.writeEndSegment();
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
		Assert.assertEquals("SEG*R1^R2~", stream.toString());
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
		Assert.assertEquals("ISA****~", stream.toString());
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
		Assert.assertEquals("ISA*:::~", stream.toString());
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
		Assert.assertEquals("ISA*TEST-ELEMENT~", stream.toString());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWriteElementDataCharSequenceIllegal() throws EDIStreamException {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		OutputStream stream = new ByteArrayOutputStream(4096);
		EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
		writer.startInterchange();
		writeHeader(writer);
		writer.writeStartSegment("GS");
		writer.writeStartElement();
		writer.writeElementData("** BAD^ELEMENT **");
		writer.writeEndSegment();
	}

	@Test
	public void testWriteElementDataCharArray() throws EDIStreamException {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		OutputStream stream = new ByteArrayOutputStream(4096);
		EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
		writer.startInterchange();
		writer.writeStartSegment("ISA");
		writer.writeStartElement();
		writer.writeElementData(new char[] {'C', 'H', 'A', 'R', 'S'}, 0, 5);
		writer.writeEndSegment();
		Assert.assertEquals("ISA*CHARS~", stream.toString());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWriteElementDataCharArrayIllegal() throws EDIStreamException {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		OutputStream stream = new ByteArrayOutputStream(4096);
		EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
		writer.startInterchange();
		writeHeader(writer);
		writer.writeStartSegment("GS");
		writer.writeStartElement();
		writer.writeElementData(new char[] {'C', 'H', '~', 'R', 'S'}, 0, 5);
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
		Assert.assertEquals("BIN*8*\n\u0000\u0001\u0002\u0003\u0004\u0005\t~", stream.toString());
	}

	@Test
	public void testWriteBinaryDataByteArray() throws EDIStreamException {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		ByteArrayOutputStream stream = new ByteArrayOutputStream(4096);
		EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
		byte[] binary = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A' };
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
		Assert.assertEquals("BIN*11*0123456789A~", stream.toString());
	}

	@Test
	public void testWriteBinaryDataByteBuffer() throws EDIStreamException {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		ByteArrayOutputStream stream = new ByteArrayOutputStream(4096);
		EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
		byte[] binary = {'B', 'U', 'S', 'T', 'M', 'Y', 'B', 'U', 'F', 'F', 'E', 'R', 'S', '\n' };
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
		Assert.assertEquals("BIN*14*BUSTMYBUFFERS\n~", stream.toString());
	}

	@Test
	public void testInputEquivalenceX12() throws Exception {
		EDIInputFactory inputFactory = EDIInputFactory.newFactory();
		final ByteArrayOutputStream expected = new ByteArrayOutputStream(16384);
		@SuppressWarnings("resource")
		InputStream source = new InputStream() {
			final InputStream delegate;
			{
				delegate = getClass().getClassLoader().getResourceAsStream(
						"x12/sample275_with_HL7_valid_BIN01.edi");
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

		int event;
		String tag = null;
		boolean composite = false;

		try {
			while (reader.hasNext()) {
				event = reader.next();

				switch (event) {
				case Events.START_INTERCHANGE:
					writer.startInterchange();
					break;
				case Events.END_INTERCHANGE:
					writer.endInterchange();
					break;
				case Events.START_SEGMENT:
					tag = reader.getText();
					writer.writeStartSegment(tag);
					break;
				case Events.END_SEGMENT:
					writer.writeEndSegment();
					break;
				case Events.START_COMPOSITE:
					writer.writeStartElement();
					composite = true;
					break;
				case Events.END_COMPOSITE:
					writer.endElement();
					composite = false;
					break;
				case Events.ELEMENT_DATA:
					String text = reader.getText();

					if ("BIN".equals(tag)) {
						long binaryDataLength = Long.parseLong(text);
						Assert.assertEquals(2768, binaryDataLength);
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
				case Events.ELEMENT_DATA_BINARY:
					writer.writeStartElementBinary();
					writer.writeBinaryData(reader.getBinaryData());
					writer.endElement();
					break;
				}
			}
		} finally {
			reader.close();
		}

		Assert.assertEquals(expected.toString().trim(), result.toString().trim());
	}

	@Test
	public void testInputEquivalenceEDIFACTA() throws Exception {
		EDIInputFactory inputFactory = EDIInputFactory.newFactory();
		final ByteArrayOutputStream expected = new ByteArrayOutputStream(16384);
		@SuppressWarnings("resource")
		InputStream source = new InputStream() {
			final InputStream delegate;
			{
				delegate = getClass().getClassLoader().getResourceAsStream(
						"EDIFACT/invoic_d97b_una.edi");
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

		int event;
		String tag = null;
		boolean composite = false;

		try {
			while (reader.hasNext()) {
				event = reader.next();

				switch (event) {
				case Events.START_INTERCHANGE:
					for (Map.Entry<String, Character> delim : reader.getDelimiters().entrySet()) {
						outputFactory.setProperty(delim.getKey(), delim.getValue());
					}
					writer = outputFactory.createEDIStreamWriter(result);
					writer.startInterchange();
					break;
				case Events.END_INTERCHANGE:
					writer.endInterchange();
					break;
				case Events.START_SEGMENT:
					tag = reader.getText();
					writer.writeStartSegment(tag);
					break;
				case Events.END_SEGMENT:
					writer.writeEndSegment();
					break;
				case Events.START_COMPOSITE:
					writer.writeStartElement();
					composite = true;
					break;
				case Events.END_COMPOSITE:
					writer.endElement();
					composite = false;
					break;
				case Events.ELEMENT_DATA:
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
				case Events.ELEMENT_DATA_BINARY:
					writer.writeStartElementBinary();
					writer.writeBinaryData(reader.getBinaryData());
					writer.endElement();
					break;
				}
			}
		} finally {
			reader.close();
		}

		Assert.assertEquals(expected.toString().trim(), result.toString().trim());
	}

	@Test
	public void testInputEquivalenceEDIFACTB() throws Exception {
		EDIInputFactory inputFactory = EDIInputFactory.newFactory();
		final ByteArrayOutputStream expected = new ByteArrayOutputStream(16384);
		@SuppressWarnings("resource")
		InputStream source = new InputStream() {
			final InputStream delegate;
			{
				delegate = getClass().getClassLoader().getResourceAsStream(
						"EDIFACT/invoic_d97b.edi");
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

		int event;
		String tag = null;
		boolean composite = false;

		try {
			while (reader.hasNext()) {
				event = reader.next();

				switch (event) {
				case Events.START_INTERCHANGE:
					writer.startInterchange();
					break;
				case Events.END_INTERCHANGE:
					writer.endInterchange();
					break;
				case Events.START_SEGMENT:
					tag = reader.getText();
					writer.writeStartSegment(tag);
					break;
				case Events.END_SEGMENT:
					writer.writeEndSegment();
					break;
				case Events.START_COMPOSITE:
					writer.writeStartElement();
					composite = true;
					break;
				case Events.END_COMPOSITE:
					writer.endElement();
					composite = false;
					break;
				case Events.ELEMENT_DATA:
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
				case Events.ELEMENT_DATA_BINARY:
					writer.writeStartElementBinary();
					writer.writeBinaryData(reader.getBinaryData());
					writer.endElement();
					break;
				}
			}
		} finally {
			reader.close();
		}

		Assert.assertEquals(expected.toString().trim(), result.toString().trim());
	}

}
