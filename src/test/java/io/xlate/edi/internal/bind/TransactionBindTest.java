package io.xlate.edi.internal.bind;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
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

import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.SchemaOutputResolver;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.xlate.edi.stream.EDINamespaces;

class TransactionBindTest {

    static final String XSD = "http://www.w3.org/2001/XMLSchema";
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

        complexTypes = results.values()
            .stream()
            .map(DOMResult::getNode)
            .map(Document.class::cast)
            .map(document -> document.getElementsByTagNameNS(XSD, "complexType"))
            .flatMap(this::elementStream)
            .collect(Collectors.toMap(e -> e.getAttribute("name"), e -> e));

        simpleTypes = results.values()
                .stream()
                .map(DOMResult::getNode)
                .map(Document.class::cast)
                .map(document -> document.getElementsByTagNameNS(XSD, "simpleType"))
                .flatMap(this::elementStream)
                .collect(Collectors.toMap(e -> e.getAttribute("name"), e -> e));
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
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) &&
                        Character.isUpperCase(name.charAt(0))){
            return name;
        }
        char[] chars = name.toCharArray();
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
                System.out.printf("%s%s: %s%n", pad, node.getAttribute("base"), clazz);
            } else {
                String name = node.getAttribute("name");
                clazz = classForElement(stack.peekFirst(), name);
                System.out.printf("%s%s: %s (%s)%n", pad, name, isStructure ? "***" : typeName, clazz);
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
                .map(Field::getType)
                .findFirst()
                .orElse(null);
    }

    <A extends Annotation> Class<?> methodType(Class<?> bean, Class<A> anno, Predicate<AccessibleObject> filter) {
        return Arrays.stream(bean.getDeclaredMethods())
                .filter(m -> !m.getReturnType().equals(Void.class))
                .filter(m -> m.isAnnotationPresent(anno))
                .filter(filter)
                .map(Method::getReturnType)
                .findFirst()
                .orElse(null);
    }

    @XmlRootElement(namespace = EDINamespaces.LOOPS)
    @XmlType(propOrder = { "aa1", "aa2", "loop" }, namespace = EDINamespaces.LOOPS)
    public static class TestTx {
        @XmlElement(name = "AA1", namespace = EDINamespaces.SEGMENTS)
        Aa1 aa1;
        @XmlElement(name = "AA2", namespace = EDINamespaces.SEGMENTS)
        Aa2 aa2;
        @XmlElement(name = "L9000", namespace = EDINamespaces.LOOPS)
        Loop9000 loop;
    }

    @XmlType(namespace = EDINamespaces.LOOPS, propOrder = { "aa1", "aa2" })
    public static class Loop9000 {
        @XmlElement(name = "AA1", namespace = EDINamespaces.SEGMENTS)
        Aa1 aa1;
        @XmlElement(name = "AA2", namespace = EDINamespaces.SEGMENTS)
        Aa2 aa2;
    }

    @XmlType(namespace = EDINamespaces.SEGMENTS, propOrder = { "aa101", "aa102", "aa103" })
    public static class Aa1 {
        @XmlElement(name = "AA101", namespace = EDINamespaces.ELEMENTS)
        private String aa101;
        @XmlElement(name = "AA102", namespace = EDINamespaces.ELEMENTS)
        private Integer aa102;
        @XmlElement(name = "AA103", namespace = EDINamespaces.ELEMENTS)
        @XmlSchemaType(name = "date")
        private Date aa103;
    }

    @XmlType(namespace = EDINamespaces.SEGMENTS, propOrder = { "aa201", "aa202", "aa203" })
    public static class Aa2 {
        @XmlElement(name = "AA201", namespace = EDINamespaces.ELEMENTS)
        private String aa201;
        @XmlElement(name = "AA202", namespace = EDINamespaces.ELEMENTS)
        private List<Integer> aa202;
        @XmlElement(name = "AA203", namespace = EDINamespaces.COMPOSITES)
        private Comp1 aa203;
    }

    @XmlType(namespace = EDINamespaces.COMPOSITES, name = "COMP1", propOrder = { "comp11", "comp12", "comp13" })
    public static class Comp1 {
        @XmlElement(name = "COMP1-1", namespace = EDINamespaces.ELEMENTS)
        private String comp11;
        @XmlElement(name = "COMP1-2", namespace = EDINamespaces.ELEMENTS)
        private Integer comp12;
        @XmlElement(name = "COMP1-3", namespace = EDINamespaces.ELEMENTS)
        private NumberType comp13;
    }

    @XmlType(name = "E600", namespace = EDINamespaces.ELEMENTS)
    public static class NumberType {
        @XmlValue
        BigDecimal value;
    }
}
