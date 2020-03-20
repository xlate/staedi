package io.xlate.edi.internal.stream;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

import io.xlate.edi.stream.EDIStreamConstants.Namespaces;

class DocumentNamespaceContext implements NamespaceContext {
    private final Map<String, String> namespaces;

    DocumentNamespaceContext() {
        List<String> names = Namespaces.all();
        namespaces = new HashMap<>(names.size());
        for (String namespace : names) {
            String prefix = StaEDIXMLStreamReader.prefixOf(namespace);
            namespaces.put(namespace, prefix);
        }
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return namespaces.entrySet()
                .stream()
                .filter(e -> e.getValue().equals(prefix))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    @Override
    public String getPrefix(String namespaceURI) {
        return namespaces.get(namespaceURI);
    }

    @Override
    public Iterator<String> getPrefixes(String namespaceURI) {
        return Collections.singletonList(namespaces.get(namespaceURI)).iterator();
    }
}
