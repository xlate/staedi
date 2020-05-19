package io.xlate.edi.internal.wiki;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamConstants;
import io.xlate.edi.stream.EDIStreamWriter;

class WriteInterchangeAcknowledgementTest {

    @Test
    void testAcknowledgementWrite() throws Exception {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        // Optionally specify delimiters - here the given values are the same as default
        factory.setProperty(EDIStreamConstants.Delimiters.SEGMENT, '~');
        factory.setProperty(EDIStreamConstants.Delimiters.DATA_ELEMENT, '*');
        factory.setProperty(EDIOutputFactory.PRETTY_PRINT, true);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);

        writer.startInterchange();

        writer.writeStartSegment("ISA")
              .writeElement("00")
              .writeElement("          ")
              .writeElement("00")
              .writeElement("          ")
              .writeElement("ZZ")
              .writeElement("Receiver       ")
              .writeElement("ZZ")
              .writeElement("Sender         ")
              .writeElement("200301")
              .writeElement("1430")
              .writeElement("^")
              .writeElement("00501")
              .writeElement("000000001")
              .writeElement("0")
              .writeElement("P")
              .writeElement(":")
              .writeEndSegment();

        writer.writeStartSegment("TA1")
              .writeElement("000000050")
              .writeElement("200229")
              .writeElement("1200")
              .writeElement("A")
              .writeElement("000")
              .writeEndSegment();

        writer.writeStartSegment("IEA")
              .writeElement("1")
              .writeElement("000000001")
              .writeEndSegment();

        writer.endInterchange();

        writer.close();

        assertEquals(new String(Files.readAllBytes(Paths.get("./src/test/resources/wiki/x12_interchange_ack.txt"))),
                     stream.toString().trim());
    }
}
