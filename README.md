# StAEDI - Streaming API for EDI
[![Build Status](https://travis-ci.org/xlate/staedi.svg?branch=master)](https://travis-ci.org/xlate/staedi) [![Coverage Status](https://coveralls.io/repos/github/xlate/staedi/badge.svg?branch=master)](https://coveralls.io/github/xlate/staedi?branch=master) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=xlate_staedi&metric=alert_status)](https://sonarcloud.io/dashboard?id=xlate_staedi)

## Overview

StAEDI is a streaming API for EDI reading and writing for Java based on the StAX (Streaming API for XML)
available in the standard JDK. The API follows the same conventions as StAX using a "pull" processing flow
for reading and an emit flow for writing. StAEDI also supports filters to allow client applications to
process only certain events in the input stream such as segment begin events.

StAEDI is licensed under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)

## Maven Coordinates

```xml
<dependency>
  <groupId>io.xlate</groupId>
  <artifactId>staedi</artifactId>
  <version>1.0.0</version>
</dependency>
```

If you wish to use a SNAPSHOT version, add the Sonatype OSS repository to your Maven configuration.

```xml
<repositories>
  <repository>
    <id>oss-sonatype</id>
    <name>oss-sonatype</name>
    <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
    <snapshots>
      <enabled>true</enabled>
    </snapshots>
  </repository>
</repositories>
```

## Reading EDI

Input data is provided using a series of events via the `EDIStreamReader` class.
In addition to events such as the start of a segment or element, the looping/nested structure of the
EDI stream is represented using derived events.

```
+ Start Interchange
| +-- Start Segment (ISA/UNB)
| |     Element Data (repeats)
| +-- End Segment (ISA / UNB)
| |
| +-- Start Functional Group (Optional for EDIFACT)
| |   +-- Start Segment (GS / UNG)
| |   |     Element Data (repeats)
| |   +-- End Segment (GS / UNG)
| |
| |   +-- Start Transaction/Message
| |   |  +-- Start Segment (ST / UNH)
| |   |  |     Element Data (repeats)
| |   |  +-- End Segment (ST / UNH)
| |   |
| |   |  // Segments / Loops specific to the transaction
| |   |
| |   |  +-- Start Segment (SE / UNT)
| |   |  |     Element Data (repeats)
| |   |  +-- End Segment (SE / UNT)
| |   +-- End Transaction/Message
| |
| |   +-- Start Segment (GE / UNE)
| |   |     Element Data (repeats)
| |   +-- End Segment (GE / UNE)
| +-- End Functional Group
| |
| +-- Start Transaction/Message (EDIFACT only, if functional group(s) not used)
| |   // Same content as messages within group
| +-- End Transaction/Message
| |
| +-- Start Segment (IEA / UNZ)
| |     Element Data (repeats)
| +-- End Segment (IEA / UNZ)
+ End Interchange
```

```java
EDIInputFactory factory = EDIInputFactory.newFactory();

// Obtain Stream to the EDI document to read.
InputStream stream = new FileInputStream(...);

EDIStreamReader reader = factory.createEDIStreamReader(stream);

while (reader.hasNext()) {
	switch (reader.next()) {
	case Events.START_INTERCHANGE:
		/* Retrieve the standard - "X12" or "EDIFACT" */
		String standard = reader.getStandard();
		/*-
		 * Retrieve the version string array. An array is used to support
		 * the componentized version element used in the EDIFACT standard.
		 *
		 * e.g. [ "00501" ] (X12) or [ "UNOA", "3" ] (EDIFACT)
		 */
		String[] version = reader.getVersion();
		break;

	case Events.START_SEGMENT:
		// Retrieve the segment name - e.g. "ISA" (X12) or "UNB" (EDIFACT)
		String segmentName = reader.getText();
		break;

	case Events.END_SEGMENT:
		break;

	case Events.START_COMPOSITE:
		break;

	case Events.END_COMPOSITE:
		break;

	case Events.ELEMENT_DATA:
		// Retrieve the value of the current element
		String data = reader.getText();
		break;
	}
}

reader.close();
stream.close();

```

## Sample Writing X12 EDI

```java
EDIOutputFactory factory = EDIOutputFactory.newFactory();

// Obtain Stream write the EDI document.
OutputStream stream = new FileOutputStream(...);

EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
int groupCount = 0;

writer.startInterchange();

// Write interchange header segment
writer.writeStartSegment("ISA");
writer.writeElement("00").writeElement("          ");
writer.writeElement("00").writeElement("          ");
writer.writeElement("ZZ").writeElement("ReceiverID     ");
writer.writeElement("ZZ").writeElement("Sender         ");
writer.writeElement("050812");
writer.writeElement("1953");
writer.writeElement("^");
writer.writeElement("00501");
writer.writeElement("508121953");
writer.writeElement("0");
writer.writeElement("P");
writer.writeElement(":");
writer.writeEndSegment();

// Write functional group header segment
groupCount++;
writer.writeStartSegment("GS");
writer.writeStartElement();

...

writer.endInterchange();

writer.close();
stream.close();

```
