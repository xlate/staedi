package io.xlate.edi.schema;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

class SecuritySupport {

	ClassLoader getContextClassLoader() throws SecurityException {
		return AccessController.doPrivileged(new GetContextClassLoader());
	}

	String getSystemProperty(final String name) {
		return AccessController.doPrivileged(new GetSystemProperty(name));
	}

	InputStream getInputStream(final File file) throws FileNotFoundException {
		try {
			return AccessController.doPrivileged(new GetInputStream(file));
		} catch (PrivilegedActionException e) {
			throw (FileNotFoundException) e.getException();
		}
	}

	URL getResource(ClassLoader loader, String name) {
		GetResource action = new GetResource(loader, name);
		return AccessController.doPrivileged(action);
	}

	InputStream getResourceAsStream(ClassLoader loader, String name) {
		GetResourceAsStream action = new GetResourceAsStream(loader, name);
		return AccessController.doPrivileged(action);
	}

	boolean doesFileExist(final File file) {
		DoesFileExist action = new DoesFileExist(file);
		return AccessController.doPrivileged(action).booleanValue();
	}

	class GetContextClassLoader implements PrivilegedAction<ClassLoader> {
		@Override
		public ClassLoader run() {
			final Thread currentThread = Thread.currentThread();
			ClassLoader loader = currentThread.getContextClassLoader();

			if (loader == null) {
				loader = ClassLoader.getSystemClassLoader();
			}

			return loader;
		}
	}

	class GetSystemProperty implements PrivilegedAction<String> {
		private final String name;

		public GetSystemProperty(String name) {
			this.name = name;
		}

		@Override
		public String run() {
			return System.getProperty(name);
		}
	}

	class GetInputStream implements PrivilegedExceptionAction<InputStream> {
		private final File file;

		public GetInputStream(File file) {
			this.file = file;
		}

		@Override
		public InputStream run() throws FileNotFoundException {
			return new FileInputStream(file);
		}
	}

	class GetResource implements PrivilegedAction<URL> {
		final ClassLoader loader;
		final String name;

		public GetResource(ClassLoader loader, String name) {
			super();
			this.loader = loader;
			this.name = name;
		}

		@Override
		public URL run() {
			final URL locator;

			if (loader == null) {
				locator = ClassLoader.getSystemResource(name);
			} else {
				locator = loader.getResource(name);
			}

			return locator;
		}
	}

	class GetResourceAsStream implements PrivilegedAction<InputStream> {
		final ClassLoader loader;
		final String name;

		public GetResourceAsStream(ClassLoader loader, String name) {
			super();
			this.loader = loader;
			this.name = name;
		}

		@Override
		public InputStream run() {
			final InputStream stream;
			
			if (loader == null) {
				stream = ClassLoader.getSystemResourceAsStream(name);
			} else {
				stream = loader.getResourceAsStream(name);
			}
			
			return stream;
		}
	}
	
	class DoesFileExist implements PrivilegedAction<Boolean> {
		private final File file;
		
		public DoesFileExist(File file) {
			this.file = file;
		}

		@Override
		public Boolean run() {
			return new Boolean(file.exists());
		}
	}
}
