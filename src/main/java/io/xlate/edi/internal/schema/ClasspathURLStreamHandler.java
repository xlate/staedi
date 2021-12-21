package io.xlate.edi.internal.schema;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

class ClasspathURLStreamHandler extends URLStreamHandler {
    /** The classloader to find resources from. */
    private final ClassLoader classLoader;

    public ClasspathURLStreamHandler(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        final String resourcePath = u.getPath();
        final URL resourceUrl = classLoader.getResource(resourcePath);

        if (resourceUrl == null) {
            throw new FileNotFoundException("Class-path resource not found: " + resourcePath);
        }

        return resourceUrl.openConnection();
    }
}
