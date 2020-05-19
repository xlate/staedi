/*******************************************************************************
 * Copyright 2017 xlate.io LLC, http://www.xlate.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package io.xlate.edi.internal.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.Location;

@SuppressWarnings("resource")
class StaEDIFilteredStreamReaderTest implements ConstantsTest {

    @Test
    /**
     * Filter all except repeat > 1 of an element or component elements where
     * the position within the composite > 1.
     *
     * @throws EDIStreamException
     */
    void testNext() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/extraDelimiter997.edi");
        EDIStreamReader reader = factory.createFilteredReader(factory.createEDIStreamReader(stream), r -> {
            if (r.getEventType() != EDIStreamEvent.ELEMENT_DATA) {
                return false;
            }
            Location location = r.getLocation();
            return location.getComponentPosition() > 1 ||
                    location.getElementOccurrence() > 1;
        });

        EDIStreamEvent event;
        int matches = 0;

        while (reader.hasNext()) {
            event = reader.next();

            if (event != EDIStreamEvent.ELEMENT_DATA) {
                fail("Unexpected event: " + event);
            }

            String text = reader.getText();
            assertTrue(text.matches(".*(R[2-9]|COMP[2-9]).*"), "Not matched: " + text);
            matches++;
        }

        assertEquals(9, matches);
    }

    @Test
    /**
     * Only allow segment tags containing S, G, or 5 to pass the filter.
     *
     * @throws EDIStreamException
     */
    void testNextTag() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        EDIStreamReader reader = factory.createFilteredReader(factory.createEDIStreamReader(stream), r -> {
            if (r.getEventType() != EDIStreamEvent.START_SEGMENT) {
                return false;
            }
            String tag = r.getText();
            return tag.matches("^.{0,2}[SG5].{0,2}$");
        });

        EDIStreamEvent event;
        int matches = 0;
        String tag = null;

        while (reader.hasNext()) {
            try {
                event = reader.nextTag();
            } catch (@SuppressWarnings("unused") NoSuchElementException e) {
                break;
            }

            if (event != EDIStreamEvent.START_SEGMENT) {
                fail("Unexpected event: " + event);
            }

            tag = reader.getText();
            assertTrue(
                       tag.indexOf('S') > -1 ||
                               tag.indexOf('G') > -1 ||
                               tag.indexOf('5') > -1);
            matches++;
        }

        assertEquals("GE", tag, "Unexpected last segment");
        assertEquals(6, matches);
    }

    @Test
    /**
     * Filter all except single character element events
     *
     * @throws EDIStreamException
     */
    void testHasNext() throws EDIStreamException, IOException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/extraDelimiter997.edi");
        EDIStreamReader reader = factory.createFilteredReader(factory.createEDIStreamReader(stream), r -> {
            if (r.getEventType() != EDIStreamEvent.ELEMENT_DATA) {
                return false;
            }
            return r.getTextLength() == 1;
        });

        EDIStreamEvent event;
        int matches = 0;

        while (reader.hasNext()) {
            event = reader.next();

            if (event != EDIStreamEvent.ELEMENT_DATA) {
                fail("Unexpected event: " + event);
            }

            String text = reader.getText();
            assertEquals(1, text.length(), "Wrong length: " + text);
            matches++;
        }

        reader.close();

        assertEquals(16, matches);
    }

    @Test
    /**
     * Test that the filtered and unfiltered readers return the same information
     * at each event of the filtered reader.
     *
     * @throws EDIStreamException
     */
    void testNextTagFilterParityWithUnfiltered() throws EDIStreamException, EDISchemaException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        final String PROP = EDIInputFactory.EDI_VALIDATE_CONTROL_STRUCTURE;
        factory.setProperty(PROP, Boolean.TRUE);
        EDIStreamReader unfiltered = factory.createEDIStreamReader(stream);
        EDIStreamReader filtered = factory.createFilteredReader(unfiltered, r -> {
            switch (r.getEventType()) {
            case START_INTERCHANGE:
            case START_TRANSACTION:
                return true;
            case START_SEGMENT:
                String tag = r.getText();
                return tag.matches("^.{0,2}[SG5].{0,2}$");
            default:
                break;
            }
            return false;
        });

        SchemaFactory schemaFactory = SchemaFactory.newFactory();

        assertThrows(IllegalArgumentException.class, () -> unfiltered.getProperty(null));
        assertThrows(IllegalArgumentException.class, () -> filtered.getProperty(null));

        assertEquals(unfiltered.getProperty(PROP), filtered.getProperty(PROP));

        filtered.next(); // START_INTERCHANGE

        assertEquals(filtered.getStandard(), unfiltered.getStandard());
        assertArrayEquals(filtered.getVersion(), unfiltered.getVersion());
        assertEquals(unfiltered.getDelimiters(), filtered.getDelimiters());

        assertNull(filtered.getControlSchema());
        assertNull(filtered.getTransactionSchema());

        Schema control = schemaFactory.getControlSchema(filtered.getStandard(), filtered.getVersion());
        filtered.setControlSchema(control);
        assertEquals(control, filtered.getControlSchema());
        assertEquals(control, unfiltered.getControlSchema());
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> filtered.setControlSchema(control));
        assertEquals("control schema already set", thrown.getMessage());

        filtered.nextTag(); // ISA
        thrown = assertThrows(IllegalStateException.class, () -> filtered.setControlSchema(control));
        assertEquals("control schema set after interchange start", thrown.getMessage());

        assertStatusEquals(unfiltered, filtered);

        filtered.nextTag(); // GS
        assertStatusEquals(unfiltered, filtered);

        filtered.next(); // START_TRANSACTION
        Schema transaction = schemaFactory.createSchema(getClass().getResourceAsStream("/x12/EDISchema997.xml"));
        filtered.setTransactionSchema(transaction);
        assertEquals(transaction, filtered.getTransactionSchema());
        assertEquals(transaction, unfiltered.getTransactionSchema());

        filtered.nextTag(); // ST
        assertStatusEquals(unfiltered, filtered);
        filtered.nextTag(); // AK5
        assertStatusEquals(unfiltered, filtered);
        filtered.nextTag(); // SE
        assertStatusEquals(unfiltered, filtered);
        filtered.nextTag(); // GE
        assertStatusEquals(unfiltered, filtered);
    }

    void assertStatusEquals(EDIStreamReader unfiltered, EDIStreamReader filtered) {
        assertEquals(unfiltered.getEventType(), filtered.getEventType());
        assertEquals(unfiltered.getText(), filtered.getText());
        assertEquals(unfiltered.getReferenceCode(), filtered.getReferenceCode());
        assertArrayEquals(unfiltered.getTextCharacters(), filtered.getTextCharacters());
        assertEquals(unfiltered.getTextStart(), filtered.getTextStart());
        assertEquals(unfiltered.getTextLength(), filtered.getTextLength());

        char[] uf_target = new char[100];
        Arrays.fill(uf_target, '\0');
        char[] f_target = new char[100];
        Arrays.fill(f_target, '\0');
        unfiltered.getTextCharacters(unfiltered.getTextStart(), uf_target, 0, unfiltered.getTextLength());
        filtered.getTextCharacters(filtered.getTextStart(), f_target, 0, filtered.getTextLength());
        assertArrayEquals(uf_target, f_target);
    }
}
