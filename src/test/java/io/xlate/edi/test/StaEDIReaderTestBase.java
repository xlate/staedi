package io.xlate.edi.test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;

import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamReader;

public abstract class StaEDIReaderTestBase {

    protected Map<String, Object> ediReaderConfig;
    protected EDIInputFactory ediInputFactory;
    protected EDIStreamReader ediReader;

    @BeforeEach
    void setupFactory() {
        ediReaderConfig = new HashMap<>();
        ediInputFactory = EDIInputFactory.newFactory();
    }

    protected void setupReader(InputStream ediStream, String schemaResource) throws Exception {
        ediReaderConfig.forEach(ediInputFactory::setProperty);
        ediReader = ediInputFactory.createEDIStreamReader(ediStream);

        if (schemaResource != null) {
            SchemaFactory schemaFactory = SchemaFactory.newFactory();
            Schema transactionSchema = schemaFactory.createSchema(getClass().getResource(schemaResource));
            ediReader = ediInputFactory.createFilteredReader(ediReader, reader -> {
                if (reader.getEventType() == EDIStreamEvent.START_TRANSACTION) {
                    reader.setTransactionSchema(transactionSchema);
                }
                return true;
            });
        }
    }

    protected void setupReader(String ediResource, String schemaResource) throws Exception {
        setupReader(getClass().getResourceAsStream(ediResource), schemaResource);
    }

    protected void setupReader(byte[] ediResource, String schemaResource) throws Exception {
        setupReader(new ByteArrayInputStream(ediResource), schemaResource);
    }

}
