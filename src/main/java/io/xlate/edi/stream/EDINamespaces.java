package io.xlate.edi.stream;

import java.util.Arrays;
import java.util.List;

public class EDINamespaces {
    private EDINamespaces() {
    }

    /**
     * XML namespace used for all loop types: INTERCHANGE, GROUP, TRANSACTIONS,
     * and internal message loops.
     */
    public static final String LOOPS = "urn:xlate.io:staedi:names:loops";

    /**
     * XML namespace used for EDI segments
     */
    public static final String SEGMENTS = "urn:xlate.io:staedi:names:segments";

    /**
     * XML namespace used for EDI composite elements - those elements containing
     * sub-elements, i.e. components.
     */
    public static final String COMPOSITES = "urn:xlate.io:staedi:names:composites";

    /**
     * XML namespace used for EDI simple elements and the components
     * of a composite element,
     */
    public static final String ELEMENTS = "urn:xlate.io:staedi:names:elements";

    public static final List<String> all() {
        return Arrays.asList(LOOPS, SEGMENTS, COMPOSITES, ELEMENTS);
    }
}