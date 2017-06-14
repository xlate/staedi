package io.xlate.edi.schema;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mapdb.DB;
import org.mapdb.DBMaker;

public class StaEDIMapSchemaFactory extends StaEDISchemaFactory {

	public static final String ID = StaEDIMapSchemaFactory.class.getName();

	static Map<String, DB> databases = new HashMap<>(1);
	static Set<String> loadedControlTypes = new HashSet<>(6);

	static synchronized DB getDB(String dbName) {
		if (databases.containsKey(dbName)) {
			return databases.get(dbName);
		}

		File dbFile = new File(dbName);
		DB db = DBMaker
				.newFileDB(dbFile)
				.transactionDisable()
				.compressionEnable()
				.readOnly()
				.mmapFileEnable()
				.make();

		databases.put(dbName, db);

		return db;
	}

	@Override
	public Schema createSchema(InputStream stream) throws EDISchemaException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Schema createSchema(URL location) throws EDISchemaException {
		String dbName = location.getPath();
		DB db = getDB(dbName);
		String[] path = location.getRef().split("/");
		String control = path[0];
		String release = path.length > 2 ? path[1] : control;
		String message = path.length > 2 ? path[2] : path[1];
		String mapKey = "messages." + release;
		final StaEDISchema schema;

		if (db.exists(mapKey)) {
			Map<String, StaEDISchema> messages = db.getTreeMap(mapKey);

			schema = messages.get(message);

			if (schema != null && !release.equals(control)) {
				Map<String, EDIType> types = getControlTypes(db, control);
				schema.proxy.setControlTypes(types);
			}
		} else {
			throw new EDISchemaException("release not supported");
		}

		return schema;
	}

	private static Map<String, EDIType> getControlTypes(DB db, String version) {
		final String key = "types." + version;
		Map<String, EDIType> types = db.getTreeMap(key);

		if (loadedControlTypes.contains(key)) {
			return types;
		}

		types.values().parallelStream()
			.filter(type -> type instanceof EDIComplexType)
			.map(type -> (EDIComplexType) type)
			.forEach(type -> type.getReferences().stream()
					.filter(ref -> ref instanceof Reference)
					.forEach(ediRef -> {
						Reference ref = (Reference) ediRef;
						String refId = ref.getRefId();
						ref.setReferencedType(types.get(refId));
					}));

		loadedControlTypes.add(key);

		return types;
	}
}
