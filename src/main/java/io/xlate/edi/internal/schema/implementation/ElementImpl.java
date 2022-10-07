package io.xlate.edi.internal.schema.implementation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.schema.implementation.ElementImplementation;

public class ElementImpl extends BaseImpl<EDISimpleType> implements ElementImplementation, Positioned {

    private static final String TOSTRING_FORMAT = "typeId: %s, minOccurs: %d, maxOccurs: %d, position: %d, values: %s, standard: { %s }";
    private final int position;
    private final Map<String, String> values;

    public ElementImpl(int minOccurs,
            int maxOccurs,
            String typeId,
            int position,
            Map<String, String> values,
            String title,
            String description) {
        super(title, description);
        super.minOccurs = minOccurs;
        super.maxOccurs = maxOccurs;
        super.typeId = typeId;
        this.position = position;
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public ElementImpl(EDIReference standardReference, int position) {
        super(null, null);
        this.setStandardReference(standardReference);
        this.typeId = standard.getId();
        this.position = position;
        this.values = standard.getValues();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) &&
                Objects.equals(position, ((ElementImpl) o).position) &&
                Objects.equals(values, ((ElementImpl) o).values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), position, values);
    }

    @Override
    public String toString() {
        return String.format(TOSTRING_FORMAT, typeId, minOccurs, maxOccurs, position, values, standard);
    }

    @Override
    public Map<String, String> getValues() {
        return values;
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public Base getBase() {
        return standard.getBase();
    }

    /**
     * @see io.xlate.edi.schema.EDISimpleType#getNumber()
     * @deprecated
     */
    @SuppressWarnings({ "java:S1123", "java:S1133" })
    @Override
    @Deprecated
    public int getNumber() {
        return standard.getNumber();
    }

    @Override
    public long getMinLength() {
        return standard.getMinLength();
    }

    @Override
    public long getMaxLength() {
        return standard.getMaxLength();
    }
}
