package com.example.governance.runtime.catalog;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.governance.registry")
public class GovernanceRegistryProperties {

    /** When true, periodically refresh cached registry metadata. */
    private boolean pollEnabled = true;

    /** Poll interval in milliseconds. */
    private long pollMs = 60_000L;
}
