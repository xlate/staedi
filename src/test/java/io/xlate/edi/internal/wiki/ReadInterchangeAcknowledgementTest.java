package io.xlate.edi.internal.wiki;

import static org.junit.jupiter.api.Assertions.assertTrue;

// import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamReader;

class ReadInterchangeAcknowledgementTest {

    @Test
    void testAcknowledgementSuccess() throws Exception {
        assertTrue(isAcknowledgementSuccess());
    }

    private boolean isAcknowledgementSuccess() throws Exception {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        String ta104 = null;
        String ta105 = null;

        //InputStream stream = new FileInputStream("x12_interchange_ack.txt");
        try (InputStream stream = getClass().getResource("/wiki/x12_interchange_ack.txt").openStream();
             EDIStreamReader reader = factory.createEDIStreamReader(stream)) {

            EDIStreamEvent event;
            String segment = null;

            while (reader.hasNext()) {
                event = reader.next();

                if (event == EDIStreamEvent.START_SEGMENT) {
                    segment = reader.getText();
                } else if (event == EDIStreamEvent.ELEMENT_DATA) {
                    if ("TA1".equals(segment)) {
                        if (reader.getLocation().getElementPosition() == 4) {
                            ta104 = reader.getText();
                        } else if (reader.getLocation().getElementPosition() == 5) {
                            ta105 = reader.getText();
                        }
                    }
                }
            }
        }

        return "A".equals(ta104) && "000".equals(ta105);
    }

}
