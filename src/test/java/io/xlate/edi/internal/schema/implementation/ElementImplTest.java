package io.xlate.edi.internal.schema.implementation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.schema.EDIType;

class ElementImplTest {

    final int DFLT_MIN_OCCURS = 1;
    final int DFLT_MAX_OCCURS = 9;
    final String DFLT_ID = "E0001";
    final int DFLT_POSITION = 1;
    final Set<String> DFLT_VALUES = Collections.emptySet();
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
        Set<String> v1 = Collections.singleton("A");
        Set<String> v2 = Collections.singleton("B");
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
        assertNotEquals(e1, new ElementImpl(DFLT_MIN_OCCURS, DFLT_MAX_OCCURS, DFLT_ID, DFLT_POSITION, DFLT_VALUES, "Another Title", DFLT_DESCR));
        assertNotEquals(e1, new ElementImpl(DFLT_MIN_OCCURS, DFLT_MAX_OCCURS, DFLT_ID, DFLT_POSITION, DFLT_VALUES, DFLT_TITLE, "Do not use!"));

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

}
