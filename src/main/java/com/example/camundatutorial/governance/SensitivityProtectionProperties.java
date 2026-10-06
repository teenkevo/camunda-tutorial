package com.example.camundatutorial.governance;

import java.util.EnumSet;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application-wide sensitivity protection settings.
 *
 * <pre>
 * app:
 *   sensitivity:
 *     protection:
 *       enabled: true
 *       response-mode: MASK
 *       mask-value: "********"
 *       protected-types:
 *         - PII
 *       block-entity-responses: true
 * </pre>
 */
@Getter
@ConfigurationProperties(prefix = "app.sensitivity.protection")
public class SensitivityProtectionProperties {

    /** Master switch for Jackson, mapping, logging, and entity-response guards. */
    @Setter
    private boolean enabled = true;

    /** MASK or OMIT for outbound protection. */
    @Setter
    private SensitivityProtectionMode responseMode = SensitivityProtectionMode.MASK;

    /** Replacement used when {@link #responseMode} is {@link SensitivityProtectionMode#MASK}. */
    @Setter
    private String maskValue = "********";

    /**
     * Which {@link SensitiveType} values are actively protected.
     * Default is PII only; add SENSITIVE / INTERNAL to widen the threshold.
     */
    private Set<SensitiveType> protectedTypes = EnumSet.of(SensitiveType.PII);

    /** Reject REST responses that return JPA {@code @Entity} types directly. */
    @Setter
    private boolean blockEntityResponses = true;

    public void setProtectedTypes(Set<SensitiveType> protectedTypes) {
        this.protectedTypes = protectedTypes == null || protectedTypes.isEmpty()
                ? EnumSet.noneOf(SensitiveType.class)
                : EnumSet.copyOf(protectedTypes);
    }

    public boolean protects(SensitiveType type) {
        return enabled && type != null && protectedTypes.contains(type);
    }
}
