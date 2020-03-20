package io.xlate.edi.internal.stream;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import io.xlate.edi.stream.EDIStreamConstants.Namespaces;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamWriter;

final class StaEDIXMLStreamWriter implements XMLStreamWriter {

    private static final QName INTERCHANGE = new QName(Namespaces.LOOPS, "INTERCHANGE");

    private final EDIStreamWriter ediWriter;
    private final Map<String, String> namespaces = new HashMap<>();

    private NamespaceContext namespaceContext;

    private final Deque<QName> elementStack = new ArrayDeque<>();
    private QName previousElement;
    private String defaultNamespaceURI;

    public StaEDIXMLStreamWriter(EDIStreamWriter ediWriter) {
        this.ediWriter = ediWriter;
        this.namespaceContext = new DocumentNamespaceContext();
    }

    @FunctionalInterface
    interface EDIStreamWriterRunner {
       void execute() throws EDIStreamException;
    }

    void execute(EDIStreamWriterRunner runner) throws XMLStreamException {
        try {
            runner.execute();
        } catch (EDIStreamException e) {
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
        switch (name.getNamespaceURI()) {
        case Namespaces.COMPOSITES:
            if (repeatedElement(name, previousElement)) {
                execute(ediWriter::writeRepeatElement);
            } else {
                execute(ediWriter::writeStartElement);
            }
            elementStack.push(name);
            break;
        case Namespaces.ELEMENTS:
            if (Namespaces.COMPOSITES.equals(elementStack.element().getNamespaceURI())) {
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
        case Namespaces.LOOPS:
            // Loops are implicit when writing
            elementStack.push(name);
            break;
        case Namespaces.SEGMENTS:
            execute(() -> ediWriter.writeStartSegment(name.getLocalPart()));
            elementStack.push(name);
            break;
        default:
            break;
        }
    }

    void writeEnd() throws XMLStreamException {
        QName name = elementStack.remove();

        switch (name.getNamespaceURI()) {
        case Namespaces.COMPOSITES:
            execute(ediWriter::endElement);
            previousElement = name;
            break;
        case Namespaces.ELEMENTS:
            if (Namespaces.COMPOSITES.equals(elementStack.element().getNamespaceURI())) {
                execute(ediWriter::endComponent);
            } else {
                execute(ediWriter::endElement);
                previousElement = name;
            }
            break;
        case Namespaces.LOOPS:
            // Loops are implicit when writing
            break;
        case Namespaces.SEGMENTS:
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
            uri = namespaces.get(prefix);
        } else {
            prefix = "";
            local = localName;
            uri = defaultNamespaceURI;
        }

        if ("INTERCHANGE".equals(local)) {
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
                               String value) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
        setPrefix(prefix, namespaceURI);
    }

    @Override
    public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
        setDefaultNamespace(namespaceURI);
    }

    @Override
    public void writeComment(String data) throws XMLStreamException {
        throw new UnsupportedOperationException();
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
        // TODO Auto-generated method stub

    }

    @Override
    public void writeDTD(String dtd) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeEntityRef(String name) throws XMLStreamException {
        // TODO Auto-generated method stub

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
        if (Namespaces.ELEMENTS.equals(elementStack.element().getNamespaceURI())) {
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
        if (Namespaces.ELEMENTS.equals(elementStack.element().getNamespaceURI())) {
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
        return namespaces.entrySet()
                .stream()
                .filter(e -> e.getValue().equals(uri))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        namespaces.put(prefix, uri);
    }

    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {
        this.defaultNamespaceURI = uri;
    }

    @Override
    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        this.namespaceContext = context;
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return namespaceContext;
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        return null;
    }

}
