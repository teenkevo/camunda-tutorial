package com.example.governance.service.openmetadata;

import com.example.governance.service.config.OpenMetadataProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Ensures glossary + demo governed assets exist in OpenMetadata after startup.
 */
@Component
@Order(50)
@ConditionalOnProperty(prefix = "app.governance.openmetadata", name = "seed", havingValue = "true", matchIfMissing = true)
public class OpenMetadataSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OpenMetadataSeedRunner.class);

    private final OpenMetadataClient client;
    private final OpenMetadataProperties properties;

    public OpenMetadataSeedRunner(OpenMetadataClient client, OpenMetadataProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        if (!client.isReachable()) {
            log.warn("OpenMetadata not reachable at {} — seed skipped (start docker compose OM stack)",
                    properties.getBaseUrl());
            return;
        }
        try {
            client.ensureGlossary();
            for (OpenMetadataProperties.SeedAsset asset : properties.getSeedAssets()) {
                client.upsertAsset(asset);
            }
            log.info("OpenMetadata governed assets seeded ({} term(s))", properties.getSeedAssets().size());
        } catch (RuntimeException ex) {
            log.warn("OpenMetadata seed failed: {}", ex.getMessage());
        }
    }
}
