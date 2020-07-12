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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.Test;

import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.SchemaFactory;

@SuppressWarnings("resource")
class StaEDISchemaTest {

    @Test
    void testSetTypesNullTypes() {
        StaEDISchema schema = new StaEDISchema(StaEDISchema.INTERCHANGE_ID, StaEDISchema.TRANSACTION_ID);
        assertThrows(NullPointerException.class, () -> schema.setTypes(null));
    }

    @Test
    void testRootTypeIsInterchange_00200() throws Exception {
        StaEDISchema schema = new StaEDISchema(StaEDISchema.INTERCHANGE_ID, StaEDISchema.TRANSACTION_ID);
        URL schemaLocation = getClass().getResource("/X12/v00200.xml");
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(schemaLocation.openStream());
        reader.nextTag(); // Pass by <schema> element
        Map<String, EDIType> types = new SchemaReaderV4(reader,
                                                        Collections.singletonMap(SchemaFactory.SCHEMA_LOCATION_URL_CONTEXT,
                                                                                 schemaLocation.toURI().resolve(".").toURL())).readTypes();
        schema.setTypes(types);

        assertEquals(EDIType.Type.INTERCHANGE, schema.getType(StaEDISchema.INTERCHANGE_ID).getType());
    }

    @Test
    void testRootTypeIsInterchange_00402() throws Exception {
        StaEDISchema schema = new StaEDISchema(StaEDISchema.INTERCHANGE_ID, StaEDISchema.TRANSACTION_ID);
        URL schemaLocation = getClass().getResource("/X12/v00402.xml");
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(schemaLocation.openStream());
        reader.nextTag(); // Pass by <schema> element
        Map<String, EDIType> types = new SchemaReaderV4(reader,
                                                        Collections.singletonMap(SchemaFactory.SCHEMA_LOCATION_URL_CONTEXT,
                                                                                 schemaLocation.toURI().resolve(".").toURL())).readTypes();
        schema.setTypes(types);

        assertEquals(EDIType.Type.INTERCHANGE, schema.getType(StaEDISchema.INTERCHANGE_ID).getType());
    }

    @Test
    void testLoadV3TransactionMultipleSyntaxElements_EDIFACT_CONTRL()
            throws EDISchemaException, XMLStreamException, FactoryConfigurationError {
        StaEDISchema schema = new StaEDISchema(StaEDISchema.INTERCHANGE_ID, StaEDISchema.TRANSACTION_ID);
        InputStream schemaStream = getClass().getResourceAsStream("/EDIFACT/CONTRL-v4r02.xml");
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(schemaStream);
        reader.nextTag(); // Pass by <schema> element
        Map<String, EDIType> types = new SchemaReaderV4(reader, Collections.emptyMap()).readTypes();
        schema.setTypes(types);

        assertEquals(EDIType.Type.TRANSACTION, schema.getType(StaEDISchema.TRANSACTION_ID).getType());
        assertEquals(4, ((EDIComplexType) schema.getType("UCF")).getSyntaxRules().size());
        assertEquals(4, ((EDIComplexType) schema.getType("UCI")).getSyntaxRules().size());
        assertEquals(7, ((EDIComplexType) schema.getType("UCM")).getSyntaxRules().size());
    }

    @Test
    void testLoadV4_TestSimpleTypes() throws EDISchemaException, XMLStreamException, FactoryConfigurationError {
        StaEDISchema schema = new StaEDISchema(StaEDISchema.INTERCHANGE_ID, StaEDISchema.TRANSACTION_ID);
        InputStream schemaStream = getClass().getResourceAsStream("/x12/IG-999-standard-included.xml");
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(schemaStream);
        reader.nextTag(); // Pass by <schema> element
        Map<String, EDIType> types = new SchemaReaderV4(reader, Collections.emptyMap()).readTypes();
        schema.setTypes(types);

        assertEquals(EDIType.Type.TRANSACTION, schema.getType(StaEDISchema.TRANSACTION_ID).getType());

        EDIType ak101 = schema.getType("DE0479");
        assertEquals(EDIType.Type.ELEMENT, ak101.getType());
        assertEquals(ak101, schema.getType("DE0479"));

        EDIType ak901 = schema.getType("DE0715");
        assertEquals(EDIType.Type.ELEMENT, ak901.getType());
        assertNotEquals(ak101, ak901);

        EDIType ak9 = schema.getType("AK9");
        assertEquals(EDIType.Type.SEGMENT, ak9.getType());
        assertNotEquals(ak101, ak9);
    }
}
