package com.example.refund.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ensures every tax type is one of VAT, PAYE, CIT, WHT (case-insensitive).
 * Pair with {@code @NotEmpty} when at least one type is required.
 */
@Documented
@Constraint(validatedBy = SupportedTaxTypesValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SupportedTaxTypes {

    String message() default "taxTypes must be a subset of VAT, PAYE, CIT, WHT";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
