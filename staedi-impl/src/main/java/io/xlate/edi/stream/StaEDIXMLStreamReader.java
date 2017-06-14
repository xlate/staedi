package io.xlate.edi.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.Queue;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import io.xlate.edi.stream.EDIStreamConstants.Events;

public class StaEDIXMLStreamReader implements XMLStreamReader {

	private static final Location location = new DefaultLocation();
	private static final QName DUMMY_QNAME = new QName("DUMMY");
	private static final QName INTERCHANGE = new QName("INTERCHANGE");

	private final EDIStreamReader ediReader;
	private final Queue<Integer> eventQueue = new ArrayDeque<>(3);
	private final Queue<QName> elementQueue = new ArrayDeque<>(3);
	private final Deque<QName> elementStack = new ArrayDeque<>();

	private final StringBuilder cdataBuilder = new StringBuilder();
	private char[] cdata;

	public StaEDIXMLStreamReader(EDIStreamReader ediReader)
			throws XMLStreamException {

		this.ediReader = ediReader;

		if (ediReader.getEventType() == Events.START_INTERCHANGE) {
			enqueueEvent(Events.START_INTERCHANGE);
		}
	}

	@Override
	public Object getProperty(String name) throws IllegalArgumentException {
		if (name == null) {
			throw new IllegalArgumentException("name must not be null");
		}
		return null;
	}

	private boolean isEvent(int eventType) {
		return eventQueue.element().equals(eventType);
	}

	private QName deriveName(QName parent, String hint) {
		String name = hint;//ediReader.getReferenceCode();

		if (name == null) {
			final io.xlate.edi.stream.Location l = ediReader.getLocation();
			final int componentPosition = l.getComponentPosition();

			if (componentPosition > 0) {
				name = String.format("%s-%d", parent, componentPosition);
			} else {
				name = String.format("%s%02d", parent, l.getElementPosition());
			}
		}

		return new QName(name);
	}

	private void enqueueEvent(int xmlEvent, QName element, boolean remember) {
		eventQueue.add(xmlEvent);
		elementQueue.add(element);

		if (remember) {
			elementStack.addFirst(element);
		}
	}

	private void advanceEvent() {
		eventQueue.remove();
		elementQueue.remove();
	}

	@SuppressWarnings("resource")
	private void enqueueEvent(int ediEvent) throws XMLStreamException {
		final QName name;
		cdataBuilder.setLength(0);
		cdata = null;

		switch(ediEvent) {
		case Events.ELEMENT_DATA: {
			name = deriveName(elementStack.getFirst(), null);
			enqueueEvent(START_ELEMENT, name, false);
			enqueueEvent(CHARACTERS, DUMMY_QNAME, false);
			enqueueEvent(END_ELEMENT, name, false);
			break;
		}
		case Events.ELEMENT_DATA_BINARY: {
			name = deriveName(elementStack.getFirst(), null);
			enqueueEvent(START_ELEMENT, name, false);
			enqueueEvent(CHARACTERS, DUMMY_QNAME, false);
			//TODO : this only will work if using a validation filter!
			InputStream input = ediReader.getBinaryData();
			OutputStream output = Base64.getEncoder().wrap(new OutputStream() {
				@Override
				public void write(int b) throws IOException {
					cdataBuilder.append((char) b);
				}
			});
			byte[] buffer = new byte[4096];
			int amount;

			try {
				while ((amount = input.read(buffer)) > -1) {
					output.write(buffer, 0, amount);
				}

				output.close();
			} catch (IOException e) {
				throw new XMLStreamException(e);
			}

			enqueueEvent(END_ELEMENT, name, false);
			break;
		}
		case Events.START_INTERCHANGE: {
			enqueueEvent(START_DOCUMENT, DUMMY_QNAME, false);
			enqueueEvent(START_ELEMENT, INTERCHANGE, true);
			break;
		}
		case Events.START_SEGMENT: {
			enqueueEvent(START_ELEMENT, new QName(ediReader.getText()), true);
			break;
		}
		case Events.START_LOOP: {
			name = deriveName(elementStack.getFirst(), ediReader.getText());
			enqueueEvent(START_ELEMENT, name, true);
			break;
		}
		case Events.START_COMPOSITE: {
			name = deriveName(elementStack.getFirst(), null);
			enqueueEvent(START_ELEMENT, name, true);
			break;
		}
		case Events.END_INTERCHANGE:
			enqueueEvent(END_ELEMENT, elementStack.removeFirst(), false);
			enqueueEvent(END_DOCUMENT, DUMMY_QNAME, false);
			break;
		case Events.END_LOOP:
		case Events.END_SEGMENT:
		case Events.END_COMPOSITE:
			enqueueEvent(END_ELEMENT, elementStack.removeFirst(), false);
			break;
		default:
			throw new IllegalStateException("Unknown state: " + ediEvent);
		}
	}

	@Override
	public int next() throws XMLStreamException {
		if (!eventQueue.isEmpty()) {
			advanceEvent();
		}

		if (eventQueue.isEmpty()) {
			try {
				enqueueEvent(ediReader.next());
			} catch (NoSuchElementException e) {
				throw new XMLStreamException(e);
			} catch (EDIStreamException e) {
				throw new XMLStreamException(e);
			}
		}

		return getEventType();
	}

	@Override
	public void require(int type, String namespaceURI, String localName)
			throws XMLStreamException {
		// TODO Auto-generated method stub

	}

	@SuppressWarnings("static-method")
	private void streamException(String message) throws XMLStreamException {
		throw new XMLStreamException(message);
	}

	@Override
	public String getElementText() throws XMLStreamException {
		if (ediReader.getEventType() != Events.ELEMENT_DATA) {
			streamException("Element text only available for simple element");
		}

		if (getEventType() != START_ELEMENT) {
			streamException("Element text only available on START_ELEMENT");
		}

		int eventType = next();

		if (eventType != CHARACTERS) {
			streamException("Unexpected event type: " + eventType);
		}

		final String text = getText();
		eventType = next();

		if (eventType != END_ELEMENT) {
			streamException("Unexpected event type after text " + eventType);
		}

		return text;
	}

	@Override
	public int nextTag() throws XMLStreamException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasNext() throws XMLStreamException {
		try {
			return ediReader.hasNext();
		} catch (EDIStreamException e) {
			throw new XMLStreamException(e);
		}
	}

	@Override
	public void close() throws XMLStreamException {
		try {
			ediReader.close();
		} catch (IOException e) {
			throw new XMLStreamException(e);
		}
	}

	@Override
	public String getNamespaceURI(String prefix) {
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
		return isEvent(CHARACTERS);
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
		return 0;
	}

	@Override
	public String getNamespacePrefix(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getNamespaceURI(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NamespaceContext getNamespaceContext() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getEventType() {
		return eventQueue.element();
	}

	@Override
	public String getText() {
		if (isCharacters()) {
			if (cdataBuilder.length() > 0) {
				return cdata.toString();
			}
			return ediReader.getText();
		}
		throw new IllegalStateException("Text only available for CHARACTERS");
	}

	@Override
	public char[] getTextCharacters() {
		if (isCharacters()) {
			if (cdataBuilder.length() > 0) {
				if (cdata == null) {
					cdata = new char[cdataBuilder.length()];
					cdataBuilder.getChars(0, cdataBuilder.length(), cdata, 0);
				}

				return cdata;
			}
			return ediReader.getTextCharacters();
		}
		throw new IllegalStateException("Text only available for CHARACTERS");
	}

	@Override
	public int getTextCharacters(
			int sourceStart,
			char[] target,
			int targetStart,
			int length) throws XMLStreamException {
		if (isCharacters()) {
			if (cdataBuilder.length() > 0) {
				if (cdata == null) {
					cdata = new char[cdataBuilder.length()];
					cdataBuilder.getChars(0, cdataBuilder.length(), cdata, 0);
				}

				// FIXME: array bounds check needed
				System.arraycopy(cdata, sourceStart, target, targetStart, length);
				return length;
			}
			return ediReader.getTextCharacters(sourceStart, target, targetStart, length);
		}
		throw new IllegalStateException("Text only available for CHARACTERS");
	}

	@Override
	public int getTextStart() {
		if (isCharacters()) {
			if (cdataBuilder.length() > 0) {
				return 0;
			}
			return ediReader.getTextStart();
		}
		throw new IllegalStateException("Text only available for CHARACTERS");
	}

	@Override
	public int getTextLength() {
		if (isCharacters()) {
			if (cdataBuilder.length() > 0) {
				return cdataBuilder.length();
			}
			return ediReader.getTextLength();
		}
		throw new IllegalStateException("Text only available for CHARACTERS");
	}

	@Override
	public String getEncoding() {
		// TODO Auto-generated method stub
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
			return elementQueue.element();
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

	private static class DefaultLocation implements Location {
		@Override
		public int getLineNumber() {
			return -1;
		}

		@Override
		public int getColumnNumber() {
			return -1;
		}

		@Override
		public int getCharacterOffset() {
			return -1;
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
