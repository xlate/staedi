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

import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamReader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StaEDIStreamReaderTransactionInformationTest {

    @SuppressWarnings("unchecked")
    List<Object>[] readFully(EDIInputFactory factory, String resource) {
        List<Object> expected = new ArrayList<>();
        List<Object> unexpected = new ArrayList<>();

        try (EDIStreamReader reader = factory.createEDIStreamReader(getClass().getResourceAsStream(resource))) {
            while (reader.hasNext()) {
                if (reader.next() == EDIStreamEvent.START_TRANSACTION) {
                    expected.add(reader.getTransactionType());
                }
            }
        } catch (Exception e) {
            unexpected.add(e);
            e.printStackTrace();
        }

        return new List[] { expected, unexpected };
    }

    /**
     * Original issue: https://github.com/xlate/staedi/issues/590
     */
    @Test
    void testMultiTXTransactionTypeWithoutError() {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        List<Object>[] results = readFully(factory, "/x12/issue590/multi-tx-transaction-type.edi");
        List<Object> expected = results[0];
        List<Object> unexpected = results[1];

        assertEquals(0, unexpected.size(), () -> "Expected none, but got: " + unexpected);
        assertEquals("855", expected.get(0));
        assertEquals("855", expected.get(1));
    }
}
