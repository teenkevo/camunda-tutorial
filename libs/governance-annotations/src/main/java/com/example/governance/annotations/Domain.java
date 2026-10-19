package com.example.governance.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the business data domain of a JPA entity. The value must match a
 * governance naming allow-list id (e.g. {@code refunds}, {@code taxpayer-registration})
 * and is used for table-name convention checks.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Domain {

    /** Domain id, e.g. {@code refunds}. */
    String value();
}
