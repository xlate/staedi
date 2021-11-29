package io.xlate.edi.internal.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.EDIType.Type;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIStreamValidationError;

class StructureTypeTest {

    @Test
    void testStructureConstruction() {
        EDIReference ref = new Reference("E1", EDIType.Type.ELEMENT, 1, 1, null, null);
        EDISyntaxRule rule = new SyntaxRestriction(EDISyntaxRule.Type.EXCLUSION, Arrays.asList(1, 8));
        StructureType s = new StructureType("SEG", Type.SEGMENT, "SEG", Arrays.asList(ref), Arrays.asList(rule), null, null);
        assertEquals("id: SEG, type: SEGMENT, code: SEG, references: [{refId: E1, minOccurs: 1, maxOccurs: 1, type: { null }}], syntaxRestrictions: [{type: EXCLUSION, positions: [1, 8]}]",
                     s.toString());
    }

    @Test
    void testStructureTypeDescriptionAccess() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        ByteArrayInputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *200711*0100*^*00501*000000001*0*T*:~"
                + "TA1*000000050*200229*1200*A*000~"
                + "IEA*1*000000001~").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        List<EDIStreamValidationError> errors = new ArrayList<>();
        EDIReference ta1reference = null;

        while (reader.hasNext()) {
            switch (reader.next()) {
            case END_SEGMENT:
                if ("TA1".equals(reader.getReferenceCode())) {
                    ta1reference = reader.getSchemaTypeReference();
                }
                break;
            case SEGMENT_ERROR:
            case ELEMENT_OCCURRENCE_ERROR:
            case ELEMENT_DATA_ERROR:
                errors.add(reader.getErrorType());
                break;
            default:
                break;
            }
        }

        assertEquals(0, errors.size());
        assertNotNull(ta1reference);
        // Value set in v00402.xml
        assertEquals("Interchange Acknowledgment", ta1reference.getTitle());
        assertNull(ta1reference.getDescription());
        assertNotNull(ta1reference.getReferencedType());
        // Values set in common.xml
        assertEquals("Interchange Acknowledgment", ta1reference.getReferencedType().getTitle());
        assertEquals("To report the status of processing a received interchange header and trailer or the non-delivery by a network provider",
                     ta1reference.getReferencedType().getDescription());

        // Expect the titles to be different String instances
        assertNotSame(ta1reference.getTitle(), ta1reference.getReferencedType().getTitle());
    }
}
