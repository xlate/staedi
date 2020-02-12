package io.xlate.edi.internal.schema;

import java.util.Map;

import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.EDIType;

public interface SchemaReader {

    Map<String, EDIType> readTypes() throws EDISchemaException;

    String getInterchangeName();

    String getTransactionName();

    String getImplementationName();

}
