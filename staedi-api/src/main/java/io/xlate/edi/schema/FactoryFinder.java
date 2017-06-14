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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * <p>
 * Implements pluggable Datatypes.
 * </p>
 * 
 * <p>
 * This class is duplicated for each StAEDI subpackage so keep it in sync. It is
 * package private for secure class loading.
 * </p>
 *
 * @author Santiago.PericasGeertsen@sun.com
 */
class FactoryFinder {
	private static final String DEFAULT_PACKAGE = "io.xlate.edi.schema.";

	/**
	 * Internal debug flag.
	 */
	private static boolean debug = false;

	/**
	 * Cache for properties in java.home/lib/staedi.properties
	 */
	static Properties cacheProps = new Properties();

	/**
	 * Flag indicating if properties from java.home/lib/staedi.properties have
	 * been cached.
	 */
	static boolean firstTime = true;

	/**
	 * Security support class use to check access control before getting certain
	 * system resources.
	 */
	static SecuritySupport security = new SecuritySupport();

	// Define system property "staedi.debug" to get output
	static {
		// Use try/catch block to support applets, which throws
		// SecurityException out of this code.
		try {
			String val = security.getSystemProperty("staedi.debug");
			// Allow simply setting the prop to turn on debug
			debug = val != null && !"false".equals(val);
		} catch (@SuppressWarnings("unused") SecurityException e) {
			debug = false;
		}
	}

	private static void dPrint(String msg) {
		if (debug) {
			System.err.println("StAEDI: " + msg);
		}
	}

	/**
	 * Attempt to load a class using the class loader supplied. If that fails
	 * and fall back is enabled, the current (i.e. bootstrap) class loader is
	 * tried.
	 * 
	 * If the class loader supplied is <code>null</code>, first try using the
	 * context class loader followed by the current (i.e. bootstrap) class
	 * loader.
	 */
	static private Class<?> getProviderClass(String className, ClassLoader cl,
			boolean doFallback) throws ClassNotFoundException {
		// make sure we have access to restricted packages
		if (System.getSecurityManager() != null) {
			if (className != null && className.startsWith(DEFAULT_PACKAGE)) {
				ClassLoader loader = FactoryFinder.class.getClassLoader();
				return Class.forName(className, true, loader);
			}
		}

		try {
			if (cl == null) {
				cl = security.getContextClassLoader();
				if (cl == null) {
					throw new ClassNotFoundException();
				}
				return cl.loadClass(className);
			}
			return cl.loadClass(className);
		} catch (ClassNotFoundException e1) {
			if (doFallback) {
				// Use current class loader - should always be bootstrap CL
				return Class.forName(className, true,
						FactoryFinder.class.getClassLoader());
			}
			throw e1;
		}
	}

	/**
	 * Create an instance of a class. Delegates to method
	 * <code>getProviderClass()</code> in order to load the class.
	 *
	 * @param className
	 *            Name of the concrete class corresponding to the service
	 *            provider
	 *
	 * @param cl
	 *            ClassLoader to use to load the class, null means to use the
	 *            bootstrap ClassLoader
	 *
	 * @param doFallback
	 *            True if the current ClassLoader should be tried as a fallback
	 *            if the class is not found using cl
	 */
	static <T> T newInstance(String className, ClassLoader cl,
			boolean doFallback) throws ConfigurationError {
		try {
			Class<?> providerClass = getProviderClass(className, cl, doFallback);
			@SuppressWarnings("unchecked")
			T instance = (T) providerClass.newInstance();

			if (debug) { // Extra check to avoid computing cl strings
				dPrint("created new instance of " + providerClass
						+ " using ClassLoader: " + cl);
			}

			return instance;
		} catch (ClassNotFoundException x) {
			throw new ConfigurationError(
					"Provider " + className + " not found", x);
		} catch (Exception x) {
			throw new ConfigurationError("Provider " + className
					+ " could not be instantiated: " + x, x);
		}
	}

	/**
	 * Finds the implementation Class object in the specified order. Main entry
	 * point.
	 * 
	 * @return Class object of factory, never null
	 *
	 * @param factoryId
	 *            Name of the factory to find, same as a property name
	 * @param fallbackClassName
	 *            Implementation class name, if nothing else is found. Use null
	 *            to mean no fallback.
	 *
	 *            Package private so this code can be shared.
	 */
	static <T> T find(String factoryId, String fallbackClassName)
			throws ConfigurationError {
		dPrint("find factoryId =" + factoryId);

		// Use the system property first
		try {
			String systemProp = security.getSystemProperty(factoryId);
			if (systemProp != null) {
				dPrint("found system property, value=" + systemProp);
				return newInstance(systemProp, null, true);
			}
		} catch (SecurityException se) {
			if (debug)
				se.printStackTrace();
		}

		// Try Jar Service Provider Mechanism
		@SuppressWarnings("unchecked")
		T provider = (T) findJarServiceProvider(factoryId);

		if (provider != null) {
			return provider;
		}

		if (fallbackClassName == null) {
			throw new ConfigurationError("Provider for " + factoryId
					+ " cannot be found", null);
		}

		dPrint("loaded from fallback value: " + fallbackClassName);
		return newInstance(fallbackClassName, null, true);
	}

	/*
	 * Try to find provider using Jar Service Provider Mechanism
	 * 
	 * @return instance of provider class if found or null
	 */
	private static Object findJarServiceProvider(String factoryId)
			throws ConfigurationError {
		String serviceId = "META-INF/services/" + factoryId;
		String factoryClassName = null;

		ClassLoader loader = getClassLoader(serviceId);

		try (InputStream is = getResourceAsStream(loader, serviceId)) {
			if (is != null) {
				Charset utf8 = StandardCharsets.UTF_8;
				Reader streamReader = new InputStreamReader(is, utf8);

				try (BufferedReader rd = new BufferedReader(streamReader)) {
					// XXX Does not handle all possible input as specified by the
					// Jar Service Provider specification
					factoryClassName = rd.readLine();
				} catch (@SuppressWarnings("unused") IOException e) {
					// Not found
				}
			}
		} catch (@SuppressWarnings("unused") IOException e) {
			// Error closing stream, ignore
		}

		if (factoryClassName != null && !"".equals(factoryClassName)) {
			dPrint("found in resource, value=" + factoryClassName);

			// Note: here we do not want to fall back to the current
			// ClassLoader because we want to avoid the case where the
			// resource file was found using one ClassLoader and the
			// provider class was instantiated using a different one.
			return newInstance(factoryClassName, loader, false);
		}

		// No provider found
		return null;
	}

	private static InputStream getResourceAsStream(ClassLoader loader, String serviceId) {
		InputStream is = security.getResourceAsStream(loader, serviceId);

		if (is == null) {
			// No provider found
			return null;
		}

		if (debug) { // Extra check to avoid computing cl strings
			dPrint("found jar resource=" + serviceId + " using ClassLoader: "
					+ loader);
		}

		return is;
	}

	private static ClassLoader getClassLoader(String serviceId) {
		// First try the Context ClassLoader
		ClassLoader loader = security.getContextClassLoader();

		if (loader != null) {
			if (security.getResource(loader, serviceId) != null) {
				return loader;
			}
		}

		return FactoryFinder.class.getClassLoader();
	}

	@SuppressWarnings("serial")
	static class ConfigurationError extends Error {
		private Exception exception;

		/**
		 * Construct a new instance with the specified detail string and
		 * exception.
		 */
		ConfigurationError(String msg, Exception x) {
			super(msg);
			this.exception = x;
		}

		Exception getException() {
			return exception;
		}
	}

}
