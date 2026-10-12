package com.example.governance.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Entity-level data governance declaration. Prefer this over separate
 * {@link DataOwner} / {@link DataDomain} on types. Enforced by the governance catalog.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataGovernance {

    /** Business owner (team / org unit), e.g. {@code Customer Operations}. */
    String owner();

    /** Data steward responsible for quality and definition. */
    String steward();

    /** Data domain id, must match governance naming allow-list (e.g. {@code refunds}). */
    String domain();

    /** Default / entity-level sensitivity classification. */
    SensitiveType classification();

    /** Retention policy code, e.g. {@code CUSTOMER_RETENTION}. */
    String retentionPolicy();
}
