/*******************************************************************************
 * Copyright 2017 xlate.io LLC, http://www.xlate.io
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package io.xlate.edi.schema;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.TreeMap;

import io.xlate.edi.stream.EDIStreamConstants;

public class SchemaUtils {

	static Properties controlIndex = new Properties();
	static NavigableMap<String, String> controlVersions = new TreeMap<>();
	static NavigableMap<String, Schema> controlSchemas = new TreeMap<>();

    static {
        try {
            Enumeration<URL> resources = getStreams("staedi-control-index.properties");

            while (resources.hasMoreElements()) {
                controlIndex.load(resources.nextElement().openStream());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (Map.Entry<Object, Object> entry : controlIndex.entrySet()) {
            final String entryKey = entry.getKey().toString();
            final String entryValue = entry.getValue().toString();

            controlVersions.put(entryKey, entryValue);
            controlSchemas.put(entryKey, null);
        }
    }

	public static Enumeration<URL> getStreams(String resource) throws IOException {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		return loader.getResources(resource);
	}

	public static URL getURL(String resource) {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		return loader.getResource(resource);
	}

	public static Schema getControlSchema(String standard, String version)
			throws EDISchemaException {

		String key = standard + '.' + version;
		Entry<String, Schema> controlEntry = controlSchemas.floorEntry(key);

		if (controlEntry != null && controlEntry.getValue() != null) {
			return controlEntry.getValue();
		}

		if (EDIStreamConstants.Standards.X12.equals(standard)) {
			Entry<String, String> pathEntry = controlVersions.floorEntry(key);
			if (pathEntry != null) {
				Schema created = getXmlSchema(pathEntry.getValue());
				controlSchemas.put(pathEntry.getKey(), created);
				return created;
			}

			return null;
		} else if (EDIStreamConstants.Standards.EDIFACT.equals(standard)) {
			Schema created = getMapSchema(standard, version, "INTERCHANGE");
			controlSchemas.putIfAbsent(key, created);
			return created;
		}

		throw new IllegalArgumentException(standard);
	}

	public static Schema getMapSchema(String standard, String version, String message)
			throws EDISchemaException {

		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		SchemaFactory schemaFactory = SchemaFactory.newFactory(StaEDIMapSchemaFactory.ID, loader);
		final URL location;
		final String stdDir;

		if (System.getProperties().containsKey("io.xlate.edi.standards")) {
			stdDir = System.getProperty("io.xlate.edi.standards");
		} else {
			stdDir = System.getenv("EDI_STANDARDS");
		}

		final URL context;

		if (stdDir != null) {
			File db = new File(stdDir + "/" + standard.toLowerCase() + ".db");

			try {
				context = db.toURI().toURL();
			} catch (MalformedURLException e) {
				throw new EDISchemaException(e);
			}
		} else {
			final String source = standard.toUpperCase() + "/";
			context = getURL(source + standard.toLowerCase() + ".db");
		}

		try {
			location = new URL(context, "#" + version + "/" + message);
		} catch (MalformedURLException e) {
			throw new EDISchemaException(e);
		}

		return schemaFactory.createSchema(location);
	}

	private static Schema getXmlSchema(String resource)
			throws EDISchemaException {

		SchemaFactory schemaFactory = SchemaFactory.newFactory();
		URL location = getURL(resource);
		return schemaFactory.createSchema(location);
	}
}
