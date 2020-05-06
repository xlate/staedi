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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import io.xlate.edi.schema.Schema;

public abstract class EDIInputFactory extends PropertySupport {

    public static final String EDI_VALIDATE_CONTROL_STRUCTURE = "io.xlate.edi.stream.EDI_VALIDATE_CONTROL_STRUCTURE";
    public static final String EDI_VALIDATE_CONTROL_CODE_VALUES = "io.xlate.edi.stream.EDI_VALIDATE_CONTROL_CODE_VALUES";

    /**
     * Create a new instance of the factory. This static method creates a new
     * factory instance.
     *
     * Once an application has obtained a reference to an EDIInputFactory it can
     * use the factory to configure and obtain stream instances.
     *
     * @return the factory implementation
     */
    public static EDIInputFactory newFactory() {
        return new io.xlate.edi.internal.stream.StaEDIInputFactory();
    }

    /**
     * Creates a new {@link EDIStreamReader} using the given {@link InputStream}
     * (with default encoding).
     *
     * @param stream
     *            {@link InputStream} from which the EDI data will be read
     * @return a new {@link EDIStreamReader} which reads from the stream
     */
    public abstract EDIStreamReader createEDIStreamReader(InputStream stream);

    /**
     * Creates a new {@link EDIStreamReader} using the given {@link InputStream}
     * and encoding.
     *
     * @param stream
     *            {@link InputStream} from which the EDI data will be read
     * @param encoding
     *            character encoding of the stream
     * @return a new {@link EDIStreamReader} which reads from the stream
     * @throws EDIStreamException
     *             when encoding is not supported
     */
    public abstract EDIStreamReader createEDIStreamReader(InputStream stream,
                                                          String encoding) throws EDIStreamException;

    /**
     * Creates a new {@link EDIStreamReader} using the given {@link InputStream}
     * (with default encoding) which uses the {@link Schema} for validation of
     * the input's control structures (interchange, group, transaction).
     *
     * Note that a separate schema for validation of messages/transactions may
     * be passed directly to the {@link EDIStreamReader} once the type and
     * version of the messages is known.
     *
     * @param stream
     *            {@link InputStream} from which the EDI data will be read
     * @param schema
     *            {@link Schema} for control structure validation
     * @return a new {@link EDIStreamReader} which reads from the stream
     */
    public abstract EDIStreamReader createEDIStreamReader(InputStream stream, Schema schema);

    /**
     * Creates a new {@link EDIStreamReader} using the given {@link InputStream}
     * and encoding which uses the {@link Schema} for validation of the input's
     * control structures (interchange, group, transaction).
     *
     * Note that a separate schema for validation of messages/transactions may
     * be passed directly to the {@link EDIStreamReader} once the type and
     * version of the messages is known.
     *
     * @param stream
     *            {@link InputStream} from which the EDI data will be read
     * @param encoding
     *            character encoding of the stream
     * @param schema
     *            {@link Schema} for control structure validation
     * @return a new {@link EDIStreamReader} which reads from the stream
     * @throws EDIStreamException
     *             when encoding is not supported
     */
    public abstract EDIStreamReader createEDIStreamReader(InputStream stream,
                                                          String encoding,
                                                          Schema schema) throws EDIStreamException;

    /**
     * Creates a new {@link EDIStreamReader} by wrapping the given reader with
     * the {@link EDIStreamFilter} filter.
     *
     * @param reader
     *            the reader to wrap
     * @param filter
     *            a filter to wrap the given reader
     * @return a new {@link EDIStreamReader} which uses filter
     */
    public abstract EDIStreamReader createFilteredReader(EDIStreamReader reader, EDIStreamFilter filter);

    /**
     * Creates a new {@link XMLStreamReader} that uses the given reader as its
     * data source. The reader should be positioned before the start of an
     * interchange or at the start of an interchange.
     *
     * XML generated by the XMLStreamReader consist exclusively of elements in
     * the namespaces declared by the constants in {@link EDINamespaces}. The
     * names of the elements will be as follows:
     *
     * <table>
     * <caption>EDI Event Namespace Cross Reference</caption>
     * <tr>
     * <td>Event</td>
     * <td>Element Local Name</td>
     * <td>Element Namespace</td>
     * <td></td>
     * </tr>
     * <tr>
     * <td>Start/End of interchange</td>
     * <td>INTERCHANGE</td>
     * <td>{@link EDINamespaces#LOOPS}</td>
     * </tr>
     * <tr>
     * <td>Start/End of functional group</td>
     * <td>GROUP</td>
     * <td>{@link EDINamespaces#LOOPS}</td>
     * </tr>
     * <tr>
     * <td>Start/End of message/transaction</td>
     * <td>TRANSACTION</td>
     * <td>{@link EDINamespaces#LOOPS}</td>
     * </tr>
     * <tr>
     * <td>Start/End of loop</td>
     * <td>Loop code from EDI schema</td>
     * <td>{@link EDINamespaces#LOOPS}</td>
     * </tr>
     * <tr>
     * <td>Start/End of segment</td>
     * <td>Segment tag from EDI data</td>
     * <td>{@link EDINamespaces#SEGMENTS}</td>
     * </tr>
     * <tr>
     * <td>Start/End of composite</td>
     * <td>Segment tag plus two digit element position from EDI data</td>
     * <td>{@link EDINamespaces#COMPOSITES}</td>
     * </tr>
     * <tr>
     * <td>Start/End of simple element</td>
     * <td>Segment tag plus two digit element position from EDI data</td>
     * <td>{@link EDINamespaces#ELEMENTS}</td>
     * </tr>
     * <tr>
     * <td>Start/End of component element in a composite</td>
     * <td>Segment tag plus two digit element position from EDI data plus hyphen
     * plus two digit component position</td>
     * <td>{@link EDINamespaces#ELEMENTS}</td>
     * </tr>
     * </table>
     *
     * Errors encountered in the EDI data will result in an XMLStreamException
     * with a message describing the error.
     *
     * @param reader
     *            the reader to wrap
     * @return a new {@link XMLStreamReader}
     * @throws XMLStreamException
     *             when the reader encounters an error in creation
     *
     * @see EDINamespaces
     */
    public abstract XMLStreamReader createXMLStreamReader(EDIStreamReader reader) throws XMLStreamException;

    /**
     * Retrieves the reporter that will be set on any EDIStreamReader created by
     * this factory instance.
     *
     * @return the reporter that will be set on any EDIStreamReader created by
     *         this factory instance
     *
     * @since 1.4
     */
    public abstract EDIReporter getEDIReporter();

    /**
     * The reporter that will be set on any EDIStreamReader created by this
     * factory instance.
     *
     * NOTE: When using an EDIReporter, errors found in the data stream that are
     * reported to the reporter will not appear in the stream of events returned
     * by the EDIStreamReader.
     *
     * @param reporter
     *            the resolver to use to report non fatal errors
     *
     * @since 1.4
     */
    public abstract void setEDIReporter(EDIReporter reporter);
}
