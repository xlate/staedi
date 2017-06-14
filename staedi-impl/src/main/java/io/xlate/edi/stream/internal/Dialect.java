package io.xlate.edi.stream.internal;



public interface Dialect {

	String getStandard();

	String getVersion();

	void setHeaderTag(String tag);

	String getHeaderTag();

	boolean isConfirmed();

	boolean isRejected();

	boolean appendHeader(CharacterSet characters, char value);

	char getSegmentTerminator();
	char getDataElementSeparator();
	char getComponentElementSeparator();
	char getRepetitionSeparator();
	char getReleaseIndicator();
	char getDecimalMark();
}
