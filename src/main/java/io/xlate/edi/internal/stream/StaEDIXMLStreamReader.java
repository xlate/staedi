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
package io.xlate.edi.internal.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import io.xlate.edi.internal.ThrowingRunnable;
import io.xlate.edi.internal.stream.tokenization.ProxyEventHandler;
import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDINamespaces;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIValidationException;

final class StaEDIXMLStreamReader implements XMLStreamReader {

    private static final Logger LOGGER = Logger.getLogger(StaEDIXMLStreamReader.class.getName());
    private static final QName DUMMY_QNAME = new QName("DUMMY");
    private static final QName INTERCHANGE = new QName(EDINamespaces.LOOPS, "INTERCHANGE", prefixOf(EDINamespaces.LOOPS));
    private static final QName TRANSACTION = new QName(EDINamespaces.LOOPS, ProxyEventHandler.LOOP_CODE_TRANSACTION, prefixOf(EDINamespaces.LOOPS));

    /* test visible */ final EDIStreamReader ediReader;
    private final Map<String, Object> properties;
    private final boolean transactionDeclaresXmlns;
    private final boolean wrapTransactionContents;
    private final boolean useSegmentImplementationCodes;
    private final Location location = new ProxyLocation();

    private final Queue<Integer> eventQueue = new ArrayDeque<>(3);
    private final Queue<QName> elementQueue = new ArrayDeque<>(3);
    private final Deque<QName> elementStack = new ArrayDeque<>(5);
    private final Deque<QName> standardNameStack = new ArrayDeque<>(5);

    private boolean withinTransaction = false;
    private boolean transactionWrapperEnqueued = false;
    private String transactionStartSegment = null;
    private String transactionEndSegment = null;
    private int currentEvent = -1;
    private QName currentElement;

    private NamespaceContext namespaceContext;
    private String compositeCode = null;

    private final StringBuilder cdataBuilder = new StringBuilder();
    private final OutputStream cdataStream = new OutputStream() {
        @Override
        public void write(int b) throws IOException {
            cdataBuilder.append((char) b);
        }
    };

    private char[] cdata;

    StaEDIXMLStreamReader(EDIStreamReader ediReader, Map<String, Object> properties) throws XMLStreamException {
        this.ediReader = ediReader;
        this.properties = new HashMap<>(properties);
        transactionDeclaresXmlns = Boolean.valueOf(String.valueOf(properties.get(EDIInputFactory.XML_DECLARE_TRANSACTION_XMLNS)));
        wrapTransactionContents = Boolean.valueOf(String.valueOf(properties.get(EDIInputFactory.XML_WRAP_TRANSACTION_CONTENTS)));
        useSegmentImplementationCodes = Boolean.valueOf(String.valueOf(properties.get(EDIInputFactory.XML_USE_SEGMENT_IMPLEMENTATION_CODES)));

        if (ediReader.getEventType() == EDIStreamEvent.START_INTERCHANGE) {
            enqueueEvent(EDIStreamEvent.START_INTERCHANGE);
            advanceEvent();
        }
    }

    StaEDIXMLStreamReader(EDIStreamReader ediReader) throws XMLStreamException {
        this(ediReader, Collections.emptyMap());
    }

    @Override
    public Object getProperty(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        return properties.get(name);
    }

    boolean declareNamespaces(QName element) {
        if (INTERCHANGE.equals(element)) {
            return true;
        }
        return this.transactionDeclaresXmlns && TRANSACTION.equals(element);
    }

    private boolean isEvent(int... eventTypes) {
        return Arrays.stream(eventTypes).anyMatch(type -> type == currentEvent);
    }

    private QName buildName(QName parent, String namespace) {
        return buildName(parent, namespace, null);
    }

    private QName buildName(QName parent, String namespace, String name) {
        String prefix = prefixOf(namespace);

        if (name == null) {
            final io.xlate.edi.stream.Location l = ediReader.getLocation();
            final int componentPosition = l.getComponentPosition();

            if (componentPosition > 0) {
                String localPart = this.compositeCode != null ? this.compositeCode : parent.getLocalPart();
                name = String.format("%s-%02d", localPart, componentPosition);
            } else {
                name = String.format("%s%02d", parent.getLocalPart(), l.getElementPosition());
            }
        }

        return new QName(namespace, name, prefix);
    }

    private void enqueueEvent(int xmlEvent, QName element, boolean remember) {
        enqueueEvent(xmlEvent, element, element, remember);
    }

    private void enqueueEvent(int xmlEvent, QName element, QName standardName, boolean remember) {
        LOGGER.finer(() -> "Enqueue XML event: " + xmlEvent + ", element: " + element);
        eventQueue.add(xmlEvent);
        elementQueue.add(element);

        if (remember) {
            elementStack.addFirst(element);
            standardNameStack.addFirst(standardName);
        }
    }

    QName parentName() {
        return standardNameStack.getFirst();
    }

    QName popElement() {
        standardNameStack.removeFirst();
        return elementStack.removeFirst();
    }

    private void advanceEvent() {
        currentEvent = eventQueue.remove();
        currentElement = elementQueue.remove();
    }

    private void enqueueEvent(EDIStreamEvent ediEvent) throws XMLStreamException {
        LOGGER.finer(() -> "Enqueue EDI event: " + ediEvent);
        final QName name;
        cdataBuilder.setLength(0);
        cdata = null;
        String readerText = null;

        switch (ediEvent) {
        case ELEMENT_DATA:
            name = buildName(parentName(), EDINamespaces.ELEMENTS);
            enqueueEvent(START_ELEMENT, name, false);
            enqueueEvent(CHARACTERS, DUMMY_QNAME, false);
            enqueueEvent(END_ELEMENT, name, false);
            break;

        case ELEMENT_DATA_BINARY:
            /*
             * This section will read the binary data and Base64 the stream
             * into an XML CDATA section.
             * */
            name = buildName(parentName(), EDINamespaces.ELEMENTS);
            enqueueEvent(START_ELEMENT, name, false);
            enqueueEvent(CDATA, DUMMY_QNAME, false);
            copyBinaryDataToCDataBuilder();
            enqueueEvent(END_ELEMENT, name, false);
            break;

        case START_INTERCHANGE:
            enqueueEvent(START_DOCUMENT, DUMMY_QNAME, false);
            enqueueEvent(START_ELEMENT, INTERCHANGE, true);
            namespaceContext = new DocumentNamespaceContext();
            break;

        case START_SEGMENT:
            readerText = ediReader.getText();
            performTransactionWrapping(readerText);
            QName standardName = buildName(parentName(), EDINamespaces.SEGMENTS, readerText);
            name = useSegmentImplementationCodes ? buildName(parentName(), EDINamespaces.SEGMENTS, ediReader.getReferenceCode()) : standardName;
            enqueueEvent(START_ELEMENT, name, standardName, true);
            break;

        case START_TRANSACTION:
            withinTransaction = true;
            name = buildName(parentName(), EDINamespaces.LOOPS, ediReader.getReferenceCode());
            enqueueEvent(START_ELEMENT, name, true);
            determineTransactionSegments();
            break;

        case START_GROUP:
        case START_LOOP:
            name = buildName(parentName(), EDINamespaces.LOOPS, ediReader.getReferenceCode());
            enqueueEvent(START_ELEMENT, name, true);
            break;

        case START_COMPOSITE:
            compositeCode = ediReader.getReferenceCode();
            name = buildName(parentName(), EDINamespaces.COMPOSITES);
            enqueueEvent(START_ELEMENT, name, true);
            break;

        case END_INTERCHANGE:
            enqueueEvent(END_ELEMENT, popElement(), false);
            namespaceContext = null;
            enqueueEvent(END_DOCUMENT, DUMMY_QNAME, false);
            break;

        case END_TRANSACTION:
            withinTransaction = false;
            compositeCode = null;
            enqueueEvent(END_ELEMENT, popElement(), false);
            break;

        case END_GROUP:
        case END_LOOP:
        case END_SEGMENT:
        case END_COMPOSITE:
            compositeCode = null;
            enqueueEvent(END_ELEMENT, popElement(), false);
            break;

        case SEGMENT_ERROR:
        case ELEMENT_OCCURRENCE_ERROR:
        case ELEMENT_DATA_ERROR:
            EDIValidationException cause = new EDIValidationException(ediEvent,
                ediReader.getErrorType(),
                ediReader.getLocation().copy(),
                ediReader.getText());
            throw new XMLStreamException("Validation exception reading EDI data as XML", this.location, cause);

        default:
            throw new IllegalStateException("Unknown state: " + ediEvent);
        }
    }

    private void determineTransactionSegments() {
        transactionStartSegment = null;
        transactionEndSegment = null;

        if (wrapTransactionContents) {
            EDIComplexType tx = (EDIComplexType) ediReader.getSchemaTypeReference().getReferencedType();
            List<EDIReference> segments = tx.getReferences();
            transactionStartSegment = segments.get(0).getReferencedType().getId();
            transactionEndSegment = segments.get(segments.size() - 1).getReferencedType().getId();
        }
    }

    private void performTransactionWrapping(String readerText) {
        if (withinTransaction && wrapTransactionContents) {
            if (transactionWrapperEnqueued) {
                if (readerText.equals(this.transactionEndSegment)) {
                    enqueueEvent(END_ELEMENT, popElement(), false);
                    transactionWrapperEnqueued = false;
                }
            } else {
                if (!readerText.equals(this.transactionStartSegment)) {
                    String local = ediReader.getStandard() + '-' + ediReader.getTransactionType() + '-' + ediReader.getTransactionVersionString();
                    QName wrapper = new QName(EDINamespaces.LOOPS, local, prefixOf(EDINamespaces.LOOPS));
                    enqueueEvent(START_ELEMENT, wrapper, true);
                    transactionWrapperEnqueued = true;
                }
            }
        }
    }

    private void copyBinaryDataToCDataBuilder() throws XMLStreamException {
        // This only will work if using a validation filter!
        InputStream input = ediReader.getBinaryData();

        ThrowingRunnable.run(() -> {
            byte[] buffer = new byte[4096];
            int amount;

            try (OutputStream output = Base64.getEncoder().wrap(cdataStream)) {
                while ((amount = input.read(buffer)) > -1) {
                    output.write(buffer, 0, amount);
                }
            }
        }, XMLStreamException::new);
    }

    private void requireCharacters() {
        if (!isCharacters()) {
            throw new IllegalStateException("Text only available for CHARACTERS");
        }
    }

    @Override
    public int next() throws XMLStreamException {
        if (eventQueue.isEmpty()) {
            LOGGER.finer(() -> "eventQueue is empty, calling ediReader.next()");
            try {
                enqueueEvent(ediReader.next());
            } catch (XMLStreamException e) {
                throw e;
            } catch (Exception e) {
                throw new XMLStreamException(e);
            }
        }

        advanceEvent();

        return getEventType();
    }

    @Override
    public void require(int type, String namespaceURI, String localName) throws XMLStreamException {
        final int currentType = getEventType();

        if (currentType != type) {
            throw new XMLStreamException("Current type " + currentType + " does not match required type " + type);
        }

        if (namespaceURI != null || localName != null) {
            if (!hasName()) {
                throw new XMLStreamException("Current type " + currentType + " does not have a corresponding name");
            }

            final QName name = getName();

            if (localName != null) {
                final String currentLocalPart = name.getLocalPart();

                if (!localName.equals(currentLocalPart)) {
                    throw new XMLStreamException("Current localPart " + currentLocalPart
                            + " does not match required localName " + localName);
                }
            }

            if (namespaceURI != null) {
                final String currentURI = name.getNamespaceURI();

                if (!namespaceURI.equals(currentURI)) {
                    throw new XMLStreamException("Current namespace " + currentURI
                            + " does not match required namespaceURI " + namespaceURI);
                }
            }
        }
    }

    static XMLStreamException streamException(String message) {
        return new XMLStreamException(message);
    }

    @Override
    public String getElementText() throws XMLStreamException {
        if (ediReader.getEventType() != EDIStreamEvent.ELEMENT_DATA) {
            throw streamException("Element text only available for simple element");
        }

        if (getEventType() != START_ELEMENT) {
            throw streamException("Element text only available on START_ELEMENT");
        }

        next(); // Advance to the text/CDATA
        final String text = getText();
        int eventType = next();

        if (eventType != END_ELEMENT) {
            throw streamException("Unexpected event type after text " + eventType);
        }

        return text;
    }

    @Override
    public int nextTag() throws XMLStreamException {
        int eventType;

        do {
            eventType = next();
        } while (eventType != START_ELEMENT && eventType != END_ELEMENT);

        return eventType;
    }

    @Override
    public boolean hasNext() throws XMLStreamException {
        try {
            return !eventQueue.isEmpty() || ediReader.hasNext();
        } catch (Exception e) {
            throw new XMLStreamException(e);
        }
    }

    @Override
    public void close() throws XMLStreamException {
        eventQueue.clear();
        elementQueue.clear();
        elementStack.clear();
        standardNameStack.clear();
        ThrowingRunnable.run(ediReader::close, XMLStreamException::new);
    }

    @Override
    public String getNamespaceURI(String prefix) {
        if (namespaceContext != null) {
            return namespaceContext.getNamespaceURI(prefix);
        }
        return null;
    }

    @Override
    public boolean isStartElement() {
        return isEvent(START_ELEMENT);
    }

    @Override
    public boolean isEndElement() {
        return isEvent(END_ELEMENT);
    }

    @Override
    public boolean isCharacters() {
        return isEvent(CHARACTERS, CDATA);
    }

    @Override
    public boolean isWhiteSpace() {
        return false;
    }

    @Override
    public String getAttributeValue(String namespaceURI, String localName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getAttributeCount() {
        return 0;
    }

    @Override
    public QName getAttributeName(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAttributeNamespace(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAttributeLocalName(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAttributePrefix(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAttributeType(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAttributeValue(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAttributeSpecified(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getNamespaceCount() {
        if (declareNamespaces(currentElement)) {
            return EDINamespaces.all().size();
        }
        return 0;
    }

    @Override
    public String getNamespacePrefix(int index) {
        if (declareNamespaces(currentElement)) {
            String namespace = EDINamespaces.all().get(index);
            return prefixOf(namespace);
        }
        return null;
    }

    @Override
    public String getNamespaceURI(int index) {
        if (declareNamespaces(currentElement)) {
            return EDINamespaces.all().get(index);
        }
        return null;
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return this.namespaceContext;
    }

    @Override
    public int getEventType() {
        return currentEvent;
    }

    @Override
    public String getText() {
        requireCharacters();

        if (cdataBuilder.length() > 0) {
            if (cdata == null) {
                cdata = new char[cdataBuilder.length()];
                cdataBuilder.getChars(0, cdataBuilder.length(), cdata, 0);
            }

            return new String(cdata);
        }
        return ediReader.getText();
    }

    @Override
    public char[] getTextCharacters() {
        requireCharacters();

        if (cdataBuilder.length() > 0) {
            if (cdata == null) {
                cdata = new char[cdataBuilder.length()];
                cdataBuilder.getChars(0, cdataBuilder.length(), cdata, 0);
            }

            return cdata;
        }
        return ediReader.getTextCharacters();
    }

    @Override
    public int getTextCharacters(int sourceStart,
                                 char[] target,
                                 int targetStart,
                                 int length) throws XMLStreamException {

        requireCharacters();

        if (cdataBuilder.length() > 0) {
            if (cdata == null) {
                cdata = new char[cdataBuilder.length()];
                cdataBuilder.getChars(0, cdataBuilder.length(), cdata, 0);
            }

            if (targetStart < 0) {
                throw new IndexOutOfBoundsException("targetStart < 0");
            }
            if (targetStart > target.length) {
                throw new IndexOutOfBoundsException("targetStart > target.length");
            }
            if (length < 0) {
                throw new IndexOutOfBoundsException("length < 0");
            }
            if (targetStart + length > target.length) {
                throw new IndexOutOfBoundsException("targetStart + length > target.length");
            }

            System.arraycopy(cdata, sourceStart, target, targetStart, length);
            return length;
        }
        return ediReader.getTextCharacters(sourceStart, target, targetStart, length);
    }

    @Override
    public int getTextStart() {
        requireCharacters();

        if (cdataBuilder.length() > 0) {
            return 0;
        }
        return ediReader.getTextStart();
    }

    @Override
    public int getTextLength() {
        requireCharacters();

        if (cdataBuilder.length() > 0) {
            return cdataBuilder.length();
        }
        return ediReader.getTextLength();
    }

    @Override
    public String getEncoding() {
        return null;
    }

    @Override
    public boolean hasText() {
        return isCharacters();
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public QName getName() {
        if (hasName()) {
            return currentElement;
        }
        throw new IllegalStateException("Text only available for START_ELEMENT or END_ELEMENT");
    }

    @Override
    public String getLocalName() {
        return getName().getLocalPart();
    }

    @Override
    public boolean hasName() {
        return isStartElement() || isEndElement();
    }

    @Override
    public String getNamespaceURI() {
        if (hasName()) {
            return currentElement.getNamespaceURI();
        }
        return null;
    }

    @Override
    public String getPrefix() {
        return null;
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public boolean isStandalone() {
        return false;
    }

    @Override
    public boolean standaloneSet() {
        return false;
    }

    @Override
    public String getCharacterEncodingScheme() {
        return null;
    }

    @Override
    public String getPITarget() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPIData() {
        throw new UnsupportedOperationException();
    }

    static String prefixOf(String namespace) {
        return String.valueOf(namespace.substring(namespace.lastIndexOf(':') + 1).charAt(0));
    }

    private class ProxyLocation implements Location {
        @Override
        public int getLineNumber() {
            return ediReader.getLocation().getLineNumber();
        }

        @Override
        public int getColumnNumber() {
            return ediReader.getLocation().getColumnNumber();
        }

        @Override
        public int getCharacterOffset() {
            return ediReader.getLocation().getCharacterOffset();
        }

        @Override
        public String getPublicId() {
            return null;
        }

        @Override
        public String getSystemId() {
            return null;
        }
    }
}
