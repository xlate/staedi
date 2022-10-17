package io.xlate.edi.internal.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.schema.EDISimpleType.Base;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIStreamValidationError;

class ElementTypeTest {

    static final byte[] INTERCHANGE_RESPONSE = (""
            + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *200711*0100*^*00501*000000001*0*T*:~"
            + "TA1*000000050*200229*1200*A*000~"
            + "IEA*0*000000001~").getBytes();

    @Test
    void testElementContructorNoValues() {
        ElementType e = new ElementType("E1", Base.STRING, -1, "1", 1, 0L, 5L, Collections.emptyMap(), Collections.emptyList(), null, null);
        assertEquals("id: E1, type: ELEMENT, base: STRING, code: 1, minLength: 0, maxLength: 5, values: {}", e.toString());
    }

    @Test
    void testElementContructorWithValues() {
        Map<String, String> values = new HashMap<>();
        values.put("ABCDE", "Title 1");
        values.put("FGHIJ", "Title 2");
        ElementType e = new ElementType("E1", Base.STRING, -1, "1", 1, 0L, 5L, values, Collections.emptyList(), null, null);
        assertEquals("id: E1, type: ELEMENT, base: STRING, code: 1, minLength: 0, maxLength: 5, values: {ABCDE=Title 1, FGHIJ=Title 2}", e.toString());
    }

    @SuppressWarnings("deprecation")
    @Test
    void testElementTypeNumberDefault() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        ByteArrayInputStream stream = new ByteArrayInputStream(INTERCHANGE_RESPONSE);
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        List<EDIStreamValidationError> errors = new ArrayList<>();
        EDIReference i03reference = null;

        while (reader.hasNext()) {
            switch (reader.next()) {
            case ELEMENT_DATA:
                if ("I03".equals(reader.getReferenceCode())) {
                    i03reference = reader.getSchemaTypeReference();
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
        assertNotNull(i03reference);
        assertNotNull(i03reference.getReferencedType());
        assertEquals(-1, ((EDISimpleType) i03reference.getReferencedType()).getNumber());
    }

    @Test
    void testElementTypeDescriptionAccess() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        ByteArrayInputStream stream = new ByteArrayInputStream(INTERCHANGE_RESPONSE);
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        List<EDIStreamValidationError> errors = new ArrayList<>();
        EDIReference i03reference = null;

        while (reader.hasNext()) {
            switch (reader.next()) {
            case ELEMENT_DATA:
                if ("I03".equals(reader.getReferenceCode())) {
                    i03reference = reader.getSchemaTypeReference();
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
        assertNotNull(i03reference);
        assertNull(i03reference.getTitle());
        assertNull(i03reference.getDescription());
        assertNotNull(i03reference.getReferencedType());
        // Values set in common.xml
        assertEquals("Security Information Qualifier", i03reference.getReferencedType().getTitle());
        assertEquals("Code identifying the type of information in the Security Information", i03reference.getReferencedType().getDescription());
    }
}
