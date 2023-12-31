# StAEDI - Streaming API for EDI
![Build](https://github.com/xlate/staedi/workflows/Build/badge.svg?branch=master) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=xlate_staedi&metric=coverage)](https://sonarcloud.io/summary/new_code?id=xlate_staedi) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=xlate_staedi&metric=alert_status)](https://sonarcloud.io/dashboard?id=xlate_staedi) [![Join the chat at https://gitter.im/xlate/staedi](https://badges.gitter.im/xlate/staedi.svg)](https://gitter.im/xlate/staedi?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![Maven Central](https://img.shields.io/maven-central/v/io.xlate/staedi)](https://search.maven.org/artifact/io.xlate/staedi) [![javadoc](https://javadoc.io/badge2/io.xlate/staedi/javadoc.svg)](https://javadoc.io/doc/io.xlate/staedi)

StAEDI is a streaming API for EDI reading, writing, and validation in Java. [[Support](https://github.com/xlate/staedi/wiki/Support) | [Wiki](https://github.com/xlate/staedi/wiki)]

![Event Flow](staedi_events.png)

The API follows the same conventions as StAX (XML API available in the standard JDK)
using a "pull" processing flow for EDI parsing and an emit flow for EDI generation.

## Features
- Support for **X12**, **EDIFACT**, and **TRADACOMS** standards
- Read structures from an EDI stream in sequence (start loop, start segment, element data, end segment, etc.)
- EDI Schema that allows for user-specified **validation** rules
- Validation of **EDI standards** (segment occurrences, element type, element length constraints, etc.)
- Validation of **industry implementations**, for example HIPAA
- Read and write EDI data using standard Java XML interfaces (StAX)
- Read EDI data using standard Java JSON interfaces (Jakarta JSON Processing, aka JSR-353/JSR-374)
- Support for X12 `ISX` segment (release character, element 01 only), introduced in version `007040`

## Maven Coordinates

```xml
<dependency>
  <groupId>io.xlate</groupId>
  <artifactId>staedi</artifactId>
  <version>CURRENT VERSION</version>
</dependency>
```

## Support
Support is available to assist with incorporating StAEDI into your business's application. Available services include
- **Development of EDI validation schemas** using your documentation (e.g. PDF)
- **Integrating StAEDI** into your Java application
- **Troubleshooting issues** with your existing integration (not including StAEDI bugs - please open an issue)

Please email ***contact at xlate dot io*** for more information.

## Have a Question?
If you have a question about StAEDI that may not require the opening of an issue, please head to the StAEDI Gitter channel at https://gitter.im/xlate/staedi to discuss.

## Reading EDI

Input data is provided using a series of events via the `EDIStreamReader` class.
In addition to events such as the start of a segment or element, the looping/nested structure of the
EDI stream is represented using derived events.

```
+ Start Interchange
| +-- Start Segment (ISA / UNB / STX)
| |     Element Data (repeats)
| +-- End Segment (ISA / UNB / STX)
| |
| +-- Start Functional Group (Optional for EDIFACT and TRADACOMS)
| |   +-- Start Segment (GS / UNG / BAT)
| |   |     Element Data (repeats)
| |   +-- End Segment (GS / UNG / BAT)
| |
| |   +-- Start Transaction/Message
| |   |  +-- Start Segment (ST / UNH / MHD)
| |   |  |     Element Data (repeats)
| |   |  +-- End Segment (ST / UNH / MHD)
| |   |
| |   |  // Segments / Loops specific to the transaction
| |   |
| |   |  +-- Start Segment (SE / UNT / MTR)
| |   |  |     Element Data (repeats)
| |   |  +-- End Segment (SE / UNT / MTR)
| |   +-- End Transaction/Message
| |
| |   +-- Start Segment (GE / UNE / EOB)
| |   |     Element Data (repeats)
| |   +-- End Segment (GE / UNE / EOB)
| +-- End Functional Group
| |
| +-- Start Transaction/Message (EDIFACT and TRADACOMS only, if functional group(s) not used)
| |   // Same content as messages within group
| +-- End Transaction/Message
| |
| +-- Start Segment (IEA / UNZ / END)
| |     Element Data (repeats)
| +-- End Segment (IEA / UNZ / END)
+ End Interchange
```

```java
EDIInputFactory factory = EDIInputFactory.newFactory();

// Obtain Stream to the EDI document to read.
InputStream stream = new FileInputStream(...);

EDIStreamReader reader = factory.createEDIStreamReader(stream);

while (reader.hasNext()) {
  switch (reader.next()) {
  case START_INTERCHANGE:
    /* Retrieve the standard - "X12", "EDIFACT", or "TRADACOMS" */
    String standard = reader.getStandard();

    /*
     * Retrieve the version string array. An array is used to support
     * the componentized version element used in the EDIFACT standard.
     *
     * e.g. [ "00501" ] (X12) or [ "UNOA", "3" ] (EDIFACT)
     */
    String[] version = reader.getVersion();
    break;

  case START_SEGMENT:
    // Retrieve the segment name - e.g. "ISA" (X12), "UNB" (EDIFACT), or "STX" (TRADACOMS)
    String segmentName = reader.getText();
    break;

  case END_SEGMENT:
    break;

  case START_COMPOSITE:
    break;

  case END_COMPOSITE:
    break;

  case ELEMENT_DATA:
    // Retrieve the value of the current element
    String data = reader.getText();
    break;
  }
}

reader.close();
stream.close();

```

### Disable Validation of Control Codes

Out of the box, instances of `EDIStreamReader` will validate the control structures
of X12 and EDIFACT messages (interchange, group, and transaction headers and trailers).
This validation includes checking that some fields contain one of an enumerated list
of values (e.g. a known transaction set code in X12 segment `ST`, element 1).

If you wish to disable the validation of the code values but keep the validation of the
structure, including field sizes and types, set the `EDIInputFactory.EDI_VALIDATE_CONTROL_CODE_VALUES`
property to `false` on an instance of `EDIInputFactory` prior to creating a new `EDIStreamReader`,
as shown below.

```java
// Create an EDIInputFactory
EDIInputFactory factory = EDIInputFactory.newFactory();
factory.setProperty(EDIInputFactory.EDI_VALIDATE_CONTROL_CODE_VALUES, false);

// Obtain an InputStream of the EDI document to read.
InputStream stream = new FileInputStream(...);

// Create an EDIStreamReader from the stream using the factory
EDIStreamReader reader = factory.createEDIStreamReader(stream);

// Continue processing with the reader...
```

## Sample Writing X12 EDI

The below example shows how X12 data could be written. TRADACOMS and EDIFACT standards are also supported,
using the segments specific to those standards.

```java
EDIOutputFactory factory = EDIOutputFactory.newFactory();

// Obtain Stream write the EDI document.
OutputStream stream = new FileOutputStream(...);

EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
int groupCount = 0;

// Set a schema for the control structures being written (interchange, group, and transaction envelope segments)
SchemaFactory schemaFactory = SchemaFactory.newFactory();
/*
 * A control schema can be created with the factory by providing the standard
 * and version array. The version is an array to support multi-field versions
 * such as the composite element UNB01 for EDIFACT.
 */
Schema controlSchema = schemaFactory.getControlSchema(EDIStreamConstants.Standards.X12, new String[] { "00501" });
writer.setControlSchema(controlSchema);

writer.startInterchange();

// Write interchange header segment
writer.writeStartSegment("ISA")
      .writeElement("00")
      .writeElement("          ")
      .writeElement("00")
      .writeElement("          ")
      .writeElement("ZZ")
      .writeElement("ReceiverID     ")
      .writeElement("ZZ")
      .writeElement("Sender         ")
      .writeElement("203001")
      .writeElement("1430")
      .writeElement("^")
      .writeElement("00501")
      .writeElement("000000001")
      .writeElement("0")
      .writeElement("P")
      .writeElement(":")
      .writeEndSegment();

// Write functional group header segment
groupCount++;
int txCount = 0;
writer.writeStartSegment("GS");
writer.writeElement("FA");

// Continue writing remainder of group header and transactions, increment `txCount` for each transaction

writer.writeStartSegment("GE")
      /* Count of transactions here must match the actual count of ST/SE pairs */
      .writeElement(String.valueOf(txCount))
      /* Control number here must match the value in the group header */
      .writeElement("1");

writer.writeStartSegment("IEA")
      /* Count of groups here must match the actual count of GS/GE pairs */
      .writeElement(String.valueOf(groupCount))
      /* Control number here must match the value in the interchange header */
      .writeElement("000000001")
      .writeEndSegment();

writer.endInterchange();

writer.close();
stream.close();
```
