package io.xlate.edi.internal.stream.validation;

import java.util.List;

import io.xlate.edi.schema.EDIControlType;
import io.xlate.edi.schema.EDIElementPosition;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.Location;

class ControlUsageNode extends UsageNode {

    String referenceValue;
    EDIControlType type;

    ControlUsageNode(UsageNode parent, int depth, EDIReference link, int siblingIndex) {
        super(parent, depth, link, siblingIndex);
        type = (EDIControlType) link.getReferencedType();
    }

    @Override
    void reset() {
        super.reset();
        this.referenceValue = null;
    }

    @Override
    void incrementUsage() {
        super.incrementUsage();
        this.referenceValue = null;
    }

    boolean matchesLocation(int segmentRef, EDIElementPosition position, Location location) {
        return position != null
                && position.matchesLocation(location)
                && type.getReferences().get(segmentRef).getReferencedType().getId().equals(location.getSegmentTag());
    }

    @Override
    void validateReference(Location location, CharSequence value, List<EDIStreamValidationError> errors) {
        if (referenceValue == null) {
            if (matchesLocation(0, type.getHeaderRefPosition(), location)) {
                this.referenceValue = value.toString();
            }
            return;
        }

        if (matchesLocation(type.getReferences().size() - 1, type.getTrailerRefPosition(), location)
                && !referenceValue.contentEquals(value)) {
            errors.add(EDIStreamValidationError.CONTROL_REFERENCE_MISMATCH);
        }
    }

}
