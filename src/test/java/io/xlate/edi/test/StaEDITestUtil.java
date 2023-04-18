package io.xlate.edi.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;
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

    public static void assertLocation(EDIStreamReader reader, int segPos, int elePos, int eleOcc, int cmpPos)
            throws EDIStreamException {
        assertEquals(segPos, reader.getLocation().getSegmentPosition());
        assertEquals(elePos, reader.getLocation().getElementPosition());
        assertEquals(eleOcc, reader.getLocation().getElementOccurrence());
        assertEquals(cmpPos, reader.getLocation().getComponentPosition());
    }

    public static void assertTextLocation(EDIStreamReader reader, String text, int segPos, int elePos, int eleOcc, int cmpPos)
            throws EDIStreamException {
        assertEquals(text, reader.getText());
        assertEquals(segPos, reader.getLocation().getSegmentPosition());
        assertEquals(elePos, reader.getLocation().getElementPosition());
        assertEquals(eleOcc, reader.getLocation().getElementOccurrence());
        assertEquals(cmpPos, reader.getLocation().getComponentPosition());
    }

    public static String[] getJavaVersion() {
        String versionString = System.getProperty("java.version");
        return versionString.split("[\\._]");
    }

    public static String toString(EDIStreamReader reader) {
        StringBuilder buffer = new StringBuilder(reader.getClass().getName());
        buffer.append('(');

        EDIStreamEvent event = reader.getEventType();
        buffer.append("event=").append(event);

        switch (event) {
        case SEGMENT_ERROR:
        case ELEMENT_DATA_ERROR:
        case ELEMENT_OCCURRENCE_ERROR:
            buffer.append(", ").append("error=").append(reader.getErrorType());
            buffer.append(", ").append("referenceCode=").append(reader.getReferenceCode());
            break;
        case ELEMENT_DATA:
            buffer.append(", ").append("text=").append(reader.getText());
            break;
        default:
            break;
        }

        buffer.append(", ").append("location=").append(reader.getLocation());

        return buffer.append(')').toString();
    }

    public static void printTransaction(String resourcePath, String schemaPath) throws Exception {
        EDIInputFactory factory = EDIInputFactory.newFactory();

        // Any InputStream can be used to create an `EDIStreamReader`
        try (InputStream stream = StaEDITestUtil.class.getResourceAsStream(resourcePath);
                EDIStreamReader reader = factory.createEDIStreamReader(stream)) {
            EDIStreamEvent event;
            boolean transactionBeginSegment = false;
            String comp = null;
            int depth = 0;
            StringBuilder buffer = new StringBuilder();

            while (reader.hasNext()) {
                event = reader.next();

                switch (event) {
                case START_INTERCHANGE:
                    System.out.println(repeat(' ', depth) + "<Interchange>");
                    depth++;
                    break;

                case END_INTERCHANGE:
                    depth--;
                    System.out.println(repeat(' ', depth) + "</Interchange>");
                    break;

                case START_GROUP:
                    System.out.println(repeat(' ', depth) + "<FunctionalGroup>");
                    depth++;
                    break;

                case END_GROUP:
                    depth--;
                    System.out.println(repeat(' ', depth) + "</FunctionalGroup>");
                    break;

                case START_TRANSACTION:
                    transactionBeginSegment = true;
                    System.out.println(repeat(' ', depth) + "<TransactionSet>");
                    depth++;
                    break;

                case END_TRANSACTION:
                    depth--;
                    System.out.println(repeat(' ', depth) + "</TransactionSet>");
                    break;

                case START_LOOP:
                    System.out.println(repeat(' ', depth) + "<" + reader.getReferenceCode() + ">");
                    depth++;
                    break;
                case END_LOOP:
                    depth--;
                    System.out.println(repeat(' ', depth) + "</" + reader.getReferenceCode() + ">");
                    break;

                case START_SEGMENT:
                    buffer.setLength(0);
                    buffer.append(repeat(' ', depth));
                    buffer.append("<Segment-");
                    buffer.append(reader.getText());
                    if (reader.getReferenceCode() != null) {
                        buffer.append(" code='");
                        buffer.append(reader.getReferenceCode());
                        buffer.append("'");
                    }
                    buffer.append(">");

                    System.out.println(buffer.toString());
                    depth++;
                    break;

                case END_SEGMENT:
                    if (transactionBeginSegment) {
                        SchemaFactory schemaFactory = SchemaFactory.newFactory();
                        URL schemaUrl = StaEDITestUtil.class.getResource(schemaPath);
                        //schemaFactory.setProperty(SchemaFactory.SCHEMA_LOCATION_URL_CONTEXT, schemaUrl);
                        Schema schema = schemaFactory.createSchema(schemaUrl);
                        reader.setTransactionSchema(schema);
                    }
                    transactionBeginSegment = false;
                    depth--;
                    System.out.println(repeat(' ', depth) + "</Segment-" + reader.getText() + ">");
                    break;

                case START_COMPOSITE:
                    System.out.println(repeat(' ', depth) + "<" + reader.getReferenceCode() + ">");
                    comp = reader.getReferenceCode();
                    depth++;
                    break;
                case END_COMPOSITE:
                    depth--;
                    System.out.println(repeat(' ', depth) + "</" + comp + ">");
                    comp = null;
                    break;

                case ELEMENT_DATA:
                    String name = reader.getReferenceCode();
                    if (name != null && !name.contains("Element")) {
                        name = "Element-" + name;
                    }
                    if (null != reader.getText() && !"".equals(reader.getText())) {
                        System.out.println(repeat(' ', depth) + "<" + name + ">" + reader.getText() + "</" + name + ">");
                        if ("null".equals(reader.getReferenceCode())) {
                            System.out.println(repeat(' ', depth) + "<" + name + ">" + reader.getText() + "</" + name + ">");
                        }
                    }

                    break;

                case SEGMENT_ERROR:
                    // Handle a segment error
                    EDIStreamValidationError segmentErrorType = reader.getErrorType();
                    System.out.println(
                        "*" + repeat(' ', depth) + "** Segment error: " + segmentErrorType.name() + " " + reader.getLocation());
                    break;

                case ELEMENT_OCCURRENCE_ERROR:
                case ELEMENT_DATA_ERROR:
                    // Handle a segment error
                    EDIStreamValidationError elementErrorType = reader.getErrorType();
                    System.out.println(
                        "*" + repeat(' ', depth) + "** Element error:" + elementErrorType.name() + ", " + reader.getReferenceCode());
                    break;

                default:
                    break;
                }
            }
        }
    }

    static String repeat(char value, int times) {
        StringBuilder buffer = new StringBuilder(times);
        for (int i = 0; i < times; i++) {
            buffer.append("    ");
        }
        return buffer.toString();
    }

    public static void assertEqualsNormalizeLineSeparators(String expected, String actual) {
        assertEquals(normalizeLines(expected), normalizeLines(actual));
    }

    public static String normalizeLines(String input) {
        return input != null ? input.replaceAll("(?:\\r\\n|\\r)", "\n") : null;
    }
}
