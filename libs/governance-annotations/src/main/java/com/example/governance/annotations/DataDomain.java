package com.example.governance.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the business data domain of a field (or legacy entity). Prefer {@link DataGovernance} on types.
 */
@Documented
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataDomain {

    /** Domain name, e.g. {@code refunds}. */
    String value();
}
