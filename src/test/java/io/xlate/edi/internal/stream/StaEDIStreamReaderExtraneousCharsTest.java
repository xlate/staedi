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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamReader;

class StaEDIStreamReaderExtraneousCharsTest {

    static final Logger LOGGER = Logger.getGlobal();

    @SuppressWarnings("unchecked")
    List<Object>[] readFully(EDIInputFactory factory, String resource) throws IOException {
        EDIStreamReader reader = factory.createEDIStreamReader(getClass().getResourceAsStream(resource));
        List<Object> expected = new ArrayList<>();
        List<Object> unexpected = new ArrayList<>();

        try {
            while (reader.hasNext()) {
                switch (reader.next()) {
                case START_INTERCHANGE:
                    expected.add(reader.getEventType());
                    break;
                case SEGMENT_ERROR:
                case ELEMENT_OCCURRENCE_ERROR:
                case ELEMENT_DATA_ERROR:
                    LOGGER.log(Level.WARNING,
                               () -> reader.getErrorType() + ", " + reader.getLocation() + ", data: [" + reader.getText() + "]");
                    unexpected.add(reader.getErrorType());
                    break;
                case START_GROUP:
                case START_TRANSACTION:
                case START_LOOP:
                case START_SEGMENT:
                case END_GROUP:
                case END_TRANSACTION:
                case END_LOOP:
                case END_SEGMENT:
                case ELEMENT_DATA:
                    expected.add(reader.getEventType() + " [" + reader.getText() + "]");
                    break;
                default:
                    expected.add(reader.getEventType() + ", " + reader.getLocation());
                    break;
                }
            }
        } catch (Exception e) {
            unexpected.add(e);
            e.printStackTrace();
        } finally {
            reader.close();
        }

        return new List[] { expected, unexpected };
    }

    /**
     * Original issue: https://github.com/xlate/staedi/issues/128
     *
     * @throws Exception
     */
    @ParameterizedTest
    @ValueSource(
            strings = {
                        "/x12/issue128/ts210_80char.edi",
                        "/EDIFACT/issue128/wrapped_invoic_d97b_una.edi",
                        "/EDIFACT/issue128/wrapped_invoic_d97b.edi"
            })
    void testExtraneousCharactersIgnoredWithoutError(String resourceName) throws Exception {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        factory.setProperty(EDIInputFactory.EDI_IGNORE_EXTRANEOUS_CHARACTERS, "true");
        List<Object>[] results = readFully(factory, resourceName);
        List<Object> unexpected = results[1];
        assertEquals(0, unexpected.size(), () -> "Expected none, but got: " + unexpected);
    }

    @ParameterizedTest
    @CsvSource({
        "/EDIFACT/issue128/wrapped_invoic_d97b_una.edi, /EDIFACT/invoic_d97b_una.edi",
        "/EDIFACT/issue128/wrapped_invoic_d97b.edi, /EDIFACT/invoic_d97b.edi"
    })
    void testExtraneousCharactersRemovedMatchesOriginal(String wrapped, String original) throws Exception {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        List<Object>[] results1 = readFully(factory, original);

        factory.setProperty(EDIInputFactory.EDI_IGNORE_EXTRANEOUS_CHARACTERS, "true");
        List<Object>[] results2 = readFully(factory, wrapped);

        assertEquals(0, results1[1].size(), () -> "Expected none, but got: " + results1[1]);
        assertEquals(results1[0], results2[0]);
        assertEquals(results1[1], results2[1]);
    }
}
