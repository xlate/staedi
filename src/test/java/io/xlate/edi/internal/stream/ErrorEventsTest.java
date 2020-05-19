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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamFilter;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIStreamValidationError;

@SuppressWarnings("resource")
public class ErrorEventsTest {

    EDIStreamFilter errorFilter = new EDIStreamFilter() {
        @Override
        public boolean accept(EDIStreamReader reader) {
            switch (reader.getEventType()) {
            case SEGMENT_ERROR:
            case ELEMENT_DATA_ERROR:
            case ELEMENT_OCCURRENCE_ERROR:
                return true;
            default:
                break;
            }
            return false;
        }
    };

    @Test
    void testInvalidElements1() throws EDIStreamException, EDISchemaException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/extraDelimiter997.edi");
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema control = SchemaUtils.getControlSchema("X12", new String[] { "00501" });
        Schema transaction = schemaFactory.createSchema(getClass().getResourceAsStream("/x12/EDISchema997.xml"));
        EDIStreamReader reader = factory.createEDIStreamReader(stream, control);

        prescan: while (reader.hasNext()) {
            switch (reader.next()) {
            case START_TRANSACTION:
                reader.setTransactionSchema(transaction);
                break prescan;
            default:
                break;
            }
        }

        reader = factory.createFilteredReader(reader, errorFilter);

        assertEquals(EDIStreamEvent.ELEMENT_DATA_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.INVALID_CHARACTER_DATA, reader.getErrorType());
        assertEquals("AK302-R1", reader.getText());
        assertEquals(2, reader.getLocation().getElementPosition());
        assertEquals(1, reader.getLocation().getElementOccurrence());
        assertEquals(-1, reader.getLocation().getComponentPosition());

        assertEquals(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.TOO_MANY_REPETITIONS, reader.getErrorType());
        assertEquals(2, reader.getLocation().getElementPosition());
        assertEquals(2, reader.getLocation().getElementOccurrence());
        assertEquals(-1, reader.getLocation().getComponentPosition());

        assertEquals(EDIStreamEvent.ELEMENT_DATA_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.INVALID_CHARACTER_DATA, reader.getErrorType());
        assertEquals("AK302-R2", reader.getText());
        assertEquals(2, reader.getLocation().getElementPosition());
        assertEquals(2, reader.getLocation().getElementOccurrence());
        assertEquals(-1, reader.getLocation().getComponentPosition());

        assertEquals(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.TOO_MANY_REPETITIONS, reader.getErrorType());
        assertEquals(2, reader.getLocation().getElementPosition());
        assertEquals(3, reader.getLocation().getElementOccurrence());
        assertEquals(-1, reader.getLocation().getComponentPosition());

        assertEquals(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.TOO_MANY_COMPONENTS, reader.getErrorType());
        assertEquals(2, reader.getLocation().getElementPosition());
        assertEquals(3, reader.getLocation().getElementOccurrence());
        assertEquals(1, reader.getLocation().getComponentPosition());

        assertEquals(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.TOO_MANY_COMPONENTS, reader.getErrorType());
        assertEquals(2, reader.getLocation().getElementPosition());
        assertEquals(3, reader.getLocation().getElementOccurrence());
        assertEquals(2, reader.getLocation().getComponentPosition());

        assertEquals(EDIStreamEvent.ELEMENT_DATA_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG, reader.getErrorType());
        assertEquals("AK304-R1", reader.getText());
        assertEquals(4, reader.getLocation().getElementPosition());
        assertEquals(1, reader.getLocation().getElementOccurrence());
        assertEquals(-1, reader.getLocation().getComponentPosition());

        assertEquals(EDIStreamEvent.ELEMENT_DATA_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.INVALID_CODE_VALUE, reader.getErrorType());
        assertEquals("AK304-R1", reader.getText());
        assertEquals(4, reader.getLocation().getElementPosition());
        assertEquals(1, reader.getLocation().getElementOccurrence());
        assertEquals(-1, reader.getLocation().getComponentPosition());

        assertEquals(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.TOO_MANY_REPETITIONS, reader.getErrorType());
        assertEquals(4, reader.getLocation().getElementPosition());
        assertEquals(2, reader.getLocation().getElementOccurrence());
        assertEquals(-1, reader.getLocation().getComponentPosition());

        assertEquals(EDIStreamEvent.ELEMENT_DATA_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG, reader.getErrorType());
        assertEquals("AK304-R2", reader.getText());
        assertEquals(4, reader.getLocation().getElementPosition());
        assertEquals(2, reader.getLocation().getElementOccurrence());
        assertEquals(-1, reader.getLocation().getComponentPosition());

        assertEquals(EDIStreamEvent.ELEMENT_DATA_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.INVALID_CODE_VALUE, reader.getErrorType());
        assertEquals("AK304-R2", reader.getText());
        assertEquals(4, reader.getLocation().getElementPosition());
        assertEquals(2, reader.getLocation().getElementOccurrence());
        assertEquals(-1, reader.getLocation().getComponentPosition());

        assertEquals(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.TOO_MANY_REPETITIONS, reader.getErrorType());
        assertEquals(4, reader.getLocation().getElementPosition());
        assertEquals(3, reader.getLocation().getElementOccurrence());
        assertEquals(-1, reader.getLocation().getComponentPosition());

        assertEquals(EDIStreamEvent.ELEMENT_DATA_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG, reader.getErrorType());
        assertEquals("AK304-R3", reader.getText());
        assertEquals(4, reader.getLocation().getElementPosition());
        assertEquals(3, reader.getLocation().getElementOccurrence());
        assertEquals(-1, reader.getLocation().getComponentPosition());

        assertEquals(EDIStreamEvent.ELEMENT_DATA_ERROR, reader.next());
        assertEquals(EDIStreamValidationError.INVALID_CODE_VALUE, reader.getErrorType());
        assertEquals("AK304-R3", reader.getText());
        assertEquals(4, reader.getLocation().getElementPosition());
        assertEquals(3, reader.getLocation().getElementOccurrence());
        assertEquals(-1, reader.getLocation().getComponentPosition());
    }

    @Test
    void testListSyntaxValid() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "UNB+UNOA:4:::02+005435656:1+006415160:1+20060515:1434+00000000000778'"
                + "UNG+INVOIC+15623+23457+20060515:1433+CD1352+UN+D:97B+A3P52'"
                + "UNH+00000000000117+INVOIC:D:97B:UN'"
                + "UNT+2+00000000000117'"
                + "UNE+1+CD1352'"
                + "UNZ+1+00000000000778'").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        reader = factory.createFilteredReader(reader, errorFilter);

        assertFalse(reader.hasNext(), "Unexpected errors");
    }

    @Test
    void testListSyntaxMissingFirst() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "UNB+UNOA:4:::02+005435656:1+006415160:1+20060515:1434+00000000000778'"
                + "UNG++15623+23457+20060515:1433+CD1352+UN+D:97B+A3P52'"
                + "UNH+00000000000117+INVOIC:D:97B:UN'"
                + "UNT+2+00000000000117'"
                + "UNE+1+CD1352'"
                + "UNZ+1+00000000000778'").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        reader = factory.createFilteredReader(reader, errorFilter);

        assertTrue(reader.hasNext(), "Expected errors not found");
        reader.next();
        assertEquals(EDIStreamValidationError.CONDITIONAL_REQUIRED_DATA_ELEMENT_MISSING, reader.getErrorType());
        assertEquals(2, reader.getLocation().getSegmentPosition());
        assertEquals(1, reader.getLocation().getElementPosition());

        assertTrue(!reader.hasNext(), "Unexpected errors exist");
    }

    @Test
    void testListSyntaxMissingSecond() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "UNB+UNOA:4:::02+005435656:1+006415160:1+20060515:1434+00000000000778'"
                + "UNG+INVOIC+15623+23457+20060515:1433+CD1352++D:97B+A3P52'"
                + "UNH+00000000000117+INVOIC:D:97B:UN'"
                + "UNT+2+00000000000117'"
                + "UNE+1+CD1352'"
                + "UNZ+1+00000000000778'").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        reader = factory.createFilteredReader(reader, errorFilter);

        assertTrue(reader.hasNext(), "Expected errors not found");
        reader.next();
        assertEquals(EDIStreamValidationError.CONDITIONAL_REQUIRED_DATA_ELEMENT_MISSING, reader.getErrorType());
        assertEquals(2, reader.getLocation().getSegmentPosition());
        assertEquals(6, reader.getLocation().getElementPosition());

        assertTrue(!reader.hasNext(), "Unexpected errors exist");
    }

    @Test
    void testTooManyOccurrencesComposite() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "UNB+UNOA:4:::02+005435656:1+006415160:1+20060515:1434+00000000000778'"
                + "UNG+INVOIC+15623+23457+20060515:1433+CD1352+UN+D:97B+A3P52'"
                + "UNH+00000000000117+INVOIC:D:97B:UN+FIRST OK*TOO MANY REPETITIONS'"
                + "UNT+2+00000000000117'"
                + "UNE+1+CD1352'"
                + "UNZ+1+00000000000778'").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        reader = factory.createFilteredReader(reader, errorFilter);

        assertTrue(reader.hasNext(), "Expected errors not found");
        reader.next();
        assertEquals(EDIStreamValidationError.TOO_MANY_REPETITIONS, reader.getErrorType());
        assertEquals(3, reader.getLocation().getSegmentPosition());
        assertEquals(3, reader.getLocation().getElementPosition());
        assertEquals(2, reader.getLocation().getElementOccurrence());

        assertTrue(!reader.hasNext(), "Unexpected errors exist");
    }

    @Test
    void testTooManyElementsComposite() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "UNB+UNOA:4:::02+005435656:1+006415160:1+20060515:1434+00000000000778'"
                + "UNG+INVOIC+15623+23457+20060515:1433+CD1352+UN+D:97B+A3P52'"
                + "UNH+00000000000117+INVOIC:D:97B:UN++++++MY:EXTRA:COMPOSITE'"
                + "UNT+2+00000000000117'"
                + "UNE+1+CD1352'"
                + "UNZ+1+00000000000778'").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        reader = factory.createFilteredReader(reader, errorFilter);

        assertTrue(reader.hasNext(), "Expected errors not found");
        reader.next();
        assertEquals(EDIStreamValidationError.TOO_MANY_DATA_ELEMENTS, reader.getErrorType());
        assertEquals(3, reader.getLocation().getSegmentPosition());
        assertEquals(8, reader.getLocation().getElementPosition());

        assertTrue(!reader.hasNext(), "Unexpected errors exist");
    }

    @Test
    void testTooManySimpleElements() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "UNB+UNOA:4:::02+005435656:1+006415160:1+20060515:1434+00000000000778'"
                + "UNG+INVOIC+15623+23457+20060515:1433+CD1352+UN+D:97B+A3P52'"
                + "UNH+00000000000117+INVOIC:D:97B:UN++++++MY_EXTRA_SIMPLE_ELEMENT'"
                + "UNT+2+00000000000117'"
                + "UNE+1+CD1352'"
                + "UNZ+1+00000000000778'").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        reader = factory.createFilteredReader(reader, errorFilter);

        assertTrue(reader.hasNext(), "Expected errors not found");
        reader.next();
        assertEquals(EDIStreamValidationError.TOO_MANY_DATA_ELEMENTS, reader.getErrorType());
        assertEquals(3, reader.getLocation().getSegmentPosition());
        assertEquals(8, reader.getLocation().getElementPosition());

        assertTrue(!reader.hasNext(), "Unexpected errors exist");
    }

    @Test
    void testValidEmptySegment() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "S01*X~"
                + "ETY~"
                + "S11*X~"
                + "S12*X~"
                + "S19*X~"
                + "S09*X~"
                + "IEA*1*508121953~").getBytes());

        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        URL schemaLocation = getClass().getResource("/x12/EDISchemaSegmentValidation.xml");
        Schema schema = schemaFactory.createSchema(schemaLocation);

        EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);
        reader = factory.createFilteredReader(reader, (r) -> {
            switch (r.getEventType()) {
            case SEGMENT_ERROR:
            case ELEMENT_DATA_ERROR:
            case ELEMENT_OCCURRENCE_ERROR:
            case START_TRANSACTION:
                return true;
            default:
                break;
            }
            return false;
        });

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next(), "Expecting start of transaction");
        reader.setTransactionSchema(schemaFactory.createSchema(getClass().getResource("/x12/EDISchemaSegmentValidationTx.xml")));
        assertTrue(!reader.hasNext(), "Unexpected errors exist");
    }

    @Test
    void testEmptySegmentSchemaWithData() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "S01*X~"
                + "ETY*DATA_SHOULD_NOT_BE_HERE~"
                + "S11*X~"
                + "S12*X~"
                + "S19*X~"
                + "S09*X~"
                + "IEA*1*508121953~").getBytes());

        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        URL schemaLocation = getClass().getResource("/x12/EDISchemaSegmentValidation.xml");
        Schema schema = schemaFactory.createSchema(schemaLocation);

        EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);
        reader = factory.createFilteredReader(reader, (r) -> {
            switch (r.getEventType()) {
            case SEGMENT_ERROR:
            case ELEMENT_DATA_ERROR:
            case ELEMENT_OCCURRENCE_ERROR:
            case START_TRANSACTION:
                return true;
            default:
                break;
            }
            return false;
        });

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next(), "Expecting start of transaction");
        reader.setTransactionSchema(schemaFactory.createSchema(getClass().getResource("/x12/EDISchemaSegmentValidationTx.xml")));
        assertTrue(reader.hasNext(), "Expected error missing");
        assertEquals(EDIStreamValidationError.TOO_MANY_DATA_ELEMENTS, reader.getErrorType());
    }

    @Test
    void testExtraAnyElementAllowedInAK1() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "GS*FA*ReceiverDept*SenderDept*20050812*195335*000005*X*005010X230~"
                + "ST*997*0001~"
                + "AK1*HC*000001**ANY1*ANY2~"
                + "AK9*A*1*1*1~"
                + "SE*4*0001~"
                + "GE*1*000005~"
                + "IEA*1*508121953~").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        reader = factory.createFilteredReader(reader, (r) -> {
            switch (r.getEventType()) {
            case SEGMENT_ERROR:
            case ELEMENT_DATA_ERROR:
            case ELEMENT_OCCURRENCE_ERROR:
            case START_TRANSACTION: // To set the schema
            case START_COMPOSITE: // To ensure no composites signaled for "any" elements
                return true;
            default:
                break;
            }
            return false;
        });

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next(), "Expecting start of transaction");
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        URL schemaLocation = getClass().getResource("/x12/EDISchema997_support_any_elements.xml");
        Schema schema = schemaFactory.createSchema(schemaLocation);
        reader.setTransactionSchema(schema);
        assertFalse(reader.hasNext(), "Unexpected errors in transaction");
    }

    @Test
    void testTooManyExtraAnyElementAllowedInAK1() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "GS*FA*ReceiverDept*SenderDept*20050812*195335*000005*X*005010X230~"
                + "ST*997*0001~"
                + "AK1*HC*000001**ANY1*ANY2*ANY3~"
                + "AK9*A*1*1*1~"
                + "SE*4*0001~"
                + "GE*1*000005~"
                + "IEA*1*508121953~").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        reader = factory.createFilteredReader(reader, (r) -> {
            switch (r.getEventType()) {
            case SEGMENT_ERROR:
            case ELEMENT_DATA_ERROR:
            case ELEMENT_OCCURRENCE_ERROR:
            case START_TRANSACTION: // To set the schema
            case START_COMPOSITE: // To ensure no composites signaled for "any" elements
                return true;
            default:
                break;
            }
            return false;
        });

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next(), "Expecting start of transaction");
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        URL schemaLocation = getClass().getResource("/x12/EDISchema997_support_any_elements.xml");
        Schema schema = schemaFactory.createSchema(schemaLocation);
        reader.setTransactionSchema(schema);

        assertTrue(reader.hasNext(), "Expected error missing");
        assertEquals(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR, reader.getEventType());
        assertEquals(EDIStreamValidationError.TOO_MANY_DATA_ELEMENTS, reader.getErrorType());
        assertEquals("AK1", reader.getLocation().getSegmentTag());
        assertEquals(6, reader.getLocation().getElementPosition());
        assertEquals(1, reader.getLocation().getElementOccurrence());
        assertEquals(-1, reader.getLocation().getComponentPosition());
    }

    @Test
    void testNoExtraAnyElementInAK1() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "GS*FA*ReceiverDept*SenderDept*20050812*195335*000005*X*005010X230~"
                + "ST*997*0001~"
                + "AK1*HC*000001~"
                + "AK9*A*1*1*1~"
                + "SE*4*0001~"
                + "GE*1*000005~"
                + "IEA*1*508121953~").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        reader = factory.createFilteredReader(reader, (r) -> {
            switch (r.getEventType()) {
            case SEGMENT_ERROR:
            case ELEMENT_DATA_ERROR:
            case ELEMENT_OCCURRENCE_ERROR:
            case START_TRANSACTION: // To set the schema
            case START_COMPOSITE: // To ensure no composites signaled for "any" elements
                return true;
            default:
                break;
            }
            return false;
        });

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next(), "Expecting start of transaction");
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        URL schemaLocation = getClass().getResource("/x12/EDISchema997_support_any_elements.xml");
        Schema schema = schemaFactory.createSchema(schemaLocation);
        reader.setTransactionSchema(schema);

        assertFalse(reader.hasNext(), "Unexpected errors in transaction");
    }

    @Test
    void testCompositesSupportedInAnyElementInAK1() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "GS*FA*ReceiverDept*SenderDept*20050812*195335*000005*X*005010X230~"
                + "ST*997*0001~"
                + "AK1*HC*000001**ANY1:ANY2~"
                + "AK9*A*1*1*1~"
                + "SE*4*0001~"
                + "GE*1*000005~"
                + "IEA*1*508121953~").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        reader = factory.createFilteredReader(reader, (r) -> {
            switch (r.getEventType()) {
            case SEGMENT_ERROR:
            case ELEMENT_DATA_ERROR:
            case ELEMENT_OCCURRENCE_ERROR:
            case START_TRANSACTION: // To set the schema
                return true;
            default:
                break;
            }
            return false;
        });

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next(), "Expecting start of transaction");
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        URL schemaLocation = getClass().getResource("/x12/EDISchema997_support_any_elements.xml");
        Schema schema = schemaFactory.createSchema(schemaLocation);
        reader.setTransactionSchema(schema);

        assertFalse(reader.hasNext(), "Unexpected errors in transaction");
    }

    @Test
    void testRequiredComponentInC030InAnyElementInAK4() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "GS*FA*ReceiverDept*SenderDept*20050812*195335*000005*X*005010X230~"
                + "ST*997*0001~"
                + "AK1*HC*000001~"
                + "AK2*837*0021~"
                + "AK3*NM1*8**8~"
                + "AK4*8:::ANYCOMPONENT*66*7*MI~"
                + "AK5*R*5~"
                + "AK9*R*1*1*0~"
                + "SE*4*0001~"
                + "GE*1*000005~"
                + "IEA*1*508121953~").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        reader = factory.createFilteredReader(reader, (r) -> {
            switch (r.getEventType()) {
            case SEGMENT_ERROR:
            case ELEMENT_DATA_ERROR:
            case ELEMENT_OCCURRENCE_ERROR:
            case START_TRANSACTION: // To set the schema
                return true;
            default:
                break;
            }
            return false;
        });

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next(), "Expecting start of transaction");
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        URL schemaLocation = getClass().getResource("/x12/EDISchema997_support_any_elements.xml");
        Schema schema = schemaFactory.createSchema(schemaLocation);
        reader.setTransactionSchema(schema);

        assertFalse(reader.hasNext(), "Unexpected errors in transaction");
    }

    @Test
    void testMissingRequiredComponentInC030InAnyElementInAK4() throws EDISchemaException, EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~"
                + "GS*FA*ReceiverDept*SenderDept*20050812*195335*000005*X*005010X230~"
                + "ST*997*0001~"
                + "AK1*HC*000001~"
                + "AK2*837*0021~"
                + "AK3*NM1*8**8~"
                + "AK4*8:::*66*7*MI~"
                + "AK5*R*5~"
                + "AK9*R*1*1*0~"
                + "SE*4*0001~"
                + "GE*1*000005~"
                + "IEA*1*508121953~").getBytes());

        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        reader = factory.createFilteredReader(reader, (r) -> {
            switch (r.getEventType()) {
            case SEGMENT_ERROR:
            case ELEMENT_DATA_ERROR:
            case ELEMENT_OCCURRENCE_ERROR:
            case START_TRANSACTION: // To set the schema
                return true;
            default:
                break;
            }
            return false;
        });

        assertEquals(EDIStreamEvent.START_TRANSACTION, reader.next(), "Expecting start of transaction");
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        URL schemaLocation = getClass().getResource("/x12/EDISchema997_support_any_elements.xml");
        Schema schema = schemaFactory.createSchema(schemaLocation);
        reader.setTransactionSchema(schema);

        assertTrue(reader.hasNext(), "Expected error missing");
        assertEquals(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR, reader.getEventType());
        assertEquals(EDIStreamValidationError.REQUIRED_DATA_ELEMENT_MISSING, reader.getErrorType());
        assertEquals("AK4", reader.getLocation().getSegmentTag());
        assertEquals(1, reader.getLocation().getElementPosition());
        assertEquals(1, reader.getLocation().getElementOccurrence());
        assertEquals(4, reader.getLocation().getComponentPosition());

        assertFalse(reader.hasNext(), "Unexpected errors in transaction");
    }
}
