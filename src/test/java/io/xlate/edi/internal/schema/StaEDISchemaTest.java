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
package io.xlate.edi.internal.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;
import java.util.Map;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.Test;

import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.EDIType;

@SuppressWarnings("resource")
public class StaEDISchemaTest {

    final String INTERCHANGE_V2 = "{http://xlate.io/EDISchema/v2}interchange";
    final String TRANSACTION_V2 = "{http://xlate.io/EDISchema/v2}transaction";

    final String INTERCHANGE_V3 = "{http://xlate.io/EDISchema/v3}interchange";
    final String TRANSACTION_V3 = "{http://xlate.io/EDISchema/v3}transaction";

    @Test
    public void testSetTypesNullTypes() {
        StaEDISchema schema = new StaEDISchema(INTERCHANGE_V2, TRANSACTION_V2);
        assertThrows(NullPointerException.class, () -> schema.setTypes(null));
    }

    @Test
    public void testRootTypeIsInterchange() throws EDISchemaException, XMLStreamException, FactoryConfigurationError {
        StaEDISchema schema = new StaEDISchema(INTERCHANGE_V3, TRANSACTION_V3);
        InputStream schemaStream = getClass().getResourceAsStream("/X12/v00200.xml");
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(schemaStream);
        reader.nextTag(); // Pass by <schema> element
        Map<String, EDIType> types = new SchemaReaderV3(reader).readTypes();
        schema.setTypes(types);

        assertEquals(EDIType.Type.INTERCHANGE, schema.getType(INTERCHANGE_V3).getType());
    }

    @Test
    public void testRootTypeIsInterchangeV3() throws EDISchemaException, XMLStreamException, FactoryConfigurationError {
        StaEDISchema schema = new StaEDISchema(INTERCHANGE_V3, TRANSACTION_V3);
        InputStream schemaStream = getClass().getResourceAsStream("/X12/v00402.xml");
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(schemaStream);
        reader.nextTag(); // Pass by <schema> element
        Map<String, EDIType> types = new SchemaReaderV3(reader).readTypes();
        schema.setTypes(types);

        assertEquals(EDIType.Type.INTERCHANGE, schema.getType(INTERCHANGE_V3).getType());
    }

    @Test
    public void testLoadV3TransactionMultipleSyntaxElements_EDIFACT_CONTRL() throws EDISchemaException, XMLStreamException, FactoryConfigurationError {
        StaEDISchema schema = new StaEDISchema(INTERCHANGE_V3, TRANSACTION_V3);
        InputStream schemaStream = getClass().getResourceAsStream("/EDIFACT/CONTRL-v4r02.xml");
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(schemaStream);
        reader.nextTag(); // Pass by <schema> element
        Map<String, EDIType> types = new SchemaReaderV3(reader).readTypes();
        schema.setTypes(types);

        assertEquals(EDIType.Type.TRANSACTION, schema.getType(TRANSACTION_V3).getType());
        assertEquals(4, ((EDIComplexType) schema.getType("UCF")).getSyntaxRules().size());
        assertEquals(4, ((EDIComplexType) schema.getType("UCI")).getSyntaxRules().size());
        assertEquals(7, ((EDIComplexType) schema.getType("UCM")).getSyntaxRules().size());
    }
}
