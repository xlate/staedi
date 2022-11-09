package io.xlate.edi.internal.schema.implementation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.test.StaEDITestUtil;

class ElementImplTest {

    final int DFLT_MIN_OCCURS = 1;
    final int DFLT_MAX_OCCURS = 9;
    final String DFLT_ID = "E0001";
    final int DFLT_POSITION = 1;
    final Map<String, String> DFLT_VALUES = Collections.emptyMap();
    final String DFLT_TITLE = "Test Element";
    final String DFLT_DESCR = "Just for testing";

    @Test
    void testHashCode() {
        ElementImpl e1 = new ElementImpl(DFLT_MIN_OCCURS, DFLT_MAX_OCCURS, DFLT_ID, DFLT_POSITION, DFLT_VALUES, DFLT_TITLE, DFLT_DESCR);
        ElementImpl e2 = new ElementImpl(DFLT_MIN_OCCURS, DFLT_MAX_OCCURS, DFLT_ID, DFLT_POSITION, DFLT_VALUES, DFLT_TITLE, DFLT_DESCR);
        ElementImpl e3 = new ElementImpl(DFLT_MIN_OCCURS, DFLT_MAX_OCCURS, DFLT_ID, 2, DFLT_VALUES, DFLT_TITLE, DFLT_DESCR);

        assertNotSame(e2, e1);
        assertEquals(e1.hashCode(), e2.hashCode());
        assertNotEquals(e1.hashCode(), e3.hashCode());
    }

    @Test
    void testEquals() {
        ElementImpl e1 = new ElementImpl(DFLT_MIN_OCCURS, DFLT_MAX_OCCURS, DFLT_ID, DFLT_POSITION, DFLT_VALUES, DFLT_TITLE, DFLT_DESCR);
        ElementImpl e2 = new ElementImpl(DFLT_MIN_OCCURS, DFLT_MAX_OCCURS, DFLT_ID, DFLT_POSITION, DFLT_VALUES, DFLT_TITLE, DFLT_DESCR);
        assertEquals(e1, e2);
    }

    @Test
    void testEquals_Identity() {
        ElementImpl e1 = new ElementImpl(DFLT_MIN_OCCURS, DFLT_MAX_OCCURS, DFLT_ID, DFLT_POSITION, DFLT_VALUES, DFLT_TITLE, DFLT_DESCR);
        assertEquals(e1, e1);
    }

    @Test
    void testEquals_NotInstances() {
        ElementImpl e1 = new ElementImpl(DFLT_MIN_OCCURS, DFLT_MAX_OCCURS, DFLT_ID, DFLT_POSITION, DFLT_VALUES, DFLT_TITLE, DFLT_DESCR);

        for (Object other : Arrays.asList(new Object(), null)) {
            assertNotEquals(e1, other);
        }
    }

    @Test
    void testEquals_DifferentPosition() {
        ElementImpl e1 = new ElementImpl(DFLT_MIN_OCCURS, DFLT_MAX_OCCURS, DFLT_ID, DFLT_POSITION, DFLT_VALUES, DFLT_TITLE, DFLT_DESCR);
        ElementImpl e2 = new ElementImpl(DFLT_MIN_OCCURS, DFLT_MAX_OCCURS, DFLT_ID, 2, DFLT_VALUES, DFLT_TITLE, DFLT_DESCR);
        assertNotEquals(e1, e2);
    }

    @Test
    void testEquals_DifferentValueSet() {
        Map<String, String> v1 = Collections.singletonMap("A", "Title A");
        Map<String, String> v2 = Collections.singletonMap("B", "Title B");
        ElementImpl e1 = new ElementImpl(DFLT_MIN_OCCURS, DFLT_MAX_OCCURS, DFLT_ID, DFLT_POSITION, v1, DFLT_TITLE, DFLT_DESCR);
        ElementImpl e2 = new ElementImpl(DFLT_MIN_OCCURS, DFLT_MAX_OCCURS, DFLT_ID, DFLT_POSITION, v2, DFLT_TITLE, DFLT_DESCR);
        assertNotEquals(e1, e2);
    }

    @Test
    void testEquals_DifferentBaseImplValues() {
        ElementImpl e1 = new ElementImpl(DFLT_MIN_OCCURS, DFLT_MAX_OCCURS, DFLT_ID, DFLT_POSITION, DFLT_VALUES, DFLT_TITLE, DFLT_DESCR);

        assertNotEquals(e1, new ElementImpl(2, DFLT_MAX_OCCURS, DFLT_ID, DFLT_POSITION, DFLT_VALUES, DFLT_TITLE, DFLT_DESCR));
        assertNotEquals(e1, new ElementImpl(DFLT_MIN_OCCURS, 10, DFLT_ID, DFLT_POSITION, DFLT_VALUES, DFLT_TITLE, DFLT_DESCR));
        assertNotEquals(e1, new ElementImpl(DFLT_MIN_OCCURS, DFLT_MAX_OCCURS, "E0002", DFLT_POSITION, DFLT_VALUES, DFLT_TITLE, DFLT_DESCR));
        assertNotEquals(e1,
                        new ElementImpl(DFLT_MIN_OCCURS,
                                        DFLT_MAX_OCCURS,
                                        DFLT_ID,
                                        DFLT_POSITION,
                                        DFLT_VALUES,
                                        "Another Title",
                                        DFLT_DESCR));
        assertNotEquals(e1,
                        new ElementImpl(DFLT_MIN_OCCURS, DFLT_MAX_OCCURS, DFLT_ID, DFLT_POSITION, DFLT_VALUES, DFLT_TITLE, "Do not use!"));

        EDISimpleType standard = new EDISimpleType() {
            @Override
            public String getId() {
                return "E0003";
            }

            @Override
            public String getCode() {
                return "3";
            }

            @Override
            public Type getType() {
                return Type.ELEMENT;
            }

            @Override
            public Base getBase() {
                return Base.STRING;
            }

            @Override
            public int getNumber() {
                return 3;
            }

            @Override
            public long getMinLength() {
                return 0;
            }

            @Override
            public long getMaxLength() {
                return 5;
            }

            @Override
            public Set<String> getValueSet() {
                return Collections.emptySet();
            }

            @Override
            public Map<String, String> getValues() {
                return Collections.emptyMap();
            }

            @Override
            public String getTitle() {
                return null;
            }

            @Override
            public String getDescription() {
                return null;
            }
        };
        EDIReference reference = new EDIReference() {
            @Override
            public EDIType getReferencedType() {
                return standard;
            }

            @Override
            public int getMinOccurs() {
                return DFLT_MIN_OCCURS;
            }

            @Override
            public int getMaxOccurs() {
                return DFLT_MAX_OCCURS;
            }

            @Override
            public String getTitle() {
                return null;
            }

            @Override
            public String getDescription() {
                return null;
            }
        };

        ElementImpl e3 = new ElementImpl(DFLT_MIN_OCCURS, DFLT_MAX_OCCURS, DFLT_ID, DFLT_POSITION, DFLT_VALUES, DFLT_TITLE, DFLT_DESCR);
        e3.setStandardReference(reference);

        assertNotEquals(e1, e3);
    }

    @Test
    void testToString() {
        ElementImpl e1 = new ElementImpl(DFLT_MIN_OCCURS, DFLT_MAX_OCCURS, DFLT_ID, DFLT_POSITION, DFLT_VALUES, DFLT_TITLE, DFLT_DESCR);
        ElementImpl e2 = new ElementImpl(DFLT_MIN_OCCURS, DFLT_MAX_OCCURS, DFLT_ID, DFLT_POSITION, DFLT_VALUES, DFLT_TITLE, DFLT_DESCR);

        assertEquals(e1.toString(), e2.toString());
    }

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
                + "HL*2*1*22*0~"
                + "SE*8*0001~"
                + "GE*1*1~"
                + "IEA*1*000000001~").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        List<EDIStreamValidationError> errors = new ArrayList<>();
        List<EDIReference> nm103references = new ArrayList<>();

        while (reader.hasNext()) {
            switch (reader.next()) {
            case START_TRANSACTION:
                reader.setTransactionSchema(SchemaFactory.newFactory()
                                                         .createSchema(getClass().getResource("/x12/005010X222/837_loop1000_only.xml")));
                break;
            case ELEMENT_DATA:
                if ("NM103".equals(reader.getReferenceCode())) {
                    nm103references.add(reader.getSchemaTypeReference());
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

        // Values set in 837_loop1000_only.xml
        assertEquals(2, nm103references.size());
        assertEquals("Submitter Last or Organization Name", nm103references.get(0).getTitle());
        assertEquals("Receiver Name", nm103references.get(1).getTitle());

        // Values of NM103 standard set in 837.xml
        assertEquals("Name Last or Organization Name", nm103references.get(0).getReferencedType().getTitle());
        assertEquals("Name Last or Organization Name", nm103references.get(1).getReferencedType().getTitle());
    }

    @Test
    void testElementTypeDescriptionAccessOnDescriminator() throws EDIStreamException, EDISchemaException {
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
                + "SE*7*0001~"
                + "GE*1*1~"
                + "IEA*1*000000001~").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        List<EDIStreamValidationError> errors = new ArrayList<>();
        List<EDIReference> nm101references = new ArrayList<>();

        while (reader.hasNext()) {
            switch (reader.next()) {
            case START_TRANSACTION:
                reader.setTransactionSchema(SchemaFactory.newFactory()
                                                         .createSchema(getClass().getResource("/x12/005010X222/837_loop1000_only.xml")));
                break;
            case ELEMENT_DATA:
                if ("NM101".equals(reader.getReferenceCode())) {
                    nm101references.add(reader.getSchemaTypeReference());
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

        // Values set in 837_loop1000_only.xml
        assertEquals(2, nm101references.size());
        assertEquals("Entity Identifier Code (Submitter)", nm101references.get(0).getTitle());
        assertEquals("Entity Identifier Code (Receiver)", nm101references.get(1).getTitle());

        // Values of NM101 standard set in 837.xml
        assertEquals("Entity Identifier Code", nm101references.get(0).getReferencedType().getTitle());
        assertEquals("Entity Identifier Code", nm101references.get(1).getReferencedType().getTitle());
    }

    @Test
    void testElementTypeDescriptionAccessPriorToDescriminator() throws EDIStreamException, EDISchemaException {
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
                + "SE*7*0001~"
                + "GE*1*1~"
                + "IEA*1*000000001~").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        List<EDIStreamValidationError> errors = new ArrayList<>();
        List<EDIReference> hlReferences = new ArrayList<>();

        while (reader.hasNext()) {
            switch (reader.next()) {
            case START_TRANSACTION:
                reader.setTransactionSchema(SchemaFactory.newFactory()
                                                         .createSchema(getClass().getResource("/x12/005010X222/837_loop1000_only.xml")));
                break;
            case ELEMENT_DATA:
                if ("HL".equals(reader.getLocation().getSegmentTag())) {
                    hlReferences.add(reader.getSchemaTypeReference());
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

        // Values set in 837_loop1000_only.xml, except for offset `1`
        assertEquals(4, hlReferences.size());
        assertEquals("Hierarchical ID Number (20)", hlReferences.get(0).getTitle());
        assertNull(hlReferences.get(1).getTitle());
        assertEquals("Hierarchical Level Code (20)", hlReferences.get(2).getTitle());
        assertEquals("Hierarchical Child Code (20)", hlReferences.get(3).getTitle());

        // Values of NM101 standard set in 837.xml
        assertEquals("Hierarchical ID Number", hlReferences.get(0).getReferencedType().getTitle());
        assertEquals("Hierarchical Parent ID Number", hlReferences.get(1).getReferencedType().getTitle());
        assertEquals("Hierarchical Level Code", hlReferences.get(2).getReferencedType().getTitle());
        assertEquals("Hierarchical Child Code", hlReferences.get(3).getReferencedType().getTitle());
    }

    @Test
    void testImplElementsPriorToDiscriminatorAreValidated() throws EDIStreamException, EDISchemaException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        ByteArrayInputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *200711*0100*^*00501*000000001*0*T*:~"
                + "GS*HC*99999999999*888888888888*20111219*1340*1*X*005010X222~"
                + "ST*837*0001*005010X222~"
                + "BHT*0019*00*565743*20110523*154959*CH~"
                + "NM1*41*2*SAMPLE INC*****46*496103~"
                + "PER*IC*EDI DEPT*EM*FEEDBACK@example.com*TE*3305551212~"
                + "NM1*40*2*PPO BLUE*****46*54771~"
                + "HL*1*BAD*20*1~"
                + "HL*2**22*0~"
                + "SE*6*0001~"
                + "GE*1*1~"
                + "IEA*1*000000001~").getBytes());

        EDIStreamReader reader = StaEDITestUtil.filterEvents(factory,
                                                             factory.createEDIStreamReader(stream),
                                                             EDIStreamEvent.START_TRANSACTION,
                                                             EDIStreamEvent.SEGMENT_ERROR,
                                                             EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR,
                                                             EDIStreamEvent.ELEMENT_DATA_ERROR);

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next());
        reader.setTransactionSchema(SchemaFactory.newFactory()
                                    .createSchema(getClass().getResource("/x12/005010X222/837_loop1000_only.xml")));

        assertEquals(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.IMPLEMENTATION_UNUSED_DATA_ELEMENT_PRESENT, reader.getErrorType());
        assertEquals("BAD", reader.getText());

        assertEquals(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.REQUIRED_DATA_ELEMENT_MISSING, reader.getErrorType());
    }
}
