package io.xlate.edi.internal.wiki;

import org.junit.jupiter.api.Test;

import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamConstants;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamWriter;

public class WriteInterchangeAcknowledgementTest {

    @Test
    public void testAcknowledgementWrite() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        // Optionally specify delimiters - here the given values are the same as default
        factory.setProperty(EDIStreamConstants.Delimiters.SEGMENT, '~');
        factory.setProperty(EDIStreamConstants.Delimiters.DATA_ELEMENT, '*');
        factory.setProperty(EDIOutputFactory.PRETTY_PRINT, true);
        EDIStreamWriter writer = factory.createEDIStreamWriter(System.out);

        writer.startInterchange();

        writer.writeStartSegment("ISA")
              .writeElement("00")
              .writeElement("          ")
              .writeElement("00")
              .writeElement("          ")
              .writeElement("ZZ")
              .writeElement("ReceiverID     ")
              .writeElement("ZZ")
              .writeElement("Sender         ")
              .writeElement("203001")
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
    }
}
