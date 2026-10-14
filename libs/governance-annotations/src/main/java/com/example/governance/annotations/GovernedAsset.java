package com.example.governance.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that this JPA entity corresponds to a governed asset whose
 * ownership, stewardship, classification, and retention live in the external
 * governance registry (OpenMetadata). The value is a stable asset id, e.g.
 * {@code "refunds.application"}. Domain for naming policy is the segment
 * before the first dot ({@code refunds}).
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GovernedAsset {

    /** Logical asset id in the external registry, e.g. {@code refunds.application}. */
    String value();
}
