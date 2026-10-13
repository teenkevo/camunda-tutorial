package com.example.governance.runtime.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * TTL-style fallback: periodically refresh cached asset metadata when webhooks are missed.
 */
@Component
@ConditionalOnProperty(prefix = "app.governance.registry", name = "poll-enabled", havingValue = "true", matchIfMissing = true)
public class GovernanceMetadataPoller {

    private static final Logger log = LoggerFactory.getLogger(GovernanceMetadataPoller.class);

    private final GovernanceMetadataService metadataService;

    public GovernanceMetadataPoller(GovernanceMetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @Scheduled(fixedDelayString = "${app.governance.registry.poll-ms:60000}")
    public void poll() {
        var keys = metadataService.snapshot().keySet();
        if (keys.isEmpty()) {
            return;
        }
        log.debug("Polling governance registry for {} cached asset(s)", keys.size());
        keys.forEach(metadataService::refresh);
    }
}
