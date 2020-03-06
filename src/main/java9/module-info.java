module io.xlate.staedi {
    requires java.base;
    requires java.desktop;
    requires java.logging;
    requires transitive java.xml;

    exports io.xlate.edi.schema;
    exports io.xlate.edi.schema.implementation;
    exports io.xlate.edi.stream;
}
