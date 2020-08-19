package io.xlate.edi.internal.schema.implementation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIStreamValidationError;

class SegmentImplTest {

    @Test
    void testElementTypeDescriptionAccess() throws EDIStreamException, EDISchemaException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        ByteArrayInputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *200711*0100*^*00501*000000001*0*T*:~"
                + "GS*HC*99999999999*888888888888*20111219*1340*1*X*005010X222~"
                + "ST*837*0001*005010X222~"
                + "BHT*0019*00*565743*20110523*154959*CH~"
                + "NM1*41*2*SAMPLE INC*****46*496103~"
                + "PER*IC*EDI DEPT*EM*FEEDBACK@example.com*TE*3305551212~"
                + "NM1*40*2*PPO BLUE*****46*54771~"
                + "HL*1**20*1~"
                + "SE*6*0001~"
                + "GE*1*1~"
                + "IEA*1*000000001~").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        List<EDIStreamValidationError> errors = new ArrayList<>();
        List<EDIReference> nm1references = new ArrayList<>();

        while (reader.hasNext()) {
            switch (reader.next()) {
            case START_TRANSACTION:
                reader.setTransactionSchema(SchemaFactory.newFactory()
                                                         .createSchema(getClass().getResource("/x12/005010X222/837_loop1000_only.xml")));
                break;
            case START_SEGMENT:
                if ("NM1".equals(reader.getReferenceCode())) {
                    nm1references.add(reader.getSchemaTypeReference());
                }
                break;
            case SEGMENT_ERROR:
            case ELEMENT_OCCURRENCE_ERROR:
            case ELEMENT_DATA_ERROR:
                errors.add(reader.getErrorType());
                System.out.println("Unexpected error: " + reader.getErrorType() + "; " + reader.getText() + "; " + reader.getLocation());
                break;
            default:
                break;
            }
        }

        assertEquals(0, errors.size(), () -> errors.toString());
        assertEquals(2, nm1references.size());
        // Values set in 837_loop1000_only.xml
        assertEquals("Submitter Name", nm1references.get(0).getTitle());
        assertEquals("Receiver Name", nm1references.get(1).getTitle());

        // Values of NM1 standard set in 837.xml
        assertEquals("Individual or Organizational Name", nm1references.get(0).getReferencedType().getTitle());
        assertEquals("Individual or Organizational Name", nm1references.get(1).getReferencedType().getTitle());
    }

}
