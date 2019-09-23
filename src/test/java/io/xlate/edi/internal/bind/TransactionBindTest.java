package io.xlate.edi.internal.bind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TransactionBindTest {

    @Test
    public void testElementBinding() throws Exception {
        JAXBContext context = JAXBContext.newInstance(TestTx.class);
        final List<DOMResult> results = new ArrayList<DOMResult>();
        context.generateSchema(new SchemaOutputResolver() {
            @Override
            public Result createOutput(String ns, String file) throws IOException {
                DOMResult result = new DOMResult();
                result.setSystemId(file);
                results.add(result);
                return result;
            }
         });
        assertEquals(1, results.size());
        Document doc = (Document) results.get(0).getNode();
        NodeList children = doc.getChildNodes().item(0).getChildNodes();
        for (int i = 0, m = children.getLength(); i < m; i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                NodeList c = ((Element) node).getChildNodes();
                for (int j = 0, n = c.getLength(); j < n; j++) {
                    Node x = c.item(j);
                    x.toString();
                }
            }
            node.toString();
        }

        StringWriter writer = new StringWriter();
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            //String xml = writer.toString();
            //System.out.println(xml);
        } catch (Exception e) {
        	fail("Unexpected exception: " + e.getMessage());
        }
    }

    @XmlRootElement(name = "transaction")
    @XmlType(name = "transactionType")
    public static class TestTx {
        @XmlElement(name = "AA1")
        Aa1 aa1;
        @XmlElement(name = "AA2")
        Aa2 aa2;
        @XmlElement(name = "L9000")
        Loop loop;
    }

    public static class Loop {
        @XmlElement(name = "AA1")
        Aa1 aa1;
        @XmlElement(name = "AA2")
        Aa2 aa2;
    }

    public static class Aa1 {
        @XmlElement(name = "AA101")
        private String aa101;
        @XmlElement(name = "AA102")
        private Integer aa102;
    }

    public static class Aa2 {
        @XmlElement(name = "AA201")
        private String aa201;
        @XmlElement(name = "AA202")
        private List<Integer> aa202;
        @XmlElement(name = "AA203") @XmlSchemaType(name = "COMP1")
        private Comp1 aa203;
    }

    @XmlType(name = "COMP1")
    public static class Comp1 {
        @XmlElement(name = "COMP1-1")
        private String comp11;
        @XmlElement(name = "COMP1-2")
        private Integer comp12;
    }
}
