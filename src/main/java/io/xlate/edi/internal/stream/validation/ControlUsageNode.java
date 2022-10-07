package io.xlate.edi.internal.stream.validation;

import java.util.List;
import java.util.logging.Logger;

import io.xlate.edi.schema.EDIControlType;
import io.xlate.edi.schema.EDIElementPosition;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.Location;

class ControlUsageNode extends UsageNode {

    static final Logger LOGGER = Logger.getLogger(ControlUsageNode.class.getName());

    String referenceValue;
    EDIControlType type;
    int count;

    ControlUsageNode(UsageNode parent, int depth, EDIReference link, int siblingIndex) {
        super(parent, depth, link, siblingIndex);
        type = (EDIControlType) link.getReferencedType();
    }

    @Override
    void reset() {
        super.reset();
        this.referenceValue = null;
        this.count = 0;
    }

    @Override
    void incrementUsage() {
        super.incrementUsage();
        this.referenceValue = null;
        this.count = 0;
    }

    boolean matchesLocation(int segmentRef, EDIElementPosition position, Location location) {
        return position != null
                && position.matchesLocation(location)
                && type.getReferences().get(segmentRef).getReferencedType().getId().equals(location.getSegmentTag());
    }

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

    void validateCount(Location location, CharSequence value, List<EDIStreamValidationError> errors) {
        if (matchesLocation(type.getReferences().size() - 1, type.getTrailerCountPosition(), location)
                // Don't bother comparing the actual value if it's not formatted correctly
                && !errors.contains(EDIStreamValidationError.INVALID_CHARACTER_DATA)
                && !String.valueOf(count).contentEquals(value)) {
            errors.add(EDIStreamValidationError.CONTROL_COUNT_DOES_NOT_MATCH_ACTUAL_COUNT);
        }
    }

    int incrementCount(EDIControlType.Type countType) {
        if (this.type.getCountType() == countType) {
            count++;
            return count;
        }
        return 0;
    }
}
