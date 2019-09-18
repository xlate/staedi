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
package io.xlate.edi.internal;

import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

import io.xlate.edi.EDIFactoryConfigurationError;

/**
 * <p>
 * Implements pluggable Datatypes.
 * </p>
 */
public class FactoryFinder {
    private static final Logger LOGGER = Logger.getLogger("io.xlate.edi");

    /**
     * Security support class use to check access control before getting certain
     * system resources.
     */
    static SecuritySupport security = new SecuritySupport();

    private FactoryFinder() {
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
    private static Class<?> getProviderClass(String className,
                                             ClassLoader cl,
                                             boolean doFallback) throws ClassNotFoundException {
        // make sure we have access to restricted packages
        if (System.getSecurityManager() != null && className != null) {
            ClassLoader loader = FactoryFinder.class.getClassLoader();
            return Class.forName(className, true, loader);
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
                return Class.forName(className,
                                     true,
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
    public static <T> T newInstance(String className,
                             ClassLoader cl,
                             boolean doFallback) throws EDIFactoryConfigurationError {
        try {
            Class<?> providerClass = getProviderClass(className, cl, doFallback);
            @SuppressWarnings("unchecked")
            T instance = (T) providerClass.getConstructor().newInstance();

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE,
                           "created new instance of " + providerClass
                                   + " using ClassLoader: " + cl);
            }

            return instance;
        } catch (ClassNotFoundException x) {
            throw new EDIFactoryConfigurationError("Provider " + className + " not found",
                                                   x);
        } catch (Exception x) {
            throw new EDIFactoryConfigurationError("Provider " + className
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
     * @throws ClassNotFoundException
     */
    public static <T> T find(String factoryId, String fallbackClassName) throws EDIFactoryConfigurationError {

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "find factoryId =" + factoryId);
        }

        // Use the system property first
        try {
            String systemProp = security.getSystemProperty(factoryId);
            if (systemProp != null) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "found system property, value=" + systemProp);
                }
                return newInstance(systemProp, null, true);
            }
        } catch (SecurityException se) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "StaEDI security exception: ", se);
            }
        }

        // Try Jar Service Provider Mechanism
        T provider = findJarServiceProvider(factoryId);

        if (provider != null) {
            return provider;
        }

        if (fallbackClassName == null) {
            throw new EDIFactoryConfigurationError("Provider for " + factoryId
                    + " cannot be found", null);
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "loaded from fallback value: " + fallbackClassName);
        }

        return newInstance(fallbackClassName, null, true);
    }

    /*
     * Try to find provider using Jar Service Provider Mechanism
     *
     * @return instance of provider class if found or null
     */
    @SuppressWarnings("unchecked")
    private static <T> T findJarServiceProvider(String factoryId) throws EDIFactoryConfigurationError {
        String serviceId = "META-INF/services/" + factoryId;
        ClassLoader cl = getClassLoader(serviceId);
        Class<?> providerClass;

        try {
            providerClass = getProviderClass(factoryId, cl, true);
            ServiceLoader<?> services = ServiceLoader.load(providerClass, cl);
            return (T) StreamSupport.stream(services.spliterator(), false)
                                    .findFirst()
                                    .orElse(null);
        } catch (ClassNotFoundException e) {
            throw new EDIFactoryConfigurationError(e);
        }
    }

    private static ClassLoader getClassLoader(String serviceId) {
        // First try the Context ClassLoader
        ClassLoader loader = security.getContextClassLoader();

        if (loader != null && security.getResource(loader, serviceId) != null) {
            return loader;
        }

        return FactoryFinder.class.getClassLoader();
    }
}
