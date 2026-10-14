package com.example.governance.runtime;

import com.example.governance.annotations.SensitiveType;
import java.util.EnumSet;
import java.util.Set;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application-wide sensitivity protection settings.
 */
@Data
@ConfigurationProperties(prefix = "app.sensitivity.protection")
public class SensitivityProtectionProperties {

    private boolean enabled = true;
    private SensitivityProtectionMode responseMode = SensitivityProtectionMode.MASK;
    private String maskValue = "********";
    private Set<SensitiveType> protectedTypes = EnumSet.of(SensitiveType.PII);
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
