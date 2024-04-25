package io.xlate.edi.internal.stream.validation;

public interface ValidatorConfig {

    boolean validateControlCodeValues();

    boolean formatElements();

    boolean trimDiscriminatorValues();

}
