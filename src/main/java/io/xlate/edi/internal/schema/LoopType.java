package io.xlate.edi.internal.schema;

import java.util.List;

import io.xlate.edi.schema.EDIElementPosition;
import io.xlate.edi.schema.EDILoopType;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.schema.EDIType;

@SuppressWarnings("java:S2160") // Intentionally inherit 'equals' from superclass
class LoopType extends StructureType implements EDILoopType {

    private final EDIElementPosition levelIdPosition;
    private final EDIElementPosition parentIdPosition;

    LoopType(String code,
            List<EDIReference> references,
            List<EDISyntaxRule> syntaxRules,
            EDIElementPosition levelIdPosition,
            EDIElementPosition parentIdPosition,
            String title,
            String description) {

        super(code, EDIType.Type.LOOP, code, references, syntaxRules, title, description);
        this.levelIdPosition = levelIdPosition;
        this.parentIdPosition = parentIdPosition;
    }

    @Override
    public EDIElementPosition getLevelIdPosition() {
        return levelIdPosition;
    }

    @Override
    public EDIElementPosition getParentIdPosition() {
        return parentIdPosition;
    }

}
