package io.xlate.edi.internal.wiki;

// import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonGenerator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIStreamValidationError;

class ReadFuncAcknowledgementTest {

    @SuppressWarnings("unused")
    @Test
    void testReadFuncAcknowledgement() throws Exception {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        JsonObject result = null;

        //InputStream stream = new FileInputStream("simple997.edi");
        try (InputStream stream = getClass().getResource("/x12/simple997.edi").openStream();
                EDIStreamReader reader = factory.createEDIStreamReader(stream)) {

            EDIStreamEvent event;
            boolean transactionBeginSegment = false;
            Deque<JsonObjectBuilder> buildStack = new ArrayDeque<>();
            JsonObjectBuilder builder = null;
            JsonArrayBuilder segmentBuilder = null;
            JsonArrayBuilder compositeBuilder = null;

            while (reader.hasNext()) {
                event = reader.next();

                switch (event) {
                case START_INTERCHANGE:
                    builder = Json.createObjectBuilder();
                    buildStack.offer(builder);
                    break;
                case END_INTERCHANGE:
                    result = builder.build();
                    break;

                case START_GROUP:
                    builder = Json.createObjectBuilder();
                    buildStack.offer(builder);
                    break;
                case END_GROUP:
                    JsonObjectBuilder groupBuilder = buildStack.removeLast();
                    builder = buildStack.peekLast();
                    builder.add(reader.getReferenceCode(), groupBuilder);
                    break;

                case START_TRANSACTION:
                    builder = Json.createObjectBuilder();
                    buildStack.offer(builder);
                    transactionBeginSegment = true;
                    break;
                case END_TRANSACTION:
                    JsonObjectBuilder transactionBuilder = buildStack.removeLast();
                    builder = buildStack.peekLast();
                    builder.add(reader.getReferenceCode(), transactionBuilder);
                    break;

                case START_LOOP:
                    builder = Json.createObjectBuilder();
                    buildStack.offer(builder);
                    break;
                case END_LOOP:
                    JsonObjectBuilder loopBuilder = buildStack.removeLast();
                    builder = buildStack.peekLast();
                    builder.add(reader.getReferenceCode(), loopBuilder);
                    break;

                case START_SEGMENT:
                    segmentBuilder = Json.createArrayBuilder();
                    break;

                case END_SEGMENT:
                    if (transactionBeginSegment) {
                        SchemaFactory schemaFactory = SchemaFactory.newFactory();
                        Schema schema = schemaFactory.createSchema(getClass().getResource("/x12/EDISchema997.xml"));
                        reader.setTransactionSchema(schema);
                    }
                    transactionBeginSegment = false;
                    builder.add(reader.getText(), segmentBuilder);
                    segmentBuilder = null;
                    break;

                case START_COMPOSITE:
                    compositeBuilder = Json.createArrayBuilder();
                    break;
                case END_COMPOSITE:
                    segmentBuilder.add(compositeBuilder);
                    compositeBuilder = null;
                    break;

                case ELEMENT_DATA:
                    if (compositeBuilder != null) {
                        compositeBuilder.add(reader.getText());
                    } else {
                        segmentBuilder.add(reader.getText());
                    }
                    break;

                case SEGMENT_ERROR:
                    // Handle a segment error
                    EDIStreamValidationError segmentErrorType = reader.getErrorType();
                    // ...
                    break;

                case ELEMENT_OCCURRENCE_ERROR:
                case ELEMENT_DATA_ERROR:
                    // Handle a segment error
                    EDIStreamValidationError elementErrorType = reader.getErrorType();
                    // ...
                    break;

                default:
                    break;
                }
            }
        }

        Assertions.assertNotNull(result);

        Json.createGeneratorFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, "true"))
            .createGenerator(System.out)
            .write(result)
            .close();
    }
}
