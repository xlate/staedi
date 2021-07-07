package io.xlate.edi.internal.schema.implementation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.xlate.edi.internal.schema.ElementPosition;
import io.xlate.edi.schema.EDIElementPosition;

class DiscriminatorImplTest {

    DiscriminatorImpl target;

    @BeforeEach
    void setUp() throws Exception {
        target = new DiscriminatorImpl(position(1, 2), Collections.singleton("50"));
    }

    ElementPosition position(int e, int c) {
        return new ElementPosition(e, c);
    }

    @Test
    void testHashCode() {
        int expected = new DiscriminatorImpl(position(1, 2), Collections.singleton("50")).hashCode();
        assertEquals(expected, target.hashCode());
    }

    @Test
    void testEquals_Same() {
        assertEquals(target.position, target.position);
        assertEquals(target, target);
    }

    @Test
    void testEquals_Identical() {
        DiscriminatorImpl identical = new DiscriminatorImpl(position(1, 2), Collections.singleton("50"));
        assertEquals(target.position, identical.position);
        assertEquals(target, identical);
    }

    @Test
    void testEquals_NotInstance() {
        for (Object other : Arrays.asList(new Object(), null)) {
            assertNotEquals(target, other);
        }
    }

    @Test
    void testEquals_PositionNotInstance() {
        DiscriminatorImpl actual = new DiscriminatorImpl(new EDIElementPosition() {
            @Override
            public int getComponentPosition() {
                return 1;
            }

            @Override
            public int getElementPosition() {
                return 2;
            }
        }, Collections.singleton("50"));

        assertNotEquals(target, actual);
    }

    @Test
    void testEquals_Different() {
        assertNotEquals(new DiscriminatorImpl(position(1, 2), Collections.singleton("60")), target);
        assertNotEquals(new DiscriminatorImpl(position(2, 2), Collections.singleton("50")), target);
        assertNotEquals(new DiscriminatorImpl(position(1, 3), Collections.singleton("50")), target);
    }

    @Test
    void testToString() {
        String expected = new DiscriminatorImpl(position(1, 2), Collections.singleton("50")).toString();
        assertEquals(expected, target.toString());
    }

}
