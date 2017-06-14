package io.xlate.edi.schema;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class StaEDISchema extends Schema implements Externalizable {

	private static final long serialVersionUID = -1469959633026577070L;

	static final String MAIN = "io.xlate.edi.schema.MAIN";

	transient final SchemaProxy proxy = new SchemaProxy(this);

	private Map<String, EDIType> types = Collections.emptyMap();
	private EDIComplexType mainLoop = null;

	public StaEDISchema() {}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in)
			throws IOException, ClassNotFoundException {
		if (in.readBoolean()) {
			mainLoop = (EDIComplexType) in.readObject();
		} else {
			mainLoop = null;
		}

		types = (Map<String, EDIType>) in.readObject();

		types.values().parallelStream()
			.filter(type -> type instanceof EDIComplexType)
			.map(type -> (EDIComplexType) type)
			.forEach(type -> type.getReferences().stream()
					.filter(ref -> ref instanceof Reference)
					.forEach(ref -> ((Reference) ref).setSchema(proxy)));
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		if (mainLoop != null) {
			out.writeBoolean(true);
			out.writeObject(mainLoop);
		} else {
			out.writeBoolean(false);
		}

		out.writeObject(types);
	}

	@Override
	public EDIComplexType getMainLoop() {
		return mainLoop;
	}

	void setTypes(Map<String, EDIType> types) throws EDISchemaException {
		if (types == null) {
			throw new NullPointerException("types cannot be null");
		}

		this.types = Collections.unmodifiableMap(types);

		if (!types.containsKey(MAIN)) {
			throw new EDISchemaException("main loop not in schema");
		}

		this.mainLoop = (EDIComplexType) types.get(MAIN);
	}

	@Override
	public EDIType getType(String name) {
		return types.get(name);
	}

	@Override
	public boolean containsSegment(String name) {
		final EDIType type = types.get(name);
		return type != null && type.getTypeCode() == EDIType.TYPE_SEGMENT;
	}

	@Override
	public Iterator<EDIType> iterator() {
		return types.values().iterator();
	}

	@Override
	public Schema reference(
			Schema referenced,
			EDIComplexType parent,
			EDIReference child) throws EDISchemaException {

		if (parent == null || parent.getTypeCode() != EDIType.TYPE_LOOP) {
			throw new EDISchemaException("parent must be loop");
		}

		final String parentId = parent.getId();
		final EDIType knownParent = this.types.get(parentId);

		if (knownParent != parent) {
			throw new EDISchemaException("parent not in this schema");
		}

		EDIReference knownChild = null;
		@SuppressWarnings("unchecked")
		List<Reference> references = (List<Reference>) parent.getReferences();

		for (EDIReference reference : references) {
			if (reference == child) {
				knownChild = reference;
				break;
			}
		}

		if (knownChild != child) {
			throw new EDISchemaException("child is not referenced by parent");
		}

		EDIComplexType main = referenced.getMainLoop();

		if (main == null || main.getTypeCode() != EDIType.TYPE_LOOP) {
			throw new EDISchemaException("referenced schema root must be loop");
		}

		StaEDISchema merged = new StaEDISchema();
		merged.types = new HashMap<>(this.types);
		merged.mainLoop = merged.attach(main, this.mainLoop, parent, child);

		if (referenced instanceof StaEDISchema) {
			((StaEDISchema) referenced).proxy.setSchema(merged);
		}

		//TODO: Consider :: should we overwrite?
		referenced.forEach(t -> merged.types.putIfAbsent(t.getId(), t));

		return merged;
	}

	private EDIComplexType attach(
			EDIComplexType referenced,
			EDIComplexType root,
			EDIComplexType parent,
			EDIReference child) {

		if (root != parent) {
			@SuppressWarnings("unchecked")
			List<Reference> references = (List<Reference>) root.getReferences();

			for (int i = 0, m = references.size(); i < m; i++) {
				Reference reference = references.get(i);
				EDIType type = reference.getReferencedType();

				if (type.getTypeCode() != EDIType.TYPE_LOOP) {
					continue;
				}

				EDIComplexType loop = (EDIComplexType) type;
				EDIComplexType newParent;
				newParent = attach(referenced, loop, parent, child);

				if (newParent == null) {
					continue;
				}

				return replaceReference(root, references, reference, i, newParent);
			}
		} else {
			return addReference(referenced, root, parent, child);
		}

		return null;
	}

	private EDIComplexType replaceReference(
			EDIComplexType loop,
			List<Reference> references,
			EDIReference reference,
			int i,
			EDIComplexType newTarget) {

		references = new ArrayList<>(references);
		int minLoopOccurs = reference.getMinOccurs();
		int maxLoopOccurs = reference.getMaxOccurs();

		final Reference newReference;
		newReference = new Reference(newTarget, minLoopOccurs, maxLoopOccurs);
		references.set(i, newReference);

		final EDIComplexType newLoop;
		newLoop = new Structure(loop, references, getSyntaxRules(loop));
		types.put(newLoop.getId(), newLoop);
		return newLoop;
	}

	@SuppressWarnings("unchecked")
	static List<SyntaxRestriction> getSyntaxRules(EDIComplexType type) {
		return (List<SyntaxRestriction>) type.getSyntaxRules();
	}

	@SuppressWarnings("unchecked")
	static List<Reference> getReferences(EDIComplexType type) {
		return (List<Reference>) type.getReferences();
	}

	private EDIComplexType addReference(
			EDIComplexType referenced,
			EDIComplexType root,
			EDIComplexType parent,
			EDIReference child) {

		List<Reference> references = getReferences(root);
		int index = 0;

		for (Reference reference : references) {
			if (reference == child) {
				break;
			}
			index++;
		}

		EDIComplexType newParent;

		references = new ArrayList<>(references);

		for (EDIReference reference : referenced.getReferences()) {
			Reference newRef = new Reference(reference);
			newRef.setSchema(proxy);
			references.add(index++, newRef);
		}

		newParent = new Structure(parent, references, getSyntaxRules(parent));
		types.put(newParent.getId(), newParent);
		return newParent;
	}

	static class SchemaProxy extends Schema {
		private static final long serialVersionUID = 1L;

		private ThreadLocal<Schema> proxy = new ThreadLocal<>();
		private ThreadLocal<Map<String, EDIType>> controlTypes;

		public SchemaProxy(StaEDISchema schema) {
			proxy.set(schema);
			controlTypes = new ThreadLocal<>();
			controlTypes.set(Collections.emptyMap());
		}

		void setSchema(StaEDISchema schema) {
			proxy.set(schema);
		}

		void setControlTypes(Map<String, EDIType> controlTypes) {
			this.controlTypes.set(controlTypes);
		}

		@Override
		public Iterator<EDIType> iterator() {
			return proxy.get().iterator();
		}

		@Override
		public EDIComplexType getMainLoop() {
			return proxy.get().getMainLoop();
		}

		@Override
		public EDIType getType(String name) {
			EDIType type = proxy.get().getType(name);
			return type != null ?
					type :
					controlTypes.get().get(name);
		}

		@Override
		public boolean containsSegment(String name) {
			return proxy.get().containsSegment(name);
		}

		@Override
		public Schema reference(
				Schema referenced,
				EDIComplexType parent,
				EDIReference child) throws EDISchemaException {

			return proxy.get().reference(referenced, parent, child);
		}
	}
}
