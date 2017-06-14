package io.xlate.edi.schema;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class StaEDISchemaFactory extends SchemaFactory {

	private static XMLInputFactory factory = XMLInputFactory.newFactory();

	private static final String reserved = "io.xlate.";

	private static final String xmlns = "http://xlate.io/2015/EDISchema";

	private static final QName QN_SCHEMA = new QName(xmlns, "schema");

	private static final QName QN_MAIN_LOOP = new QName(
			xmlns,
			"mainLoop");
	private static final QName QN_COMPOSITE_T = new QName(
			xmlns,
			"compositeType");
	private static final QName QN_ELEMENT_T = new QName(xmlns, "elementType");
	private static final QName QN_LOOP_T = new QName(xmlns, "loopType");
	private static final QName QN_SEGMENT_T = new QName(xmlns, "segmentType");

	private static final QName QN_SEQUENCE = new QName(xmlns, "sequence");

	private static final QName QN_COMPOSITE = new QName(xmlns, "composite");
	private static final QName QN_ELEMENT = new QName(xmlns, "element");
	private static final QName QN_LOOP = new QName(xmlns, "loop");
	private static final QName QN_SEGMENT = new QName(xmlns, "segment");

	private static final QName QN_SYNTAX = new QName(xmlns, "syntax");
	private static final QName QN_POSITION = new QName(xmlns, "position");

	private static final QName QN_ENUMERATION = new QName(xmlns, "enumeration");
	private static final QName QN_VALUE = new QName(xmlns, "value");

	private static final String ID_INTERCHANGE = reserved
			+ "edi.schema.INTERCHANGE";
	private static final String ID_GROUP = reserved + "edi.schema.GROUP";
	private static final String ID_TRANSACTION = reserved
			+ "edi.schema.TRANSACTION";

	private static final Map<QName, Integer> complex;
	private static final Set<QName> references;

	private static final Set<String> specialIdentifiers;

	static {
		complex = new HashMap<>(3);
		complex.put(QN_COMPOSITE_T, EDIType.TYPE_COMPOSITE);
		complex.put(QN_LOOP_T, EDIType.TYPE_LOOP);
		complex.put(QN_MAIN_LOOP, EDIType.TYPE_LOOP);
		complex.put(QN_SEGMENT_T, EDIType.TYPE_SEGMENT);

		references = new HashSet<>(3);
		references.add(QN_COMPOSITE);
		references.add(QN_ELEMENT);
		references.add(QN_LOOP);
		references.add(QN_SEGMENT);

		specialIdentifiers = new HashSet<>(3);
		specialIdentifiers.add(ID_INTERCHANGE);
		specialIdentifiers.add(ID_GROUP);
		specialIdentifiers.add(ID_TRANSACTION);
	}

	private final Map<String, Object> properties;
	private final Set<String> supportedProperties;

	public StaEDISchemaFactory() {
		properties = new HashMap<>();
		supportedProperties = new HashSet<>();
	}

	@Override
	public Schema createSchema(InputStream stream) throws EDISchemaException {
		StaEDISchema schema = new StaEDISchema();
		Map<String, EDIType> types = loadTypes(stream);
		validateReferences(types);
		schema.setTypes(types);

		return schema;
	}

	@Override
	public Schema createSchema(URL location) throws EDISchemaException {
		try {
			return createSchema(location.openStream());
		} catch (IOException e) {
			throw new EDISchemaException("Unable to open stream", e);
		}
	}

	static Map<String, EDIType> loadTypes(InputStream stream)
			throws EDISchemaException {

		Map<String, EDIType> types = new HashMap<>(100);

		try {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);

			schemaScan:
			while (reader.hasNext()) {
				switch (reader.next()) {
				case XMLStreamConstants.START_ELEMENT:
					QName element = reader.getName();

					if (element.equals(QN_SCHEMA)) {
						continue schemaScan;
					}

					if (element.equals(QN_MAIN_LOOP)) {
						if (types.containsKey(StaEDISchema.MAIN)) {
							throw new EDISchemaException(
									"Multiple mainLoop elements",
									reader.getLocation());
						}

						types.put(StaEDISchema.MAIN, buildComplexType(reader, element));
						continue;
					}

					String name = reader.getAttributeValue(null, "name");

					if (complex.containsKey(element)) {
						nameCheck(name, types, reader);
						types.put(name, buildComplexType(reader, element));
					} else if (QN_ELEMENT_T.equals(element)) {
						nameCheck(name, types, reader);
						types.put(name, buildSimpleType(reader));
					} else {
						throw new EDISchemaException("unknown element "
								+ element, reader.getLocation());
					}

					break;

				case XMLStreamConstants.END_ELEMENT:
					if (reader.getName().equals(QN_SCHEMA)) {
						break schemaScan;
					}
					break;
				default:
					checkEvent(reader);
					break;
				}
			}
		} catch (XMLStreamException e) {
			throw new EDISchemaException(e);
		}

		return types;
	}

	static void nameCheck(
			String name,
			Map<String, EDIType> types,
			XMLStreamReader reader) throws EDISchemaException {

		if (name == null) {
			throw new EDISchemaException(
					"missing type name",
					reader.getLocation());
		}

		if (types.containsKey(name)) {
			throw new EDISchemaException(
					"duplicate name: " + name,
					reader.getLocation());
		}
	}

	static void validateReferences(Map<String, EDIType> types)
			throws EDISchemaException {

		for (Entry<String, EDIType> entry : types.entrySet()) {
			EDIType type = entry.getValue();

			if (type instanceof EDISimpleType) {
				continue;
			}

			Structure struct = (Structure) type;

			for (EDIReference ref : struct.getReferences()) {
				Reference impl = (Reference) ref;
				EDIType target = types.get(impl.getRefId());

				if (target == null) {
					StringBuilder excp = new StringBuilder();
					if (StaEDISchema.MAIN.equals(struct.getId())) {
						excp.append(QN_MAIN_LOOP.getLocalPart());
					} else {
						excp.append("Type ");
						excp.append(struct.getId());
					}

					excp.append(" references undeclared ");
					excp.append(impl.getRefTag());
					excp.append(" with ref='");
					excp.append(impl.getRefId());
					excp.append('\'');
					throw new EDISchemaException(excp.toString());
				}

				final int refType = target.getTypeCode();

				if (refType != refTypeId(impl.getRefTag())) {
					StringBuilder excp = new StringBuilder();
					excp.append("Type '");
					excp.append(impl.getRefId());
					excp.append("' must not be referenced as \'");
					excp.append(impl.getRefTag());
					excp.append("\' in definition of type '");
					excp.append(struct.getId());
					excp.append('\'');
					throw new EDISchemaException(excp.toString());
				}

				switch (struct.getTypeCode()) {
				case EDIType.TYPE_LOOP: {
					if (refType != EDIType.TYPE_SEGMENT
							&& refType != EDIType.TYPE_LOOP) {
						StringBuilder excp = new StringBuilder();
						if (StaEDISchema.MAIN.equals(struct.getId())) {
							excp.append(QN_MAIN_LOOP.getLocalPart());
						} else {
							excp.append("Loop ");
							excp.append(struct.getId());
						}
						excp.append(" attempts to reference type with id = ");
						excp.append(impl.getRefId());
						excp.append(". Loops may only reference loop or segment types");
						throw new EDISchemaException(excp.toString());
					}

					/*if (refType == EDIType.TYPE_LOOP) {
						if (ref.getMinOccurs() > 0) {
							StringBuilder excp = new StringBuilder();
							excp.append("Reference to loop ");
							excp.append(impl.getRefId());
							excp.append(" must not specify minOccurs");
							throw new EDISchemaException(excp.toString());
						}
					}*/

					((Reference) ref).setReferencedType(target);
					break;
				}
				case EDIType.TYPE_SEGMENT: {
					if (refType != EDIType.TYPE_ELEMENT
							&& refType != EDIType.TYPE_COMPOSITE) {
						StringBuilder excp = new StringBuilder();
						excp.append("Segment ");
						excp.append(struct.getId());
						excp.append(" attempts to reference type with id = ");
						excp.append(impl.getRefId());
						excp.append(". Segments may only reference element or composite types");
						throw new EDISchemaException(excp.toString());
					}

					((Reference) ref).setReferencedType(target);
					break;
				}
				case EDIType.TYPE_COMPOSITE: {
					if (refType != EDIType.TYPE_ELEMENT) {
						StringBuilder excp = new StringBuilder();
						excp.append("Composite ");
						excp.append(struct.getId());
						excp.append(" attempts to reference type with id = ");
						excp.append(impl.getRefId());
						excp.append(". Composite may only reference element types");
						throw new EDISchemaException(excp.toString());
					}

					((Reference) ref).setReferencedType(target);
					break;
				}
				}
			}
		}
	}

	static int refTypeId(String tag) {
		if (QN_LOOP.getLocalPart().equals(tag)) {
			return EDIType.TYPE_LOOP;
		} else if (QN_SEGMENT.getLocalPart().equals(tag)) {
			return EDIType.TYPE_SEGMENT;
		} else if (QN_COMPOSITE.getLocalPart().equals(tag)) {
			return EDIType.TYPE_COMPOSITE;
		} else if (QN_ELEMENT.getLocalPart().equals(tag)) {
			return EDIType.TYPE_ELEMENT;
		} else {
			throw new IllegalArgumentException("Unexpected element: " + tag);
		}
	}

	/*static Structure buildRoot(XMLStreamReader reader)
			throws EDISchemaException, XMLStreamException {

		final int type = EDIType.TYPE_LOOP;
		final String name = StaEDISchema.MAIN;
		final List<EDIReference> references = new ArrayList<EDIReference>(8);
		final List<EDISyntaxRule> rules = Collections.emptyList();

		addReferences(reader, references);

		return new Structure(name, type, name, references, rules);
	}*/

	static Structure buildComplexType(XMLStreamReader reader, QName complexType)
			throws EDISchemaException, XMLStreamException {

		final int type = complex.get(complexType);
		final String name;

		if (complexType.equals(QN_MAIN_LOOP)) {
			name = StaEDISchema.MAIN;
		} else {
			name = reader.getAttributeValue(null, "name");

			if (type == EDIType.TYPE_SEGMENT && !name.matches("^[A-Z][A-Z0-9]{1,2}$")) {
				throw new EDISchemaException("Invalid segment name [" + name + ']', reader.getLocation());
			}
		}

		String code = reader.getAttributeValue(null, "code");

		if (code == null) {
			code = name;
		}

		final List<Reference> refs = new ArrayList<>(8);
		final List<SyntaxRestriction> rules = new ArrayList<>(2);
		boolean sequence = false;

		scan:
		while (reader.hasNext()) {
			switch (reader.next()) {
			case XMLStreamConstants.START_ELEMENT:
				QName element = reader.getName();

				if (element.equals(QN_SEQUENCE)) {
					if (sequence) {
						throw new EDISchemaException(
								"multiple sequence elements",
								reader.getLocation());
					}
					sequence = true;
					addReferences(reader, refs);
					continue;
				}

				if (element.equals(QN_SYNTAX)) {
					switch (type) {
					case EDIType.TYPE_SEGMENT:
					case EDIType.TYPE_COMPOSITE:
						addSyntax(reader, rules);
						continue;
					default:
						break;
					}
				}

				throw new EDISchemaException(
						"unexpected element " + element,
						reader.getLocation());

			case XMLStreamConstants.END_ELEMENT:
				if (reader.getName().equals(complexType)) {
					break scan;
				}
				break;
			default:
				checkEvent(reader);
				break;
			}
		}

		return new Structure(name, type, code, refs, rules);
	}

	static void addReferences(XMLStreamReader reader, List<Reference> refs)
			throws EDISchemaException, XMLStreamException {

		while (reader.hasNext()) {
			switch (reader.next()) {
			case XMLStreamConstants.START_ELEMENT:
				QName element = reader.getName();
				String tag = element.getLocalPart();

				if (!references.contains(element)) {
					throw new EDISchemaException(
							"unexpected element " + element,
							reader.getLocation());
				}

				String refId = reader.getAttributeValue(null, "ref");
				String min = reader.getAttributeValue(null, "minOccurs");
				String max = reader.getAttributeValue(null, "maxOccurs");

				final int minValue;

				try {
					minValue = min != null ? Integer.parseInt(min) : 0;
				} catch (@SuppressWarnings("unused") NumberFormatException e) {
					throw new EDISchemaException(
							"invalid minOccurs",
							reader.getLocation());
				}

				final int maxValue;

				try {
					maxValue = max != null ? Integer.parseInt(max) : 1;
				} catch (@SuppressWarnings("unused") NumberFormatException e) {
					throw new EDISchemaException(
							"invalid maxOccurs",
							reader.getLocation());
				}

				Reference ref = new Reference(refId, tag, minValue, maxValue);

				refs.add(ref);

				break;

			case XMLStreamConstants.END_ELEMENT:
				if (reader.getName().equals(QN_SEQUENCE)) {
					return;
				}
				if (reader.getName().equals(QN_MAIN_LOOP)) {
					return;
				}
				break;
			default:
				checkEvent(reader);
				break;
			}
		}
	}

	static void addSyntax(XMLStreamReader reader, List<SyntaxRestriction> rules)
			throws EDISchemaException, XMLStreamException {

		String type = reader.getAttributeValue(null, "type");
		int typeInt;

		if ("paired".equals(type)) {
			typeInt = EDISyntaxRule.SYNTAX_PAIRED;
		} else if ("required".equals(type)) {
			typeInt = EDISyntaxRule.SYNTAX_REQUIRED;
		} else if ("exclusion".equals(type)) {
			typeInt = EDISyntaxRule.SYNTAX_EXCLUSION;
		} else if ("conditional".equals(type)) {
			typeInt = EDISyntaxRule.SYNTAX_CONDITIONAL;
		} else if ("list".equals(type)) {
			typeInt = EDISyntaxRule.SYNTAX_LIST;
		} else {
			String message = "Invalid syntax 'type': [" + type + ']';
			throw new EDISchemaException(message, reader.getLocation());
		}

		SyntaxRestriction rule =
				new SyntaxRestriction(typeInt, buildSyntaxPositions(reader));

		rules.add(rule);
	}

	static List<Integer> buildSyntaxPositions(XMLStreamReader reader)
			throws EDISchemaException, XMLStreamException {

		final List<Integer> positions = new ArrayList<>(5);

		while (reader.hasNext()) {
			int event = reader.next();

			switch (event) {
			case XMLStreamConstants.START_ELEMENT:
				QName element = reader.getName();

				if (QN_POSITION.equals(element)) {
					String position = reader.getElementText();

					try {
						positions.add(Integer.parseInt(position));
					} catch (@SuppressWarnings("unused") NumberFormatException e) {
						throw new EDISchemaException(
								"invalid position",
								reader.getLocation());
					}
				}

				break;
			case XMLStreamConstants.END_ELEMENT:
				if (QN_SYNTAX.equals(reader.getName())) {
					return positions;
				}
				break;
			default:
				checkEvent(reader);
				break;
			}
		}

		throw new EDISchemaException(
				"missing end element " + QN_SYNTAX,
				reader.getLocation());
	}

	static Element buildSimpleType(XMLStreamReader reader)
			throws EDISchemaException, XMLStreamException {

		String name = reader.getAttributeValue(null, "name");
		String base = reader.getAttributeValue(null, "base");
		String nbr = reader.getAttributeValue(null, "number");
		String min = reader.getAttributeValue(null, "minLength");
		String max = reader.getAttributeValue(null, "maxLength");
		int number;

		try {
			number = nbr != null ? Integer.parseInt(nbr) : -1;
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			throw new EDISchemaException("invalid number", reader.getLocation());
		}

		int intBase;

		if ("string".equals(base)) {
			intBase = EDISimpleType.BASE_STRING;
		} else if ("identifier".equals(base)) {
			intBase = EDISimpleType.BASE_IDENTIFIER;
		} else if ("numeric".equals(base)) {
			intBase = EDISimpleType.BASE_INTEGER;
		} else if ("decimal".equals(base)) {
			intBase = EDISimpleType.BASE_DECIMAL;
		} else if ("date".equals(base)) {
			intBase = EDISimpleType.BASE_DATE;
		} else if ("time".equals(base)) {
			intBase = EDISimpleType.BASE_TIME;
		} else if ("binary".equals(base)) {
			intBase = EDISimpleType.BASE_BINARY;
		} else {
			String message = "Invalid element 'type': [" + base + ']';
			throw new EDISchemaException(message, reader.getLocation());
		}

		final int minLength;

		try {
			minLength = min != null ? Integer.parseInt(min) : 1;
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			throw new EDISchemaException(
					"invalid minLength",
					reader.getLocation());
		}

		final int maxLength;

		try {
			maxLength = max != null ? Integer.parseInt(max) : 1;
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			throw new EDISchemaException(
					"invalid maxLength",
					reader.getLocation());
		}

		final Element e;

		if (intBase == EDISimpleType.BASE_IDENTIFIER) {
			QName element;
			Set<String> values = new HashSet<>();

			idscan:
			while (reader.hasNext()) {
				switch (reader.next()) {
				case XMLStreamConstants.START_ELEMENT:
					element = reader.getName();

					if (element.equals(QN_ENUMERATION)) {
						continue;
					} else if (element.equals(QN_VALUE)) {
						values.add(reader.getElementText());
					} else {
						throw new EDISchemaException(
								"unexpected element " + element,
								reader.getLocation());
					}

					break;
				case XMLStreamConstants.END_ELEMENT:
					if (reader.getName().equals(QN_ENUMERATION)) {
						break idscan;
					}
				}
			}
			e = new Element(name, intBase, number, minLength, maxLength, values);
		} else {
			e = new Element(name, intBase, number, minLength, maxLength);
		}
		return e;
	}

	private static void checkEvent(XMLStreamReader reader)
			throws EDISchemaException {

		int event = reader.getEventType();

		switch (event) {
		case XMLStreamConstants.CHARACTERS:
		case XMLStreamConstants.SPACE:
			String text = reader.getText().trim();

			if (text.length() > 0) {
				throw new EDISchemaException(
						"unexpected xml [" + text + "]",
						reader.getLocation());
			}
			break;
		case XMLStreamConstants.COMMENT:
			// Ignore comments
			break;
		default:
			throw new EDISchemaException(
					"unexpected xml " + event,
					reader.getLocation());
		}
	}

	@Override
	public boolean isPropertySupported(String name) {
		return supportedProperties.contains(name);
	}

	@Override
	public Object getProperty(String name) throws IllegalArgumentException {
		if (!isPropertySupported(name)) {
			throw new IllegalArgumentException("Unsupported property: " + name);
		}

		return properties.get(name);
	}

	@Override
	public void setProperty(String name, Object value)
			throws IllegalArgumentException {

		if (!isPropertySupported(name)) {
			throw new IllegalArgumentException("Unsupported property: " + name);
		}

		properties.put(name, value);
	}

}
