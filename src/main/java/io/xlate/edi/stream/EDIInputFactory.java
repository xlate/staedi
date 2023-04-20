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

    /**
     * When set to false, control structures, segments, elements, and codes will
     * not be validated unless a user-provided control schema has been set using
     * {@link EDIStreamReader#setControlSchema(Schema)}.
     *
     * When set to true AND no user-provided control schema has been set, the
     * reader will attempt to find a known control schema for the detected EDI
     * dialect and version to be used for control structure validation.
     *
     * Default value: true
     *
     * @see EDIOutputFactory#EDI_VALIDATE_CONTROL_STRUCTURE
     */
    public static final String EDI_VALIDATE_CONTROL_STRUCTURE = "io.xlate.edi.stream.EDI_VALIDATE_CONTROL_STRUCTURE";

    /**
     * When set to false, enumerated code values of control structure elements
     * will be ignore. Element size and type validation will still occur.
     */
    public static final String EDI_VALIDATE_CONTROL_CODE_VALUES = "io.xlate.edi.stream.EDI_VALIDATE_CONTROL_CODE_VALUES";

    /**
     * When set to true, XMLStreamReader instances created from an
     * EDIInputFactory will generate XMLNS attributes on the TRANSACTION element
     * in addition to the INTERCHANGE element.
     */
    public static final String XML_DECLARE_TRANSACTION_XMLNS = "io.xlate.edi.stream.XML_DECLARE_TRANSACTION_XMLNS";

    /**
     * When set to true, XMLStreamReader instances created from an
     * EDIInputFactory will generate an element wrapper around the transaction's
     * content element. The wrapper will not contain the elements representing
     * the transaction's header and trailer segments.
     *
     * The wrapper element will be in the {@link EDINamespaces#LOOPS LOOPS} XML
     * name space and the local name will be constructed as follows: <br>
     * {@code <Standard Name> + '-' + <Transaction Code> + '-' + <Transaction Version String>}
     *
     * @since 1.16
     */
    public static final String XML_WRAP_TRANSACTION_CONTENTS = "io.xlate.edi.stream.XML_WRAP_TRANSACTION_CONTENTS";

    /**
     * When set to true, the XML elements representing segments in an EDI implementation schema
     * will be named according to the schema-defined {@code code} attribute for the segment.
     *
     * Default value: false
     *
     * @since 1.21
     */
    public static final String XML_USE_SEGMENT_IMPLEMENTATION_CODES = "io.xlate.edi.stream.XML_USE_SEGMENT_IMPLEMENTATION_CODES";

    /**
     * When set to true, non-graphical, control characters will be ignored in
     * the EDI input stream. This includes characters ranging from 0x00 through
     * 0x1F and 0x7F.
     *
     * @since 1.13
     */
    public static final String EDI_IGNORE_EXTRANEOUS_CHARACTERS = "io.xlate.edi.stream.EDI_IGNORE_EXTRANEOUS_CHARACTERS";

    /**
     * When set to true, hierarchical loops will be nested in the EDI input
     * stream. The nesting structure is determined by the linkage specified by
     * the EDI data itself using pointers given in the EDI schema for a loop.
     *
     * For example, the hierarchical information given by the X12 HL segment.
     *
     * Default value: true
     *
     * @since 1.18
     */
    public static final String EDI_NEST_HIERARCHICAL_LOOPS = "io.xlate.edi.stream.EDI_NEST_HIERARCHICAL_LOOPS";

    /**
     * When set to true, functional group, transaction, and loop start/end
     * events will allow for {@link EDIStreamReader#getText()} to be called,
     * which is the legacy behavior.
     *
     * The default value is `true` and this property is deprecated. In the next
     * major release, the property's default value will be `false`.
     *
     * Default value: true
     *
     * @since 1.20
     * @deprecated use {@link EDIStreamReader#getReferenceCode()} and
     *             {@link EDIStreamReader#getSchemaTypeReference()} to retrieve
     *             additional information for non-textual event types.
     */
    @Deprecated
    public static final String EDI_ENABLE_LOOP_TEXT = "io.xlate.edi.stream.EDI_ENABLE_LOOP_TEXT"; //NOSONAR

    /**
     * When set to true, simple data elements not containing data will be
     * represented via the JSON parsers as a <i>null</i> value.
     *
     * Default value: false
     *
     * @since 1.14
     */
    public static final String JSON_NULL_EMPTY_ELEMENTS = "io.xlate.edi.stream.JSON_NULL_EMPTY_ELEMENTS";

    /**
     * When set to true, simple data elements will be represented via the JSON
     * parsers as JSON objects. When false, simple elements will be JSON
     * primitive values (string, number) in the data array of their containing
     * structures.
     *
     * Default value: false
     *
     * @since 1.14
     */
    public static final String JSON_OBJECT_ELEMENTS = "io.xlate.edi.stream.JSON_OBJECT_ELEMENTS";

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
     * and encoding. The encoding must be a valid
     * {@link java.nio.charset.Charset Charset}.
     *
     * @param stream
     *            {@link InputStream} from which the EDI data will be read
     * @param encoding
     *            character encoding of the stream, must be a valid
     *            {@link java.nio.charset.Charset Charset}.
     * @return a new {@link EDIStreamReader} which reads from the stream
     * @throws EDIStreamException
     *             when encoding is not supported
     */
    public abstract EDIStreamReader createEDIStreamReader(InputStream stream,
                                                          String encoding)
            throws EDIStreamException;

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
     * control structures (interchange, group, transaction). The encoding must
     * be a valid {@link java.nio.charset.Charset Charset}.
     *
     * Note that a separate schema for validation of messages/transactions may
     * be passed directly to the {@link EDIStreamReader} once the type and
     * version of the messages is known.
     *
     * @param stream
     *            {@link InputStream} from which the EDI data will be read
     * @param encoding
     *            character encoding of the stream, must be a valid
     *            {@link java.nio.charset.Charset Charset}.
     * @param schema
     *            {@link Schema} for control structure validation
     * @return a new {@link EDIStreamReader} which reads from the stream
     * @throws EDIStreamException
     *             when encoding is not supported
     */
    public abstract EDIStreamReader createEDIStreamReader(InputStream stream,
                                                          String encoding,
                                                          Schema schema)
            throws EDIStreamException;

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
     * Creates a new JSON parser of type <code>J</code> that uses the given
     * reader as its data source. The reader should be positioned before the
     * start of an interchange or at the start of an interchange.
     *
     * @param <J>
     *            the type of the parser being created
     * @param reader
     *            the reader to wrap
     * @param type
     *            the type of the parser being created
     * @return a new JSON parser of type <code>J</code>
     *
     * @throws IllegalArgumentException
     *             when type is an unsupported parser type
     *
     * @since 1.14
     */
    public abstract <J> J createJsonParser(EDIStreamReader reader, Class<J> type);

    /**
     * Retrieves the reporter that will be set on any EDIStreamReader created by
     * this factory instance.
     *
     * @return the reporter that will be set on any EDIStreamReader created by
     *         this factory instance
     *
     * @throws ClassCastException
     *             when reporter is not an instance of EDIReporter
     *
     * @since 1.4
     * @deprecated use {@link #getErrorReporter()} instead
     */
    @SuppressWarnings({ "java:S1123", "java:S1133" })
    @Deprecated /*(forRemoval = true, since = "1.9")*/
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
     * @deprecated use {@link #setErrorReporter(EDIInputErrorReporter)} instead
     */
    @SuppressWarnings({ "java:S1123", "java:S1133" })
    @Deprecated /*(forRemoval = true, since = "1.9")*/
    public abstract void setEDIReporter(EDIReporter reporter);

    /**
     * Retrieves the reporter that will be set on any EDIStreamReader created by
     * this factory instance.
     *
     * @return the reporter that will be set on any EDIStreamReader created by
     *         this factory instance
     *
     * @since 1.9
     */
    public abstract EDIInputErrorReporter getErrorReporter();

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
     * @since 1.9
     */
    public abstract void setErrorReporter(EDIInputErrorReporter reporter);
}
