package com.example.camundatutorial.governance;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as carrying classified data. Retained at runtime so enforcement
 * (logging redaction, API masking, Camunda guards) can inspect it.
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Sensitive {

    /**
     * Classification of this property. Type-level (whole-class) sensitivity is not supported;
     * only fields/getters may be marked.
     */
    SensitiveType value();
}
