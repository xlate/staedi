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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;

import org.junit.jupiter.api.Test;

import io.xlate.edi.internal.schema.SchemaUtils;
import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamConstants.Standards;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamFilter;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIStreamValidationError;

class EventSequenceTest {

    @Test
    void testValidatedTagSequence() throws EDISchemaException,
                                           EDIStreamException,
                                           IllegalStateException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "GS*FA*ReceiverDept*SenderDept*20050812*195335*000005*X*005010X230~"
                + "ST*997*0001*005010X230~"
                + "AK1*HC*000001~"
                + "AK2*837*0021~"
                + "AK3*NM1*8**8~"
                + "AK4*8*66*7*MI~"
                + "AK5*R*5~"
                + "AK9*R*1*1*0~"
                + "SE*8*0001~"
                + "GE*1*000005~"
                + "IEA*1*508121953~").getBytes());

        EDIStreamEvent event;

        @SuppressWarnings("resource")
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        assertEquals(EDIStreamEvent.START_INTERCHANGE, reader.next());
        reader.setControlSchema(SchemaUtils.getControlSchema(reader.getStandard(), reader.getVersion()));

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.next());
        assertEquals("ISA", reader.getText());
        while ((event = reader.next()) == EDIStreamEvent.ELEMENT_DATA) {
            continue;
        }
        assertEquals(EDIStreamEvent.END_SEGMENT, event);

        assertEquals(EDIStreamEvent.START_GROUP, reader.next());
        assertEquals("GROUP", reader.getText());

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.next());
        assertEquals("GS", reader.getText());
        while ((event = reader.next()) == EDIStreamEvent.ELEMENT_DATA) {
            continue;
        }
        assertEquals(EDIStreamEvent.END_SEGMENT, event);

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next());
        assertEquals("TRANSACTION", reader.getText());
        reader.setTransactionSchema(loadX12FuncAckSchema());

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.next());
        assertEquals("ST", reader.getText());
        while ((event = reader.next()) == EDIStreamEvent.ELEMENT_DATA) {
            continue;
        }
        assertEquals(EDIStreamEvent.END_SEGMENT, event);

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.next());
        assertEquals("AK1", reader.getText());
        while ((event = reader.next()) == EDIStreamEvent.ELEMENT_DATA) {
            continue;
        }
        assertEquals(EDIStreamEvent.END_SEGMENT, event);

        assertEquals(EDIStreamEvent.START_LOOP, reader.next());
        assertEquals("2000", reader.getText());

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.next());
        assertEquals("AK2", reader.getText());
        while ((event = reader.next()) == EDIStreamEvent.ELEMENT_DATA) {
            continue;
        }
        assertEquals(EDIStreamEvent.END_SEGMENT, event);

        assertEquals(EDIStreamEvent.START_LOOP, reader.next());
        assertEquals("2100", reader.getText());

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.next());
        assertEquals("AK3", reader.getText());
        while ((event = reader.next()) == EDIStreamEvent.ELEMENT_DATA) {
            continue;
        }
        assertEquals(EDIStreamEvent.END_SEGMENT, event);

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.next());
        assertEquals("AK4", reader.getText());
        ak4: while (true) {
            switch (event = reader.next()) {
            case ELEMENT_DATA:
            case START_COMPOSITE:
            case END_COMPOSITE:
                continue;
            default:
                break ak4;
            }
        }
        assertEquals(EDIStreamEvent.END_SEGMENT, event);
        assertEquals(EDIStreamEvent.END_LOOP, reader.next()); // 2100

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.next());
        assertEquals("AK5", reader.getText());
        while ((event = reader.next()) == EDIStreamEvent.ELEMENT_DATA) {
            continue;
        }
        assertEquals(EDIStreamEvent.END_SEGMENT, event);
        assertEquals(EDIStreamEvent.END_LOOP, reader.next()); // 2000

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.next());
        assertEquals("AK9", reader.getText());
        while ((event = reader.next()) == EDIStreamEvent.ELEMENT_DATA) {
            continue;
        }
        assertEquals(EDIStreamEvent.END_SEGMENT, event);

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.next());
        assertEquals("SE", reader.getText());
        while ((event = reader.next()) == EDIStreamEvent.ELEMENT_DATA) {
            continue;
        }
        assertEquals(EDIStreamEvent.END_SEGMENT, event);

        assertEquals(EDIStreamEvent.END_TRANSACTION, reader.next());

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.next());
        assertEquals("GE", reader.getText());
        while ((event = reader.next()) == EDIStreamEvent.ELEMENT_DATA) {
            continue;
        }
        assertEquals(EDIStreamEvent.END_SEGMENT, event);
        assertEquals(EDIStreamEvent.END_GROUP, reader.next()); // 0000

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.next());
        assertEquals("IEA", reader.getText());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next()); // IEA01
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next()); // IEA02
        assertEquals(EDIStreamEvent.END_SEGMENT, reader.next()); // IEA
        assertEquals(EDIStreamEvent.END_INTERCHANGE, reader.next());
    }

    @Test
    void testMissingTagSequence() throws EDISchemaException,
                                         EDIStreamException,
                                         IllegalStateException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "GS*FA*ReceiverDept*SenderDept*20050812*195335*000005*X*005010X230~"
                + "ST*997*0001*005010X230~"
                + "AK1*HC*000001~"
                + "SE*8*0001~"
                + "GE*1*000005~"
                + "IEA*1*508121953~").getBytes());

        EDIStreamEvent event;

        @SuppressWarnings("resource")
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        assertEquals(EDIStreamEvent.START_INTERCHANGE, reader.next());
        reader.setControlSchema(SchemaUtils.getControlSchema(reader.getStandard(), reader.getVersion()));

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.next());
        assertEquals("ISA", reader.getText());
        while ((event = reader.next()) == EDIStreamEvent.ELEMENT_DATA) {
            continue;
        }
        assertEquals(EDIStreamEvent.END_SEGMENT, event);

        assertEquals(EDIStreamEvent.START_GROUP, reader.next());
        assertEquals("GROUP", reader.getText());

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.next());
        assertEquals("GS", reader.getText());
        while ((event = reader.next()) == EDIStreamEvent.ELEMENT_DATA) {
            continue;
        }
        assertEquals(EDIStreamEvent.END_SEGMENT, event);

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next());
        assertEquals("TRANSACTION", reader.getText());
        reader.setTransactionSchema(loadX12FuncAckSchema());

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.next());
        assertEquals("ST", reader.getText());
        while ((event = reader.next()) == EDIStreamEvent.ELEMENT_DATA) {
            continue;
        }
        assertEquals(EDIStreamEvent.END_SEGMENT, event);

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.next());
        assertEquals("AK1", reader.getText());
        while ((event = reader.next()) == EDIStreamEvent.ELEMENT_DATA) {
            continue;
        }
        assertEquals(EDIStreamEvent.END_SEGMENT, event);

        // Missing AK9 in our test schema, does not match the standard.
        assertEquals(EDIStreamEvent.SEGMENT_ERROR, reader.next());
        assertEquals("AK9", reader.getText());

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.next());
        assertEquals("SE", reader.getText());
        while ((event = reader.next()) == EDIStreamEvent.ELEMENT_DATA) {
            continue;
        }
        assertEquals(EDIStreamEvent.END_SEGMENT, event);

        assertEquals(EDIStreamEvent.END_TRANSACTION, reader.next());

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.next());
        assertEquals("GE", reader.getText());
        while ((event = reader.next()) == EDIStreamEvent.ELEMENT_DATA) {
            continue;
        }
        assertEquals(EDIStreamEvent.END_SEGMENT, event);
        assertEquals(EDIStreamEvent.END_GROUP, reader.next()); // GROUP

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.next());
        assertEquals("IEA", reader.getText());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next()); // IEA01
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next()); // IEA02
        assertEquals(EDIStreamEvent.END_SEGMENT, reader.next()); // IEA
        assertEquals(EDIStreamEvent.END_INTERCHANGE, reader.next());
    }

    @Test
    void testValidSequenceEDIFACT() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "UNB+UNOA:3+005435656:1+006415160:1+060515:1434+00000000000778'"
                + "UNH+00000000000117+INVOIC:D:97B:UN'"
                + "BLA+UNVALIDATED'"
                + "UNT+2+00000000000117'"
                + "UNZ+1+00000000000778'").getBytes());

        @SuppressWarnings("resource")
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        assertEquals(EDIStreamEvent.START_INTERCHANGE, reader.next());
        assertArrayEquals(new String[] { "UNOA", "3" }, reader.getVersion());

        Schema schema = SchemaUtils.getControlSchema(Standards.EDIFACT, reader.getVersion());
        reader.setControlSchema(schema);

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.next());
        assertEquals("UNB", reader.getText());

        assertEquals(EDIStreamEvent.START_COMPOSITE, reader.next());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next());
        assertEquals("UNOA", reader.getText());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next());
        assertEquals("3", reader.getText());
        assertEquals(EDIStreamEvent.END_COMPOSITE, reader.next());

        assertEquals(EDIStreamEvent.START_COMPOSITE, reader.next());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next());
        assertEquals("005435656", reader.getText());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next());
        assertEquals("1", reader.getText());
        assertEquals(EDIStreamEvent.END_COMPOSITE, reader.next());

        assertEquals(EDIStreamEvent.START_COMPOSITE, reader.next());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next());
        assertEquals("006415160", reader.getText());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next());
        assertEquals("1", reader.getText());
        assertEquals(EDIStreamEvent.END_COMPOSITE, reader.next());

        assertEquals(EDIStreamEvent.START_COMPOSITE, reader.next());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next());
        assertEquals("060515", reader.getText());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next());
        assertEquals("1434", reader.getText());
        assertEquals(EDIStreamEvent.END_COMPOSITE, reader.next());

        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next());
        assertEquals("00000000000778", reader.getText());
        assertEquals(EDIStreamEvent.END_SEGMENT, reader.next());

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next());
        assertEquals("TRANSACTION", reader.getText());

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.next());
        assertEquals("UNH", reader.getText());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next());
        assertEquals("00000000000117", reader.getText());

        assertEquals(EDIStreamEvent.START_COMPOSITE, reader.next());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next());
        assertEquals("INVOIC", reader.getText());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next());
        assertEquals("D", reader.getText());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next());
        assertEquals("97B", reader.getText());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next());
        assertEquals("UN", reader.getText());
        assertEquals(EDIStreamEvent.END_COMPOSITE, reader.next());
        assertEquals(EDIStreamEvent.END_SEGMENT, reader.next());

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.next());
        assertEquals("BLA", reader.getText());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next());
        assertEquals("UNVALIDATED", reader.getText());
        assertEquals(EDIStreamEvent.END_SEGMENT, reader.next());

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.next());
        assertEquals("UNT", reader.getText());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next());
        assertEquals("2", reader.getText());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next());
        assertEquals("00000000000117", reader.getText());
        assertEquals(EDIStreamEvent.END_SEGMENT, reader.next());

        assertEquals(EDIStreamEvent.END_TRANSACTION, reader.next());

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.next());
        assertEquals("UNZ", reader.getText());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next());
        assertEquals("1", reader.getText());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next());
        assertEquals("00000000000778", reader.getText());
        assertEquals(EDIStreamEvent.END_SEGMENT, reader.next());
        assertEquals(EDIStreamEvent.END_INTERCHANGE, reader.next());
    }

    @Test
    void testElementErrorSequence() throws EDISchemaException, EDIStreamException, IllegalStateException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "GS*FA*ReceiverDept*SenderDept*BAD_DATE*295335*000005*X*005010X230~"
                + "ST*997*0001~"
                + "AK1*<NOT_IN_CODESET>*000001~"
                + "AK9*R*1*1*0~"
                + "AK9*~"
                + "SE*8*0001~"
                + "GE*1*<TOO_LONG_AND_NOT_NUMERIC>^0001^AGAIN!*:~"
                + "IEA*1*508121953~").getBytes());

        @SuppressWarnings("resource")
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        assertEquals(EDIStreamEvent.START_INTERCHANGE, reader.next());
        reader.setControlSchema(SchemaUtils.getControlSchema(reader.getStandard(), reader.getVersion()));

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.nextTag());
        assertEquals("ISA", reader.getText());
        assertEquals(EDIStreamEvent.START_GROUP, reader.nextTag());
        assertEquals("GROUP", reader.getText());
        assertEquals(EDIStreamEvent.START_SEGMENT, reader.nextTag());
        assertEquals("GS", reader.getText());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next()); // GS01
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next()); // GS02
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next()); // GS03

        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next()); // GS04
        assertEquals("BAD_DATE", reader.getText());

        // GS05 invalid time
        assertEquals(EDIStreamEvent.ELEMENT_DATA_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.INVALID_TIME, reader.getErrorType());
        assertEquals("295335", reader.getText());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next()); // GS05

        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next()); // GS06
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next()); // GS07
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next()); // GS08

        // GS04 BAD_DATE (known bad only after GS08 version is set)
        assertEquals(EDIStreamEvent.ELEMENT_DATA_ERROR, reader.next());
        assertEquals("373", reader.getReferenceCode());
        assertEquals(EDIStreamValidationError.INVALID_DATE, reader.getErrorType());
        assertEquals("BAD_DATE", reader.getText());
        assertEquals(2, reader.getLocation().getSegmentPosition());
        assertEquals("GS", reader.getLocation().getSegmentTag());
        assertEquals(4, reader.getLocation().getElementPosition());
        assertEquals(1, reader.getLocation().getElementOccurrence());
        assertEquals(-1, reader.getLocation().getComponentPosition());

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.nextTag());
        reader.setTransactionSchema(loadX12FuncAckSchema());

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.nextTag());
        assertEquals("ST", reader.getText());

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.nextTag());
        assertEquals("AK1", reader.getText());
        // AK01 <NOT_IN_CODESET>
        assertEquals(EDIStreamEvent.ELEMENT_DATA_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG, reader.getErrorType());
        assertEquals("<NOT_IN_CODESET>", reader.getText());
        assertEquals(EDIStreamEvent.ELEMENT_DATA_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.INVALID_CODE_VALUE, reader.getErrorType());
        assertEquals("<NOT_IN_CODESET>", reader.getText());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next()); // AK01

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.nextTag());
        assertEquals("AK9", reader.getText());

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.nextTag());
        assertEquals("AK9", reader.getText());

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.nextTag());
        assertEquals("SE", reader.getText());

        //assertEquals(EDIStreamEvent.END_TRANSACTION, reader.nextTag());

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.nextTag());
        assertEquals("GE", reader.getText());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next()); // GE01

        // GE02 <TOO_LONG_AND_NOT_NUMERIC>
        assertEquals(EDIStreamEvent.ELEMENT_DATA_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG, reader.getErrorType());
        assertEquals("<TOO_LONG_AND_NOT_NUMERIC>", reader.getText());
        assertEquals(EDIStreamEvent.ELEMENT_DATA_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.INVALID_CHARACTER_DATA, reader.getErrorType());
        assertEquals("<TOO_LONG_AND_NOT_NUMERIC>", reader.getText());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next()); // GE02
        assertEquals("97", reader.getReferenceCode());

        // GE02 - 2nd occurrence
        assertEquals(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.TOO_MANY_REPETITIONS, reader.getErrorType());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next());
        assertEquals("0001", reader.getText());

        // GE02 - 3rd occurrence plus data error
        assertEquals(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR, reader.next());
        assertEquals("97", reader.getReferenceCode());
        assertEquals(EDIStreamValidationError.TOO_MANY_REPETITIONS, reader.getErrorType());
        assertEquals(EDIStreamEvent.ELEMENT_DATA_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.INVALID_CHARACTER_DATA, reader.getErrorType());
        assertEquals("AGAIN!", reader.getText()); // data association with error
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next());
        assertEquals("AGAIN!", reader.getText()); // here comes the element data
                                                  // event

        // GE03 too many elements
        assertEquals(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.TOO_MANY_DATA_ELEMENTS, reader.getErrorType());

        //assertEquals(EDIStreamEvent.END_GROUP, reader.nextTag());

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.nextTag());
        assertEquals("IEA", reader.getText());
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next()); // IEA01
        assertEquals(EDIStreamEvent.ELEMENT_DATA, reader.next()); // IEA02
        assertEquals(EDIStreamEvent.END_SEGMENT, reader.next()); // IEA
        assertEquals(EDIStreamEvent.END_INTERCHANGE, reader.next());
    }

    @Test
    @SuppressWarnings("resource")
    void testSegmentNameMatchesReferenceCode() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*01*0000000000*01*0000000000*ZZ*ABCDEFGHIJKLMNO*ZZ*123456789012345*101127*1719*`*00402*000003438*0*P*>\n" +
                "GS*HC*99999999999*888888888888*20111219*1340*1377*X*005010X222\n" +
                "ST*837*0001*005010X222\n" +
                "BHT*0019*00*565743*20110523*154959*CH\n" +
                "HL*1**20*1\n" +
                "SE*4*0001\n" +
                "GE*1*1377\n" +
                "IEA*1*000003438\n" +
                "").getBytes());

        EDIStreamReader unfiltered = factory.createEDIStreamReader(stream);
        EDIStreamReader reader = factory.createFilteredReader(unfiltered, new EDIStreamFilter() {
            @Override
            public boolean accept(EDIStreamReader reader) {
                switch (reader.getEventType()) {
                case START_TRANSACTION:
                case START_SEGMENT:
                    return true;
                default:
                    return false;
                }
            }
        });

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.nextTag());
        assertEquals("ISA", reader.getText());
        assertEquals("ISA", reader.getReferenceCode());
        assertEquals(EDIStreamEvent.START_SEGMENT, reader.nextTag());
        assertEquals("GS", reader.getText());
        assertEquals("GS", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next(), "Expecting start of transaction");
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        URL schemaLocation = getClass().getResource("/x12/005010X222/837.xml");
        Schema schema = schemaFactory.createSchema(schemaLocation);
        reader.setTransactionSchema(schema);

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.nextTag());
        assertEquals("ST", reader.getText());
        assertEquals("ST", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.nextTag());
        assertEquals("BHT", reader.getText());
        assertEquals("BHT", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.nextTag());
        assertEquals("HL", reader.getText());
        assertEquals("HL", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.nextTag());
        assertEquals("SE", reader.getText());
        assertEquals("SE", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.nextTag());
        assertEquals("GE", reader.getText());
        assertEquals("GE", reader.getReferenceCode());

        assertEquals(EDIStreamEvent.START_SEGMENT, reader.nextTag());
        assertEquals("IEA", reader.getText());
        assertEquals("IEA", reader.getReferenceCode());

        assertTrue(!reader.hasNext(), "Unexpected events exist");
    }

    private Schema loadX12FuncAckSchema() throws EDISchemaException {
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        URL schemaLocation = getClass().getResource("/x12/EDISchema997.xml");

        return schemaFactory.createSchema(schemaLocation);
    }
}
