package io.xlate.edi.internal.schema;

import java.util.List;

import io.xlate.edi.schema.EDIControlType;
import io.xlate.edi.schema.EDIElementPosition;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.schema.EDIType;

@SuppressWarnings("java:S2160") // Intentionally inherit 'equals' from superclass
class ControlType extends StructureType implements EDIControlType {

    private final EDIElementPosition headerRefPosition;
    private final EDIElementPosition trailerRefPosition;
    private final EDIElementPosition trailerCountPosition;
    private final EDIControlType.Type countType;

    @SuppressWarnings("java:S107")
    ControlType(String id,
            EDIType.Type type,
            String code,
            List<EDIReference> references,
            List<EDISyntaxRule> syntaxRules,
            EDIElementPosition headerRefPosition,
            EDIElementPosition trailerRefPosition,
            EDIElementPosition trailerCountPosition,
            EDIControlType.Type countType,
            String title,
            String description) {

        super(id, type, code, references, syntaxRules, title, description);
        this.headerRefPosition = headerRefPosition;
        this.trailerRefPosition = trailerRefPosition;
        this.trailerCountPosition = trailerCountPosition;
        this.countType = countType;
    }

    @Override
    public EDIElementPosition getHeaderReferencePosition() {
        return headerRefPosition;
    }

    @Override
    public EDIElementPosition getTrailerReferencePosition() {
        return trailerRefPosition;
    }

    @Override
    public EDIElementPosition getTrailerCountPosition() {
        return trailerCountPosition;
    }

    @Override
    public EDIControlType.Type getCountType() {
        return countType;
    }
}
