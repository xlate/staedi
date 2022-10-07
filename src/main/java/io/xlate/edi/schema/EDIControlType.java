package io.xlate.edi.schema;

/**
 * Provides access to attributes specified for a control structure complex type
 * such as the interchange, group, and transaction envelope structures.
 *
 * @since 1.21
 */
public interface EDIControlType extends EDIComplexType {

    public enum Type {
        /**
         * Nothing will be counted or validated
         */
        NONE,

        /**
         * Indicates that the element identified by {@link EDIControlType#getTrailerCountPosition()} will
         * contain a count of the control structures internal to the {@link EDIControlType}.
         */
        CONTROLS,

        /**
         * Indicates that the element identified by {@link EDIControlType#getTrailerCountPosition()} will
         * contain a count of the segments within the {@link EDIControlType}, <em>including</em> the header
         * and trailer segments.
         */
        SEGMENTS;

        public static Type fromString(String value) {
            for (Type entry : values()) {
                if (entry.name().equalsIgnoreCase(value)) {
                    return entry;
                }
            }
            throw new IllegalArgumentException("No enum constant for " + Type.class.getName() + "." + value);
        }
    }

    /**
     * Get the position of the element within the header segment that is used as
     * the control reference/number for the envelope.
     *
     * @return the position of the element holding the structure's header
     *         control reference
     */
    EDIElementPosition getHeaderRefPosition();

    /**
     * Get the position of the element within the trailer segment that is used as
     * the control reference/number for the envelope.
     *
     * @return the position of the element holding the structure's trailer
     *         control reference
     */
    EDIElementPosition getTrailerRefPosition();

    /**
     * Get the position of the element within the trailer segment that is used as
     * the control count for the envelope. The actual structures counted are determined
     * by the value returned by {@link #getCountType()}.
     *
     * @return the position of the element holding the structure's trailer
     *         control count
     */
    EDIElementPosition getTrailerCountPosition();

    /**
     * Get the type of structures
     *
     * @return the type of count
     */
    Type getCountType();

}
