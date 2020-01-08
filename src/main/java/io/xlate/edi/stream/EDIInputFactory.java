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
package io.xlate.edi.stream;

import java.io.InputStream;

import io.xlate.edi.internal.stream.StaEDIInputFactory;
import io.xlate.edi.schema.Schema;

public abstract class EDIInputFactory extends PropertySupport {

    public static final String EDI_VALIDATE_CONTROL_STRUCTURE = "io.xlate.edi.stream.EDI_VALIDATE_CONTROL_STRUCTURE";

    /**
     * Create a new instance of the factory. This static method creates a new
     * factory instance.
     *
     * Once an application has obtained a reference to an EDIInputFactory it
     * can use the factory to configure and obtain stream instances.
     *
     * @return the factory implementation
     */
    public static EDIInputFactory newFactory() {
        return new StaEDIInputFactory();
    }

    /**
     * Creates a new {@link EDIStreamReader} using the given {@link InputStream} (with default encoding).
     *
     * @param stream {@link InputStream} from which the EDI data will be read
     * @return a new {@link EDIStreamReader} which reads from the stream
     */
    public abstract EDIStreamReader createEDIStreamReader(InputStream stream);

    /**
     * Creates a new {@link EDIStreamReader} using the given {@link InputStream} and encoding.
     *
     * @param stream {@link InputStream} from which the EDI data will be read
     * @param encoding character encoding of the stream
     * @return a new {@link EDIStreamReader} which reads from the stream
     * @throws EDIStreamException when encoding is not supported
     */
    public abstract EDIStreamReader createEDIStreamReader(InputStream stream,
                                                          String encoding) throws EDIStreamException;

    /**
     * Creates a new {@link EDIStreamReader} using the given {@link InputStream} (with default encoding)
     * which uses the {@link Schema} for validation of the input's control structures
     * (interchange, group, transaction).
     *
     * Note that a separate schema for validation of messages/transactions may be passed directly to the
     * {@link EDIStreamReader} once the type and version of the messages is known.
     *
     * @param stream {@link InputStream} from which the EDI data will be read
     * @param schema {@link Schema} for control structure validation
     * @return a new {@link EDIStreamReader} which reads from the stream
     */
    public abstract EDIStreamReader createEDIStreamReader(InputStream stream, Schema schema);

    /**
     * Creates a new {@link EDIStreamReader} using the given {@link InputStream} and encoding
     * which uses the {@link Schema} for validation of the input's control structures
     * (interchange, group, transaction).
     *
     * Note that a separate schema for validation of messages/transactions may be passed directly to the
     * {@link EDIStreamReader} once the type and version of the messages is known.
     *
     * @param stream {@link InputStream} from which the EDI data will be read
     * @param encoding character encoding of the stream
     * @param schema {@link Schema} for control structure validation
     * @return a new {@link EDIStreamReader} which reads from the stream
     * @throws EDIStreamException when encoding is not supported
     */
    public abstract EDIStreamReader createEDIStreamReader(InputStream stream,
                                                          String encoding,
                                                          Schema schema) throws EDIStreamException;

    /**
     * Creates a new {@link EDIStreamReader} by wrapping the given reader with the
     * {@link EDIStreamFilter} filter.
     *
     * @param reader the reader to wrap
     * @param filter a filter to wrap the given reader
     * @return a new {@link EDIStreamReader} which uses filter
     */
    public abstract EDIStreamReader createFilteredReader(EDIStreamReader reader, EDIStreamFilter filter);

}
