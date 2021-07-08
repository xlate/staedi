package io.xlate.edi.schema;

/**
 * Provides access to attributes specified for a loop complex type.
 *
 * @since 1.18
 */
public interface EDILoopType extends EDIComplexType {

    /**
     * For hierarchical loops, get the position of the element within the first
     * segment that identifies the loop. If not a hierarchical loop, null is
     * returned.
     *
     * @return the position of the element holding the loop's level ID
     */
    EDIElementPosition getLevelIdPosition();

    /**
     * For hierarchical loops, get the position of the element within the first
     * segment that identifies the parent of the loop. If not a hierarchical
     * loop, null is returned.
     *
     * @return the position of the element holding the loop's parent level ID
     */
    EDIElementPosition getParentIdPosition();

}
