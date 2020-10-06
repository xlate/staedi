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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamReader;

class StaEDIStreamReaderExtraneousCharsTest {

    Logger LOGGER = Logger.getGlobal();

    /**
     * Original issue: https://github.com/xlate/staedi/issues/128
     *
     * @throws Exception
     */
    @Test
    void testValidatorLookAhead_Issue122() throws Exception {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        factory.setProperty(EDIInputFactory.EDI_IGNORE_EXTRANEOUS_CHARACTERS, "true");
        EDIStreamReader reader = factory.createEDIStreamReader(getClass().getResourceAsStream("/x12/issue128/ts210_80char.edi"));
        List<Object> unexpected = new ArrayList<>();

        try {
            while (reader.hasNext()) {
                switch (reader.next()) {
                case SEGMENT_ERROR:
                case ELEMENT_OCCURRENCE_ERROR:
                case ELEMENT_DATA_ERROR:
                    LOGGER.log(Level.WARNING, () -> reader.getErrorType() + ", " + reader.getLocation() + ", data: [" + reader.getText() + "]");
                    unexpected.add(reader.getErrorType());
                    break;
                default:
                    break;
                }
            }
        } catch (Exception e) {
            unexpected.add(e);
            e.printStackTrace();
        } finally {
            reader.close();
        }

        assertEquals(0, unexpected.size(), () -> "Expected none, but got: " + unexpected);
    }

}
