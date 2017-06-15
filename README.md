# StAEDI - Streaming API for EDI [![Build Status](https://travis-ci.org/xlate/staedi.svg?branch=master)](https://travis-ci.org/xlate/staedi)

## Overview

StAEDI is a streaming API for EDI reading and writing for Java based on the StAX (Streaming API for XML)
available in the standard JDK. The API follows the same conventions as StAX using a "pull" processing flow
for reading and an emit flow for writing. StAEDI also supports filters to allow client applications to 
process only certain events in the input stream such as segment begin events.

StAEDI is licensed under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)

## Usage (using Maven)

```xml
<dependency>
	<groupId>io.xlate</groupId>
	<artifactId>staedi-impl</artifactId>
	<version>0.0.1</version>
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

## Sample Reading EDI

```java
EDIInputFactory factory = EDIInputFactory.newFactory();

// Obtain Stream to the EDI document to read.
InputStream stream = new FileInputStream(...);

EDIStreamReader reader = factory.createEDIStreamReader(stream);

while (reader.hasNext()) {
	switch (reader.next()) {
	case Events.START_INTERCHANGE:
		// Retrieve the standard - "X12" or "EDIFACT"
		String standard = reader.getStandard();
		// Retrieve the version - e.g. "00501" (X12) or "30000" (EDIFACT)
		String version = reader.getVersion();
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
