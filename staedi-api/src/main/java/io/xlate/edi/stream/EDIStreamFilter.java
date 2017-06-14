package io.xlate.edi.stream;

/**
 * This interface declares a simple filter interface that one can create to
 * filter EDIStreamReaders
 * 
 * @version 1.0
 */
public interface EDIStreamFilter {

	/**
	 * Tests whether the current state is part of this stream. This method will
	 * return true if this filter accepts this event and false otherwise.
	 *
	 * The method should not change the state of the reader when accepting a
	 * state.
	 *
	 * @param reader
	 *            the event to test
	 * @return true if this filter accepts this event, false otherwise
	 */
	boolean accept(EDIStreamReader reader);

}
