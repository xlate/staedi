package io.xlate.edi.internal.bind;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.junit.jupiter.api.Test;

import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDINamespaces;
import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIStreamWriter;

class AcknowledgementBindTest {

    @Test
    void testEDIBinding() throws Exception {
        JAXBContext context = JAXBContext.newInstance(Interchange.class);
        Marshaller m = context.createMarshaller();
        EDIOutputFactory oFactory = EDIOutputFactory.newFactory();
        oFactory.setProperty(EDIOutputFactory.PRETTY_PRINT, Boolean.TRUE);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EDIStreamWriter writer = oFactory.createEDIStreamWriter(out);
        XMLStreamWriter xmlWriter = oFactory.createXMLStreamWriter(writer);

        ISA header = new ISA();
        //ISA*00*          *00*          *ZZ*Receiver       *ZZ*Sender         *200301*1430*^*00501*000000001*0*P*:~
        header.ISA01 = "00";
        header.ISA02 = "          ";
        header.ISA03 = "00";
        header.ISA04 = "          ";
        header.ISA05 = "ZZ";
        header.ISA06 = "Receiver       ";
        header.ISA07 = "ZZ";
        header.ISA08 = "Sender         ";
        header.ISA09 = "200301";
        header.ISA10 = "1430";
        header.ISA11 = "^";
        header.ISA12 = "00501";
        header.ISA13 = "000000001";
        header.ISA14 = "0";
        header.ISA15 = "P";
        header.ISA16 = ":";

        IEA trailer = new IEA();
        trailer.IEA01 = "1";
        trailer.IEA02 = "000000001";

        Interchange interchange = new Interchange();
        interchange.header = header;
        interchange.trailer = trailer;

        m.marshal(interchange, xmlWriter);

        EDIInputFactory iFactory = EDIInputFactory.newFactory();
        Interchange interchange2;
        try (EDIStreamReader reader = iFactory.createEDIStreamReader(new ByteArrayInputStream(out.toByteArray()))) {
            assertEquals(EDIStreamEvent.START_INTERCHANGE, reader.next());
            XMLStreamReader xmlReader = iFactory.createXMLStreamReader(reader);
            Unmarshaller u = context.createUnmarshaller();
            interchange2 = (Interchange) u.unmarshal(xmlReader);
        }

        assertEquals(interchange.header.ISA01, interchange2.header.ISA01);
        assertEquals(interchange.header.ISA02, interchange2.header.ISA02);
        assertEquals(interchange.header.ISA03, interchange2.header.ISA03);
        assertEquals(interchange.header.ISA04, interchange2.header.ISA04);
        assertEquals(interchange.header.ISA05, interchange2.header.ISA05);
        assertEquals(interchange.header.ISA06, interchange2.header.ISA06);
        assertEquals(interchange.header.ISA07, interchange2.header.ISA07);
        assertEquals(interchange.header.ISA08, interchange2.header.ISA08);
        assertEquals(interchange.header.ISA09, interchange2.header.ISA09);
        assertEquals(interchange.header.ISA10, interchange2.header.ISA10);
        assertEquals(interchange.header.ISA11, interchange2.header.ISA11);
        assertEquals(interchange.header.ISA12, interchange2.header.ISA12);
        assertEquals(interchange.header.ISA13, interchange2.header.ISA13);
        assertEquals(interchange.header.ISA14, interchange2.header.ISA14);
        assertEquals(interchange.header.ISA15, interchange2.header.ISA15);
        assertEquals(interchange.header.ISA16, interchange2.header.ISA16);

        assertEquals(interchange.trailer.IEA01, interchange2.trailer.IEA01);
        assertEquals(interchange.trailer.IEA02, interchange2.trailer.IEA02);

    }

    @XmlType
    @XmlRootElement(name = "INTERCHANGE", namespace = EDINamespaces.LOOPS)
    static class Interchange {
        @XmlElement(name = "ISA", namespace = EDINamespaces.SEGMENTS)
        ISA header;

        @XmlElement(name = "IEA", namespace = EDINamespaces.SEGMENTS)
        IEA trailer;
    }

    @XmlType(namespace = EDINamespaces.SEGMENTS, propOrder = {})
    static class ISA {
        @XmlElement(namespace = EDINamespaces.ELEMENTS)
        String ISA01;
        @XmlElement(namespace = EDINamespaces.ELEMENTS)
        String ISA02;
        @XmlElement(namespace = EDINamespaces.ELEMENTS)
        String ISA03;
        @XmlElement(namespace = EDINamespaces.ELEMENTS)
        String ISA04;
        @XmlElement(namespace = EDINamespaces.ELEMENTS)
        String ISA05;
        @XmlElement(namespace = EDINamespaces.ELEMENTS)
        String ISA06;
        @XmlElement(namespace = EDINamespaces.ELEMENTS)
        String ISA07;
        @XmlElement(namespace = EDINamespaces.ELEMENTS)
        String ISA08;
        @XmlElement(namespace = EDINamespaces.ELEMENTS)
        String ISA09;
        @XmlElement(namespace = EDINamespaces.ELEMENTS)
        String ISA10;
        @XmlElement(namespace = EDINamespaces.ELEMENTS)
        String ISA11;
        @XmlElement(namespace = EDINamespaces.ELEMENTS)
        String ISA12;
        @XmlElement(namespace = EDINamespaces.ELEMENTS)
        String ISA13;
        @XmlElement(namespace = EDINamespaces.ELEMENTS)
        String ISA14;
        @XmlElement(namespace = EDINamespaces.ELEMENTS)
        String ISA15;
        @XmlElement(namespace = EDINamespaces.ELEMENTS)
        String ISA16;
    }

    @XmlType(namespace = EDINamespaces.SEGMENTS, propOrder = {})
    static class IEA {
        @XmlElement(namespace = EDINamespaces.ELEMENTS)
        String IEA01;
        @XmlElement(namespace = EDINamespaces.ELEMENTS)
        String IEA02;
    }
}
