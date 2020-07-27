package io.xlate.edi.internal.wiki;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamConstants;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamWriter;

public class WriteEdifactExampleTest {

    @Test
    void testEdifactExample() throws EDIStreamException, IOException {
        // (1) Create an EDIOutputFactory
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        int messageCount = 0; // Counter for the UNH/UNT messages contained in the interchange
        int messageSegmentCount = 0; // Counter for the segments in a UNH/UNT message

        // (2) Optionally specify delimiters - here the given values are the same as default
        factory.setProperty(EDIStreamConstants.Delimiters.SEGMENT, '\'');
        factory.setProperty(EDIStreamConstants.Delimiters.DATA_ELEMENT, '+');
        factory.setProperty(EDIStreamConstants.Delimiters.COMPONENT_ELEMENT, ':');
        factory.setProperty(EDIStreamConstants.Delimiters.RELEASE, '?');
        factory.setProperty(EDIStreamConstants.Delimiters.REPETITION, ' ');

        // Write each segment on a new line (optional)
        // factory.setProperty(EDIOutputFactory.PRETTY_PRINT, true);

        // (3) Create an EDIStreamWriter. Any OutputStream may be used - here we are writing to a file
        OutputStream stream = new FileOutputStream("target/edifact.out");
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);

        // (4) Start the interchange
        writer.startInterchange();

        // Optionally write a UNA segment. When delimiters are specified via the `EDIOutputFactory`'s
        // properties, a UNA segment will be automatically written.
        writer.writeStartSegment("UNA").writeEndSegment();

        // (5) Write the beginning segment for the interchange.
        writer.writeStartSegment("UNB");

        // Writing composite elements is done by first starting the element,
        // then writing the components. Finally, the element is ended.
        writer.writeStartElement()
              .writeComponent("UNOC")
              .writeComponent("3")
              .endElement();

        writer.writeStartElement()
              .writeComponent("0123456789012")
              .writeComponent("14")
              .endElement();

        writer.writeStartElement()
              .writeComponent("0123456789012")
              .writeComponent("14")
              .endElement();

        writer.writeStartElement()
              .writeComponent("200702")
              .writeComponent("0734")
              .endElement();

        // A simple data element may be written with a single method call.
        writer.writeElement("00000563");

        // Complete the UNB segment
        writer.writeEndSegment();

        // (6) Write the message header segment
        writer.writeStartSegment("UNH");
        // Keep track of the message count for the UNZ trailer segment
        messageCount++;
        // Keep track of the segment count for this message
        messageSegmentCount++;

        writer.writeElement("0001");

        writer.writeStartElement()
              .writeComponent("INVOIC")
              .writeComponent("D")
              .writeComponent("96A")
              .writeComponent("UN")
              .writeComponent("EAN008")
              .endElement();

        writer.writeEndSegment();

        // Begin Message BGM
        writer.writeStartSegment("BGM");
        messageSegmentCount++;
        writer.writeElement("380");
        writer.writeElement("1676245");
        writer.writeElement("9");
        writer.writeEndSegment();

        // From Here do further INVOIC related segments like DTM and so on

        // (7) Write the message trailer segment
        writer.writeStartSegment("UNT")
              .writeElement(String.valueOf(++messageSegmentCount))
              .writeElement("0001")
              .writeEndSegment();

        // (8) Write the message trailer segment
        writer.writeStartSegment("UNZ")
              .writeElement(String.valueOf(messageCount))
              .writeElement("00000563")
              .writeEndSegment();

        // (9) End the interchange
        writer.endInterchange();

        // (10) Close the EDIStreamWriter. This must be done to ensure the output is flushed and written
        writer.close();

        assertEquals(""
                + "UNA:+.? '"
                + "UNB+UNOC:3+0123456789012:14+0123456789012:14+200702:0734+00000563'"
                + "UNH+0001+INVOIC:D:96A:UN:EAN008'"
                + "BGM+380+1676245+9'"
                + "UNT+3+0001'"
                + "UNZ+1+00000563'",
                String.join("", Files.readAllLines(Paths.get("target/edifact.out"))));
    }
}
