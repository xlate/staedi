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

public abstract class EDIOutputFactory extends PropertySupport {

    public static final String PRETTY_PRINT = "io.xlate.edi.stream.PRETTY_PRINT";

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
     *            - the stream to write to
     * @return the writer instance
     */
    public abstract EDIStreamWriter createEDIStreamWriter(OutputStream stream);

    /**
     * Create a new EDIStreamWriter that writes to a stream
     *
     * @param stream
     *            - the stream to write to
     * @param encoding
     *            - the encoding to use
     * @return the writer instance
     * @throws EDIStreamException
     *  when encoding is not supported
     */
    public abstract EDIStreamWriter createEDIStreamWriter(OutputStream stream,
                                                          String encoding) throws EDIStreamException;

    /**
     * Creates a new {@link XMLStreamWriter} that uses the given writer as its output.
     *
     * XML Elements written to the writer must use the namespaces declared by the constants
     * in {@link EDINamespaces}. The sequence of elements is critical and must align with
     * the structure of the intended EDI output to be written via the given EDI writer.
     *
     * @param writer the writer used to generate EDI output using the XML writer
     * @return a new {@link XMLStreamWriter}
     */
    public abstract XMLStreamWriter createXMLStreamWriter(EDIStreamWriter writer);
}
