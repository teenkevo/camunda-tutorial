package com.example.refund.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SupportedTaxTypesValidatorTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void acceptsSupportedTypesCaseInsensitive() {
        Sample sample = new Sample(List.of("vat", "PAYE"));
        assertThat(validator.validate(sample)).isEmpty();
    }

    @Test
    void rejectsUnsupportedType() {
        Sample sample = new Sample(List.of("VAT", "GST"));
        Set<ConstraintViolation<Sample>> violations = validator.validate(sample);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("taxTypes"));
    }

    static class Sample {
        @SupportedTaxTypes
        private final List<String> taxTypes;

        Sample(List<String> taxTypes) {
            this.taxTypes = taxTypes;
        }
    }
}
