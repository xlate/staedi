package io.xlate.edi.internal.bind;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class TransactionBindTest {

    final String XSD = "http://www.w3.org/2001/XMLSchema";
    Map<String, Element> complexTypes;
    Map<String, Element> simpleTypes;

    @Test
    void testElementBinding() throws Exception {
        JAXBContext context = JAXBContext.newInstance(TestTx.class);
        final Map<String, DOMResult> results = new HashMap<>();
        context.generateSchema(new SchemaOutputResolver() {
            @Override
            public Result createOutput(String ns, String file) throws IOException {
                DOMResult result = new DOMResult();
                result.setSystemId(file);
                results.put(ns, result);
                return result;
            }
        });
        //assertEquals(1, results.size());

        QName rootElementName = getRootElementName(TestTx.class);

        for (DOMResult result : results.values()) {
            StringWriter writer = new StringWriter();
            try {
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                transformer.transform(new DOMSource(result.getNode()), new StreamResult(writer));
                String xml = writer.toString();
                System.out.println(xml);
            } catch (Exception e) {
                fail("Unexpected exception: " + e.getMessage());
            }
        }

        Document doc = (Document) results.get(rootElementName.getNamespaceURI()).getNode();

        Element rootElement = elementStream(doc.getElementsByTagNameNS(XSD, "element"))
                                .filter(e -> rootElementName.getLocalPart().equals(e.getAttribute("name")))
                                .findFirst()
                                .orElse(null);

        complexTypes = elementStream(doc.getElementsByTagNameNS(XSD, "complexType"))
                .collect(Collectors.toMap(e -> e.getAttribute("name"), e -> e));
        simpleTypes = elementStream(doc.getElementsByTagNameNS(XSD, "simpleType"))
                .collect(Collectors.toMap(e -> e.getAttribute("name"), e -> e));

        Element rootType = complexTypes.get(rootElement.getAttribute("type"));
        print("", elementStream(rootType.getElementsByTagNameNS(XSD, "element")), new ArrayDeque<>(Arrays.asList(TestTx.class)));
    }

    QName getRootElementName(Class<?> type) {
        XmlRootElement rootElement = type.getAnnotation(XmlRootElement.class);
        String ns;
        String localName;

        if (rootElement != null) {
            if ("##default".equals(rootElement.name())) {
                localName = decapitalize(type.getSimpleName());
            } else {
                localName = rootElement.name();
            }
            if ("##default".equals(rootElement.namespace())) {
                ns = "";
            } else {
                ns = rootElement.namespace();
            }

            return new QName(ns, localName);
        }

        throw new IllegalStateException("Missing XmlRootElement annotation on root class");
    }

    /**
     * Utility method to take a string and convert it to normal Java variable
     * name capitalization.  This normally means converting the first
     * character from upper case to lower case, but in the (unusual) special
     * case when there is more than one character and both the first and
     * second characters are upper case, we leave it alone.
     * <p>
     * Thus "FooBah" becomes "fooBah" and "X" becomes "x", but "URL" stays
     * as "URL".
     *
     * COPYIED FROM java.beans.Introspector
     *
     * @param  name The string to be decapitalized.
     * @return  The decapitalized version of the string.
     */
    static String decapitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) &&
                        Character.isUpperCase(name.charAt(0))){
            return name;
        }
        char chars[] = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    Stream<Element> elementStream(NodeList nodes) {
        return IntStream.range(0, nodes.getLength()).mapToObj(nodes::item).map(Element.class::cast);
    }

    Element getElement(Element parent, String ns, String name) {
        return elementStream(parent.getElementsByTagNameNS(ns, name)).findFirst().orElse(null);
    }

    void print(String pad, Stream<Element> nodes, Deque<Class<?>> stack) {
        nodes.forEach(node -> {
            String typeName = node.getAttribute("type");
            boolean isStructure = complexTypes.containsKey(typeName);
            boolean isSimpleType = simpleTypes.containsKey(typeName);
            boolean isExtension = node.hasAttribute("base");
            Class<?> clazz = null;

            if (isExtension) {
                clazz = classForValue(stack.peekFirst());
                System.out.printf("%s%s: %s\n", pad, node.getAttribute("base"), clazz);
            } else {
                String name = node.getAttribute("name");
                clazz = classForElement(stack.peekFirst(), name);
                System.out.printf("%s%s: %s (%s)\n", pad, name, isStructure ? "***" : typeName, clazz);
            }

            if (isStructure || isSimpleType) {
                Element type = isStructure ? complexTypes.get(typeName) : simpleTypes.get(typeName);
                if (clazz != null)
                    stack.offerFirst(clazz);

                if (isStructure)
                    print(pad + "  ", elementStream(type.getElementsByTagNameNS(XSD, "element")), stack);
                else
                    print(pad + "  ", elementStream(type.getElementsByTagNameNS(XSD, "restriction")), stack);

                if (clazz != null)
                    stack.removeFirst();
            }
        });
    }

    Class<?> classForElement(Class<?> bean, String name) {
        return getAnnotatedClass(bean, XmlElement.class, a -> a.getAnnotation(XmlElement.class).name().equals(name));
    }

    Class<?> classForValue(Class<?> bean) {
        return getAnnotatedClass(bean, XmlValue.class, a -> true);
    }

    <A extends Annotation> Class<?> getAnnotatedClass(Class<?> bean, Class<A> annotation, Predicate<AccessibleObject> filter) {
        Class<?> type = fieldType(bean, annotation, filter);
        if (type != null) {
            return type;
        }
        return methodType(bean, annotation, filter);
    }

    <A extends Annotation> Class<?> fieldType(Class<?> bean, Class<A> anno, Predicate<AccessibleObject> filter) {
        return Arrays.stream(bean.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(anno))
                .filter(filter)
                .map(f -> f.getType())
                .findFirst()
                .orElse(null);
    }

    <A extends Annotation> Class<?> methodType(Class<?> bean, Class<A> anno, Predicate<AccessibleObject> filter) {
        return Arrays.stream(bean.getDeclaredMethods())
                .filter(m -> !m.getReturnType().equals(Void.class))
                .filter(m -> m.isAnnotationPresent(anno))
                .filter(filter)
                .map(m -> m.getReturnType())
                .findFirst()
                .orElse(null);
    }

    @XmlRootElement()
    @XmlType(propOrder = { "aa1", "aa2", "loop" })
    public static class TestTx {
        @XmlElement(name = "AA1")
        Aa1 aa1;
        @XmlElement(name = "AA2")
        Aa2 aa2;
        @XmlElement(name = "L9000")
        Loop9000 loop;
    }

    @XmlType(propOrder = { "aa1", "aa2" })
    public static class Loop9000 {
        @XmlElement(name = "AA1")
        Aa1 aa1;
        @XmlElement(name = "AA2")
        Aa2 aa2;
    }

    @XmlType(propOrder = { "aa101", "aa102", "aa103" })
    public static class Aa1 {
        @XmlElement(name = "AA101")
        private String aa101;
        @XmlElement(name = "AA102")
        private Integer aa102;
        @XmlElement(name = "AA103")
        @XmlSchemaType(name = "date")
        private Date aa103;
    }

    @XmlType(propOrder = { "aa201", "aa202", "aa203" })
    public static class Aa2 {
        @XmlElement(name = "AA201")
        private String aa201;
        @XmlElement(name = "AA202")
        private List<Integer> aa202;
        @XmlElement(name = "AA203")
        private Comp1 aa203;
    }

    @XmlType(name = "COMP1", propOrder = { "comp11", "comp12", "comp13" })
    public static class Comp1 {
        @XmlElement(name = "COMP1-1")
        private String comp11;
        @XmlElement(name = "COMP1-2")
        private Integer comp12;
        @XmlElement(name = "COMP1-3")
        private NumberType comp13;
    }

    @XmlType(name = "E600")
    public static class NumberType {
        @XmlValue
        BigDecimal value;
    }
}
