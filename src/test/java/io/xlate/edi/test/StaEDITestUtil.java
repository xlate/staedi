package io.xlate.edi.test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamFilter;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIStreamWriter;

public class StaEDITestUtil {

    /**
     * Write a full segment to the output writer. Elements in the elements parameter
     * are written as simple elements or composite elements when they are an array.
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

    public static String[] getJavaVersion() {
        String versionString = System.getProperty("java.version");
        return versionString.split("[\\._]");
    }
}
