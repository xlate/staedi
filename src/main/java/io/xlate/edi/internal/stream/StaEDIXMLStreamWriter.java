package io.xlate.edi.internal.stream;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamWriter;
import io.xlate.edi.stream.EDINamespaces;

final class StaEDIXMLStreamWriter implements XMLStreamWriter {

    private static final QName INTERCHANGE = new QName(EDINamespaces.LOOPS, "INTERCHANGE");

    private final EDIStreamWriter ediWriter;

    private final Deque<QName> elementStack = new ArrayDeque<>();
    private QName previousElement;

    private NamespaceContext namespaceContext;
    private final Deque<Map<String, String>> namespaceStack = new ArrayDeque<>();

    StaEDIXMLStreamWriter(EDIStreamWriter ediWriter) {
        this.ediWriter = ediWriter;
        namespaceStack.push(new HashMap<>()); // Root namespace scope
    }

    @FunctionalInterface
    interface EDIStreamWriterRunner {
        void execute() throws EDIStreamException;
    }

    void execute(EDIStreamWriterRunner runner) throws XMLStreamException {
        try {
            runner.execute();
        } catch (Exception e) {
            throw new XMLStreamException(e);
        }
    }

    boolean repeatedElement(QName name, QName previousElement) {
        if (previousElement == null) {
            return false;
        }

        if (name.equals(previousElement)) {
            return true;
        }

        return name.getLocalPart().equals(previousElement.getLocalPart());
    }

    void writeStart(QName name) throws XMLStreamException {
        namespaceStack.push(new HashMap<>());

        switch (name.getNamespaceURI()) {
        case EDINamespaces.COMPOSITES:
            if (repeatedElement(name, previousElement)) {
                execute(ediWriter::writeRepeatElement);
            } else {
                execute(ediWriter::writeStartElement);
            }
            elementStack.push(name);
            break;
        case EDINamespaces.ELEMENTS:
            if (EDINamespaces.COMPOSITES.equals(elementStack.element().getNamespaceURI())) {
                execute(ediWriter::startComponent);
            } else {
                if (repeatedElement(name, previousElement)) {
                    execute(ediWriter::writeRepeatElement);
                } else {
                    execute(ediWriter::writeStartElement);
                }
            }
            elementStack.push(name);
            break;
        case EDINamespaces.LOOPS:
            // Loops are implicit when writing
            elementStack.push(name);
            break;
        case EDINamespaces.SEGMENTS:
            execute(() -> ediWriter.writeStartSegment(name.getLocalPart()));
            elementStack.push(name);
            break;
        default:
            break;
        }
    }

    void writeEnd() throws XMLStreamException {
        QName name = elementStack.remove();
        namespaceStack.remove();

        switch (name.getNamespaceURI()) {
        case EDINamespaces.COMPOSITES:
            execute(ediWriter::endElement);
            previousElement = name;
            break;
        case EDINamespaces.ELEMENTS:
            if (EDINamespaces.COMPOSITES.equals(elementStack.element().getNamespaceURI())) {
                execute(ediWriter::endComponent);
            } else {
                execute(ediWriter::endElement);
                previousElement = name;
            }
            break;
        case EDINamespaces.LOOPS:
            // Loops are implicit when writing
            break;
        case EDINamespaces.SEGMENTS:
            execute(ediWriter::writeEndSegment);
            break;
        default:
            break;
        }
    }

    @Override
    public void writeStartElement(String localName) throws XMLStreamException {
        String uri;
        String local;
        String prefix;
        int idx = localName.indexOf(':');

        if (idx >= 0) {
            prefix = localName.substring(0, idx);
            local = localName.substring(idx + 1);
            uri = getNamespaceURI(prefix);
        } else {
            prefix = "";
            local = localName;
            uri = getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX);
        }

        if (INTERCHANGE.getLocalPart().equals(local)) {
            writeStart(INTERCHANGE);
        } else {
            if (uri == null || uri.isEmpty()) {
                throw new XMLStreamException("Element " + localName + " has an undefined namespace");
            }

            writeStart(new QName(uri, local, prefix));
        }
    }

    @Override
    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        writeStart(new QName(namespaceURI, localName));
    }

    @Override
    public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        writeStart(new QName(namespaceURI, localName, prefix));
    }

    @Override
    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        writeStartElement(namespaceURI, localName);
        writeEnd();
    }

    @Override
    public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        writeStartElement(prefix, localName, namespaceURI);
        writeEnd();
    }

    @Override
    public void writeEmptyElement(String localName) throws XMLStreamException {
        writeStartElement(localName);
        writeEnd();
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        writeEnd();
    }

    @Override
    public void writeEndDocument() throws XMLStreamException {
        execute(ediWriter::endInterchange);
    }

    @Override
    public void close() throws XMLStreamException {
        execute(ediWriter::close);
    }

    @Override
    public void flush() throws XMLStreamException {
        execute(ediWriter::flush);
    }

    @Override
    public void writeAttribute(String localName, String value) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeAttribute(String prefix,
                               String namespaceURI,
                               String localName,
                               String value)
            throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
        // No operation - ignored
    }

    @Override
    public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
        // No operation - ignored
    }

    @Override
    public void writeComment(String data) throws XMLStreamException {
        // No operation - ignored
    }

    @Override
    public void writeProcessingInstruction(String target) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeCData(String data) throws XMLStreamException {
        writeCharacters(data);
    }

    @Override
    public void writeDTD(String dtd) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeEntityRef(String name) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeStartDocument() throws XMLStreamException {
        execute(ediWriter::startInterchange);
    }

    @Override
    public void writeStartDocument(String version) throws XMLStreamException {
        writeStartDocument();
    }

    @Override
    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        writeStartDocument();
    }

    @Override
    public void writeCharacters(String text) throws XMLStreamException {
        if (EDINamespaces.ELEMENTS.equals(elementStack.element().getNamespaceURI())) {
            execute(() -> ediWriter.writeElementData(text));
        } else {
            for (int i = 0, m = text.length(); i < m; i++) {
                if (!Character.isWhitespace(text.charAt(i))) {
                    throw new XMLStreamException("Illegal non-whitespace characters");
                }
            }
        }

    }

    @Override
    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        if (EDINamespaces.ELEMENTS.equals(elementStack.element().getNamespaceURI())) {
            execute(() -> ediWriter.writeElementData(text, start, start + len));
        } else {
            for (int i = start, m = start + len; i < m; i++) {
                if (!Character.isWhitespace(text[i])) {
                    throw new XMLStreamException("Illegal non-whitespace characters");
                }
            }
        }
    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        return namespaceStack.stream()
                             .filter(m -> m.containsValue(uri))
                             .flatMap(m -> m.entrySet().stream())
                             .filter(e -> e.getValue().equals(uri))
                             .map(Map.Entry::getKey)
                             .findFirst()
                             .orElseGet(() -> getContextPrefix(uri));
    }

    String getNamespaceURI(String prefix) {
        return namespaceStack.stream()
                             .filter(m -> m.containsKey(prefix))
                             .map(m -> m.get(prefix))
                             .findFirst()
                             .orElseGet(() -> getContextNamespaceURI(prefix));
    }

    String getContextNamespaceURI(String prefix) {
        if (namespaceContext != null) {
            return namespaceContext.getNamespaceURI(prefix);
        }
        return null;
    }

    String getContextPrefix(String uri) {
        if (namespaceContext != null) {
            return namespaceContext.getPrefix(uri);
        }
        return null;
    }

    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        Objects.requireNonNull(prefix);
        if (uri != null) {
            namespaceStack.element().put(prefix, uri);
        } else {
            namespaceStack.element().remove(prefix);
        }
    }

    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {
        setPrefix(XMLConstants.DEFAULT_NS_PREFIX, uri);
    }

    @Override
    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        if (this.namespaceContext != null) {
            throw new XMLStreamException("NamespaceContext has already been set");
        }

        if (!elementStack.isEmpty()) {
            throw new XMLStreamException("NamespaceContext must only be called at the start of the document");
        }

        this.namespaceContext = Objects.requireNonNull(context);

        // Clear the root contexts (per setNamespaceContext JavaDoc)
        namespaceStack.getLast().clear();
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return namespaceContext;
    }

    @Override
    public Object getProperty(String name) {
        throw new IllegalArgumentException("Properties not supported");
    }

}
