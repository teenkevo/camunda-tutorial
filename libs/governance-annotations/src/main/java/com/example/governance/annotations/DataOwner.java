package com.example.governance.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the owner of a field (or legacy entity). Prefer {@link DataGovernance} on entity types.
 */
@Documented
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataOwner {

    /** Owning team or role, e.g. {@code tax-ops}. */
    String value();
}
