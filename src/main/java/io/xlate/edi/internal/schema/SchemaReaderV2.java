package io.xlate.edi.internal.schema;

import javax.xml.stream.XMLStreamReader;

class SchemaReaderV2 extends SchemaReaderBase implements SchemaReader {

    public SchemaReaderV2(XMLStreamReader reader) {
        super(StaEDISchemaFactory.XMLNS_V2, reader);
    }

}
