package io.xlate.edi.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamFilter;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.EDIStreamWriter;

public class StaEDITestUtil {

    /**
     * Write a full segment to the output writer. Elements in the elements
     * parameter are written as simple elements or composite elements when they
     * are an array.
     *
     * @param writer
     * @param segmentTag
     * @param elements
     * @throws EDIStreamException
     */
    public static void write(EDIStreamWriter writer, String segmentTag, Object... elements) throws EDIStreamException {
        writer.writeStartSegment(segmentTag);

        for (Object e : elements) {
            if (e instanceof String) {
                writer.writeElement((String) e);
            } else if (e instanceof String[]) {
                writer.writeStartElement();
                for (String c : (String[]) e) {
                    writer.writeComponent(c);
                }
                writer.endElement();
            }
        }

        writer.writeEndSegment();
    }

    public static EDIStreamReader filterEvents(EDIInputFactory factory, EDIStreamReader reader, EDIStreamEvent... events) {
        final Set<EDIStreamEvent> filteredEvents = new HashSet<>(Arrays.asList(events));
        final EDIStreamFilter filter = new EDIStreamFilter() {
            @Override
            public boolean accept(EDIStreamReader reader) {
                return filteredEvents.contains(reader.getEventType());
            }
        };
        return factory.createFilteredReader(reader, filter);
    }

    public static void assertEvent(EDIStreamReader reader, EDIStreamEvent event) throws EDIStreamException {
        assertEquals(event, reader.next());
    }

    public static void assertEvent(EDIStreamReader reader, EDIStreamEvent event, String referenceCode)
            throws EDIStreamException {
        assertEvent(reader, event, null, referenceCode);
    }

    public static void assertEvent(EDIStreamReader reader, EDIStreamEvent event, EDIStreamValidationError error, String referenceCode)
            throws EDIStreamException {
        assertEquals(event, reader.next());
        if (error != null) {
            assertEquals(error, reader.getErrorType(), () -> "Unexpected error type for code: " + reader.getReferenceCode());
        }
        assertEquals(referenceCode, reader.getReferenceCode());
    }

    public static void assertEvent(EDIStreamReader reader, EDIStreamEvent event, EDIStreamValidationError error, String text, String referenceCode)
            throws EDIStreamException {
        assertEquals(event, reader.next());
        if (error != null) {
            assertEquals(error, reader.getErrorType(), () -> "Unexpected error type for code: " + reader.getReferenceCode());
        }
        assertEquals(text, reader.getText());
        assertEquals(referenceCode, reader.getReferenceCode());
    }

    public static String[] getJavaVersion() {
        String versionString = System.getProperty("java.version");
        return versionString.split("[\\._]");
    }
}
