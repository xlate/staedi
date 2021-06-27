/*******************************************************************************
 * Copyright 2020 xlate.io LLC, http://www.xlate.io
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


import static io.xlate.edi.test.StaEDITestUtil.assertEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;

import org.junit.jupiter.api.Test;

import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamFilter;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIStreamValidationError;

@SuppressWarnings("resource")
class CompositeValidationTest {

    @Test
    void testInvalidCompositeOccurrences() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "S01*X~"
                + "S10*A*1:COMP2*2:COMP3~" // TOO_MANY_DATA_ELEMENTS
                + "S11*A*2:REP1^3:REP2^4:REP3*5~" // TOO_MANY_REPETITIONS
                + "S11*B*3:REP1*4~" // IMPLEMENTATION_UNUSED_DATA_ELEMENT_PRESENT
                + "S11*B**:5~" // TOO_MANY_COMPONENTS
                + "S99*X:X~" // SEGMENT_NOT_IN_DEFINED_TRANSACTION_SET
                + "S12*A^B**X~" // REQUIRED_DATA_ELEMENT_MISSING (S1202), IMPLEMENTATION_TOO_FEW_REPETITIONS (S1203)
                + "S12*A*X:Y*1^2*YY~" // IMPLEMENTATION_TOO_FEW_REPETITIONS (S1201), IMPLEMENTATION_INVALID_CODE_VALUE (S1204 == YY)
                + "S12*A^B*X:Y:ZZ:??*1^2*XX~" // IMPLEMENTATION_UNUSED_DATA_ELEMENT_PRESENT (S1202-03 == ZZ)
                + "S13*A*1234567890~" // IMPLEMENTATION_TOO_FEW_REPETITIONS (S1202)
                + "S09*X~"
                + "IEA*1*508121953~").getBytes());

        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        URL schemaLocation = getClass().getResource("/x12/EDISchemaSegmentValidation.xml");
        Schema schema = schemaFactory.createSchema(schemaLocation);

        EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);
        reader = factory.createFilteredReader(reader, new EDIStreamFilter() {
            @Override
            public boolean accept(EDIStreamReader reader) {
                switch (reader.getEventType()) {
                case START_TRANSACTION:
                case SEGMENT_ERROR:
                case ELEMENT_OCCURRENCE_ERROR:
                case ELEMENT_DATA_ERROR:
                case START_LOOP:
                case END_LOOP:
                    return true;
                default:
                    return false;
                }
            }
        });

        assertEvent(reader, EDIStreamEvent.START_TRANSACTION);
        reader.setTransactionSchema(schemaFactory.createSchema(getClass().getResource("/x12/composites/invalid-composite-occurrences.xml")));

        // Loop A
        assertEvent(reader, EDIStreamEvent.START_LOOP, "0000A");
        assertEvent(reader, EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR, EDIStreamValidationError.TOO_MANY_DATA_ELEMENTS, null);
        assertEvent(reader, EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR, EDIStreamValidationError.TOO_MANY_REPETITIONS, "C001");
        assertEvent(reader, EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR, EDIStreamValidationError.IMPLEMENTATION_UNUSED_DATA_ELEMENT_PRESENT, "C001");
        assertEvent(reader, EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR, EDIStreamValidationError.TOO_MANY_COMPONENTS, "E002");
        assertEvent(reader, EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR, EDIStreamValidationError.TOO_MANY_COMPONENTS, "E002");
        assertEvent(reader, EDIStreamEvent.SEGMENT_ERROR, EDIStreamValidationError.SEGMENT_NOT_IN_DEFINED_TRANSACTION_SET, "S99", null);
        assertEvent(reader, EDIStreamEvent.END_LOOP, "0000A");

        assertEvent(reader, EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR, EDIStreamValidationError.REQUIRED_DATA_ELEMENT_MISSING, "C002");
        assertEvent(reader, EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR, EDIStreamValidationError.IMPLEMENTATION_TOO_FEW_REPETITIONS, "E002");
        assertEvent(reader, EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR, EDIStreamValidationError.IMPLEMENTATION_TOO_FEW_REPETITIONS, "E001");
        assertEvent(reader, EDIStreamEvent.ELEMENT_DATA_ERROR, EDIStreamValidationError.IMPLEMENTATION_INVALID_CODE_VALUE, "E003");

        assertEvent(reader, EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR, EDIStreamValidationError.IMPLEMENTATION_UNUSED_DATA_ELEMENT_PRESENT, "E003");
        assertEquals(10, reader.getLocation().getSegmentPosition());
        assertEquals(2, reader.getLocation().getElementPosition());
        assertEquals(3, reader.getLocation().getComponentPosition());

        assertEvent(reader, EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR, EDIStreamValidationError.TOO_MANY_COMPONENTS, "C002");
        assertEquals(10, reader.getLocation().getSegmentPosition());
        assertEquals(2, reader.getLocation().getElementPosition());
        assertEquals(4, reader.getLocation().getComponentPosition());

        assertEvent(reader, EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR, EDIStreamValidationError.IMPLEMENTATION_TOO_FEW_REPETITIONS, "E002");

        assertTrue(!reader.hasNext(), "Unexpected segment errors exist");
    }
}
