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
package io.xlate.edi.stream;

import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.schema.SchemaUtils;
import io.xlate.edi.stream.EDIStreamConstants.Events;

public class ErrorEventsTest {

    EDIStreamFilter errorFilter = new EDIStreamFilter() {
        @Override
        public boolean accept(EDIStreamReader reader) {
            switch (reader.getEventType()) {
            case Events.SEGMENT_ERROR:
            case Events.ELEMENT_DATA_ERROR:
            case Events.ELEMENT_OCCURRENCE_ERROR:
                return true;
            }
            return false;
        }
    };

    @SuppressWarnings("resource")
    @Test
    public void testInvalidElements1() throws EDIStreamException, EDISchemaException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getClassLoader().getResourceAsStream("x12/extraDelimiter997.edi");
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema control = SchemaUtils.getControlSchema("X12", new String[] { "00501" });
        Schema transaction = schemaFactory.createSchema(SchemaUtils.getURL("x12/EDISchema997.xml"));
        EDIStreamReader reader = factory.createEDIStreamReader(stream, control);
        String segment = null;

        prescan: while (reader.hasNext()) {
            switch (reader.next()) {
            case Events.START_SEGMENT: {
                segment = reader.getText();
                break;
            }
            case Events.END_SEGMENT:
                if ("ST".equals(segment)) {
                    reader.addSchema(transaction);
                    break prescan;
                }
                break;
            default:
                break;
            }
        }

        reader = factory.createFilteredReader(reader, errorFilter);

        Assert.assertEquals(Events.ELEMENT_DATA_ERROR, reader.next());
        Assert.assertEquals(EDIStreamValidationError.INVALID_CHARACTER_DATA, reader.getErrorType());
        Assert.assertEquals("AK302-R1", reader.getText());
        Assert.assertEquals(2, reader.getLocation().getElementPosition());
        Assert.assertEquals(1, reader.getLocation().getElementOccurrence());
        Assert.assertEquals(-1, reader.getLocation().getComponentPosition());

        Assert.assertEquals(Events.ELEMENT_OCCURRENCE_ERROR, reader.next());
        Assert.assertEquals(EDIStreamValidationError.TOO_MANY_REPETITIONS, reader.getErrorType());
        Assert.assertEquals(2, reader.getLocation().getElementPosition());
        Assert.assertEquals(2, reader.getLocation().getElementOccurrence());
        Assert.assertEquals(-1, reader.getLocation().getComponentPosition());

        Assert.assertEquals(Events.ELEMENT_DATA_ERROR, reader.next());
        Assert.assertEquals(EDIStreamValidationError.INVALID_CHARACTER_DATA, reader.getErrorType());
        Assert.assertEquals("AK302-R2", reader.getText());
        Assert.assertEquals(2, reader.getLocation().getElementPosition());
        Assert.assertEquals(2, reader.getLocation().getElementOccurrence());
        Assert.assertEquals(-1, reader.getLocation().getComponentPosition());

        Assert.assertEquals(Events.ELEMENT_OCCURRENCE_ERROR, reader.next());
        Assert.assertEquals(EDIStreamValidationError.TOO_MANY_REPETITIONS, reader.getErrorType());
        Assert.assertEquals(2, reader.getLocation().getElementPosition());
        Assert.assertEquals(3, reader.getLocation().getElementOccurrence());
        Assert.assertEquals(-1, reader.getLocation().getComponentPosition());

        Assert.assertEquals(Events.ELEMENT_OCCURRENCE_ERROR, reader.next());
        Assert.assertEquals(EDIStreamValidationError.TOO_MANY_COMPONENTS, reader.getErrorType());
        Assert.assertEquals(2, reader.getLocation().getElementPosition());
        Assert.assertEquals(3, reader.getLocation().getElementOccurrence());
        Assert.assertEquals(1, reader.getLocation().getComponentPosition());

        Assert.assertEquals(Events.ELEMENT_OCCURRENCE_ERROR, reader.next());
        Assert.assertEquals(EDIStreamValidationError.TOO_MANY_COMPONENTS, reader.getErrorType());
        Assert.assertEquals(2, reader.getLocation().getElementPosition());
        Assert.assertEquals(3, reader.getLocation().getElementOccurrence());
        Assert.assertEquals(2, reader.getLocation().getComponentPosition());

        Assert.assertEquals(Events.ELEMENT_DATA_ERROR, reader.next());
        Assert.assertEquals(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG, reader.getErrorType());
        Assert.assertEquals("AK304-R1", reader.getText());
        Assert.assertEquals(4, reader.getLocation().getElementPosition());
        Assert.assertEquals(1, reader.getLocation().getElementOccurrence());
        Assert.assertEquals(-1, reader.getLocation().getComponentPosition());

        Assert.assertEquals(Events.ELEMENT_DATA_ERROR, reader.next());
        Assert.assertEquals(EDIStreamValidationError.INVALID_CODE_VALUE, reader.getErrorType());
        Assert.assertEquals("AK304-R1", reader.getText());
        Assert.assertEquals(4, reader.getLocation().getElementPosition());
        Assert.assertEquals(1, reader.getLocation().getElementOccurrence());
        Assert.assertEquals(-1, reader.getLocation().getComponentPosition());

        Assert.assertEquals(Events.ELEMENT_OCCURRENCE_ERROR, reader.next());
        Assert.assertEquals(EDIStreamValidationError.TOO_MANY_REPETITIONS, reader.getErrorType());
        Assert.assertEquals(4, reader.getLocation().getElementPosition());
        Assert.assertEquals(2, reader.getLocation().getElementOccurrence());
        Assert.assertEquals(-1, reader.getLocation().getComponentPosition());

        Assert.assertEquals(Events.ELEMENT_DATA_ERROR, reader.next());
        Assert.assertEquals(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG, reader.getErrorType());
        Assert.assertEquals("AK304-R2", reader.getText());
        Assert.assertEquals(4, reader.getLocation().getElementPosition());
        Assert.assertEquals(2, reader.getLocation().getElementOccurrence());
        Assert.assertEquals(-1, reader.getLocation().getComponentPosition());

        Assert.assertEquals(Events.ELEMENT_DATA_ERROR, reader.next());
        Assert.assertEquals(EDIStreamValidationError.INVALID_CODE_VALUE, reader.getErrorType());
        Assert.assertEquals("AK304-R2", reader.getText());
        Assert.assertEquals(4, reader.getLocation().getElementPosition());
        Assert.assertEquals(2, reader.getLocation().getElementOccurrence());
        Assert.assertEquals(-1, reader.getLocation().getComponentPosition());

        Assert.assertEquals(Events.ELEMENT_OCCURRENCE_ERROR, reader.next());
        Assert.assertEquals(EDIStreamValidationError.TOO_MANY_REPETITIONS, reader.getErrorType());
        Assert.assertEquals(4, reader.getLocation().getElementPosition());
        Assert.assertEquals(3, reader.getLocation().getElementOccurrence());
        Assert.assertEquals(-1, reader.getLocation().getComponentPosition());

        Assert.assertEquals(Events.ELEMENT_DATA_ERROR, reader.next());
        Assert.assertEquals(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG, reader.getErrorType());
        Assert.assertEquals("AK304-R3", reader.getText());
        Assert.assertEquals(4, reader.getLocation().getElementPosition());
        Assert.assertEquals(3, reader.getLocation().getElementOccurrence());
        Assert.assertEquals(-1, reader.getLocation().getComponentPosition());

        Assert.assertEquals(Events.ELEMENT_DATA_ERROR, reader.next());
        Assert.assertEquals(EDIStreamValidationError.INVALID_CODE_VALUE, reader.getErrorType());
        Assert.assertEquals("AK304-R3", reader.getText());
        Assert.assertEquals(4, reader.getLocation().getElementPosition());
        Assert.assertEquals(3, reader.getLocation().getElementOccurrence());
        Assert.assertEquals(-1, reader.getLocation().getComponentPosition());
    }
}
