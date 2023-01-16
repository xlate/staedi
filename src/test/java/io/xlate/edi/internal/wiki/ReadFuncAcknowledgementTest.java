package io.xlate.edi.internal.wiki;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

        // Any InputStream can be used to create an `EDIStreamReader`
        try (InputStream stream = getClass().getResource("/x12/simple997.edi").openStream();
             EDIStreamReader reader = factory.createEDIStreamReader(stream)) {

            EDIStreamEvent event;
            boolean transactionBeginSegment = false;
            Deque<JsonObjectBuilder> buildStack = new ArrayDeque<>();
            JsonObjectBuilder builder = null;
            JsonArrayBuilder transactions = null;

            Deque<Set<String>> activeLoops = new ArrayDeque<>();
            Set<String> peerLoops;

            Map<String, JsonArrayBuilder> loopBuilders = new HashMap<>();
            JsonArrayBuilder currentLoop;

            JsonArrayBuilder segmentBuilder = null;
            JsonArrayBuilder compositeBuilder = null;

            while (reader.hasNext()) {
                event = reader.next();

                switch (event) {
                case START_INTERCHANGE:
                    // Called at the beginning of the EDI stream once the X12 dialect is confirmed
                    builder = Json.createObjectBuilder();
                    buildStack.offer(builder);
                    break;
                case END_INTERCHANGE:
                    // Called following the IEA segment
                    result = builder.build();
                    break;

                case START_GROUP:
                    // Called prior to the start of the GS segment
                    builder = Json.createObjectBuilder();
                    buildStack.offer(builder);
                    transactions = Json.createArrayBuilder();
                    break;
                case END_GROUP:
                    // Called following the GE segment
                    JsonObjectBuilder groupBuilder = buildStack.removeLast();
                    groupBuilder.add("TRANSACTIONS", transactions);
                    builder = buildStack.peekLast();
                    builder.add(reader.getReferenceCode(), groupBuilder);
                    break;

                case START_TRANSACTION:
                    // Called prior to the start of the ST segment
                    builder = Json.createObjectBuilder();
                    buildStack.offer(builder);
                    // Set a boolean so that when the ST segment is complete, our code
                    // can set a transaction schema to use with the reader. This boolean
                    // allows the END_SEGMENT code to know the current context of the
                    // segment.
                    transactionBeginSegment = true;
                    break;
                case END_TRANSACTION:
                    // Called following the SE segment
                    JsonObjectBuilder transactionBuilder = buildStack.removeLast();
                    transactions.add(transactionBuilder);
                    builder = buildStack.peekLast();
                    break;

                case START_LOOP:
                    // Called before the start of the segment that begins a loop.
                    // The loop's `code` from the schema can be obtained by a call
                    // to `reader.getReferenceCode()`
                    builder = Json.createObjectBuilder();
                    buildStack.offer(builder);
                    break;
                case END_LOOP:
                    // Called following the end of the segment that ends a loop.
                    // The loop's `code` from the schema can be obtained by a call
                    // to `reader.getReferenceCode()`
                    JsonObjectBuilder loopBuilder = buildStack.removeLast();
                    builder = buildStack.peekLast();
                    builder.add(reader.getReferenceCode(), loopBuilder);
                    break;

                case START_SEGMENT:
                    segmentBuilder = Json.createArrayBuilder();
                    break;

                case END_SEGMENT:
                    if (transactionBeginSegment) {
                        /*
                         * At the end of the ST segment, load the schema for use to validate
                         * the transaction.
                         */
                        SchemaFactory schemaFactory = SchemaFactory.newFactory();
                        // Any InputStream or URL can be used to create a `Schema`
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
