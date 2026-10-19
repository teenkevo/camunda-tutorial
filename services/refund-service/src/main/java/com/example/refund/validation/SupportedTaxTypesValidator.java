package com.example.refund.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;

public class SupportedTaxTypesValidator implements ConstraintValidator<SupportedTaxTypes, Collection<String>> {

    public static final Set<String> SUPPORTED = Set.of("VAT", "PAYE", "CIT", "WHT");

    @Override
    public boolean isValid(Collection<String> value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            // emptiness is owned by @NotEmpty / @NotNull
            return true;
        }
        for (String raw : value) {
            if (raw == null || raw.isBlank()) {
                return false;
            }
            if (!SUPPORTED.contains(raw.trim().toUpperCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
    }
}
