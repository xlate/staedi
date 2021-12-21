package io.xlate.edi.internal.schema;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClasspathURLStreamHandlerTest {

    ClasspathURLStreamHandler cut;

    @BeforeEach
    void setup() {
        cut = new ClasspathURLStreamHandler(getClass().getClassLoader());
    }

    @Test
    void testResourceUrlResolvesNull() throws MalformedURLException {
        URL target = new URL(null, "classpath:does/not/exist.txt", cut);
        @SuppressWarnings("unused")
        FileNotFoundException ex = assertThrows(FileNotFoundException.class, () -> {
            target.openConnection();
        });
    }

    @Test
    void testResourceUrlFound() throws IOException {
        URL target = new URL(null, "classpath:logging.properties", cut);
        URLConnection connection = target.openConnection();
        assertNotNull(connection);
    }

}
