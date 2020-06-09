/**
 * StAEDI is a streaming API for EDI reading, writing, and validation in Java.
 *
 * @author Michael Edgar
 * @see <a href="https://github.com/xlate/staedi">StAEDI on GitHub</a>
 */
module io.xlate.staedi {
    requires java.base;
    requires java.logging;
    requires transitive java.xml;

    exports io.xlate.edi.schema;
    exports io.xlate.edi.schema.implementation;
    exports io.xlate.edi.stream;
}
