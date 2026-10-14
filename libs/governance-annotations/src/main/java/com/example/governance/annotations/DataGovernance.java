package com.example.governance.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @deprecated Prefer {@link GovernedAsset} for asset identity. Mutable attributes
 *     (owner, steward, classification, retention) belong in OpenMetadata, not code.
 */
@Deprecated(since = "hybrid-governance", forRemoval = false)
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
