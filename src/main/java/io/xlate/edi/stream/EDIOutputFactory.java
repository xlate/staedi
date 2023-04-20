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

import java.io.OutputStream;

import javax.xml.stream.XMLStreamWriter;

import io.xlate.edi.schema.Schema;

public abstract class EDIOutputFactory extends PropertySupport {

    /**
     * <p>
     * When set to true, the EDI output will have a platform specific line
     * separator written after each segment terminator.
     *
     * <p>
     * Default value is false.
     */
    public static final String PRETTY_PRINT = "io.xlate.edi.stream.PRETTY_PRINT";

    /**
     * <p>
     * When set to true, empty trailing elements in a segment and empty trailing
     * components in a composite element will be truncated. I.e, they will not
     * be written to the output.
     *
     * <p>
     * Default value is false.
     */
    public static final String TRUNCATE_EMPTY_ELEMENTS = "io.xlate.edi.stream.TRUNCATE_EMPTY_ELEMENTS";

    /**
     * <p>
     * When set to true and a schema has been provided, elements written to the output will
     * be padded to the minimum length required by the schema.
     *
     * <p>
     * Default value is false.
     *
     * @since 1.11
     */
    public static final String FORMAT_ELEMENTS = "io.xlate.edi.stream.FORMAT_ELEMENTS";

    /**
     * When set to false, control structures, segments, elements, and codes will
     * not be validated unless a user-provided control schema has been set using
     * {@link EDIStreamWriter#setControlSchema(Schema)}.
     *
     * When set to true AND no user-provided control schema has been set, the
     * writer will attempt to find a known control schema for the detected EDI
     * dialect and version to be used for control structure validation.
     *
     * Default value: false
     *
     * @see EDIInputFactory#EDI_VALIDATE_CONTROL_STRUCTURE
     *
     * @since 2.0
     */
    public static final String EDI_VALIDATE_CONTROL_STRUCTURE = "io.xlate.edi.stream.EDI_VALIDATE_CONTROL_STRUCTURE";

    /**
     * Create a new instance of the factory. This static method creates a new
     * factory instance.
     *
     * Once an application has obtained a reference to an EDIOutputFactory it
     * can use the factory to configure and obtain stream instances.
     *
     * @return the factory implementation
     */
    public static EDIOutputFactory newFactory() {
        return new io.xlate.edi.internal.stream.StaEDIOutputFactory();
    }

    /**
     * Create a new EDIStreamWriter that writes to a stream
     *
     * @param stream
     *            {@link OutputStream} to which the EDI data will be written
     * @return the writer instance
     */
    public abstract EDIStreamWriter createEDIStreamWriter(OutputStream stream);

    /**
     * Create a new EDIStreamWriter that writes to a stream
     *
     * @param stream
     *            {@link OutputStream} to which the EDI data will be written
     * @param encoding
     *            character encoding of the stream, must be a valid
     *            {@link java.nio.charset.Charset Charset}.
     * @return the writer instance
     * @throws EDIStreamException
     *             when encoding is not supported
     */
    public abstract EDIStreamWriter createEDIStreamWriter(OutputStream stream,
                                                          String encoding)
            throws EDIStreamException;

    /**
     * Creates a new {@link XMLStreamWriter} that uses the given writer as its
     * output.
     *
     * XML Elements written to the writer must use the namespaces declared by
     * the constants in {@link EDINamespaces}. The sequence of elements is
     * critical and must align with the structure of the intended EDI output to
     * be written via the given EDI writer.
     *
     * @param writer
     *            the writer used to generate EDI output using the XML writer
     * @return a new {@link XMLStreamWriter}
     */
    public abstract XMLStreamWriter createXMLStreamWriter(EDIStreamWriter writer);

    /**
     * Retrieves the reporter that will be set on any EDIStreamWriter created by
     * this factory instance.
     *
     * @return the reporter that will be set on any EDIStreamWriter created by
     *         this factory instance
     *
     * @since 1.9
     */
    public abstract EDIOutputErrorReporter getErrorReporter();

    /**
     * The reporter that will be set on any EDIStreamWriter created by this
     * factory instance.
     *
     * NOTE: When using an EDIOutputErrorReporter, errors found in the data
     * stream that are reported to the reporter will not be throw as instances
     * of {@link EDIValidationException}.
     *
     * @param reporter
     *            the resolver to use to report non fatal errors
     *
     * @since 1.9
     */
    public abstract void setErrorReporter(EDIOutputErrorReporter reporter);
}
