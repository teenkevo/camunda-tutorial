package com.example.governance.runtime.catalog;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * On startup, publishes local entity governance metadata to the governance microservice
 * and optionally verifies conformance.
 */
@Component
@ConditionalOnProperty(prefix = "app.governance.catalog", name = "publish-on-startup", havingValue = "true", matchIfMissing = true)
public class GovernanceMetadataPublisher implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GovernanceMetadataPublisher.class);

    private final GovernanceCatalogProperties properties;
    private final GovernanceMetadataScanner scanner;
    private final GovernanceRequestValidationScanner validationScanner;
    private final GovernanceCatalogClient client;
    private final Environment environment;

    public GovernanceMetadataPublisher(
            GovernanceCatalogProperties properties,
            GovernanceMetadataScanner scanner,
            GovernanceRequestValidationScanner validationScanner,
            GovernanceCatalogClient client,
            Environment environment
    ) {
        this.properties = properties;
        this.scanner = scanner;
        this.validationScanner = validationScanner;
        this.client = client;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String serviceName = properties.getServiceName();
        Set<String> packages = scanPackages();
        List<GovernanceAssetDto> assets = scanner.scan(serviceName, packages);
        List<GovernanceValidationDto> validations = validationScanner.scan(serviceName, packages);
        if (assets.isEmpty() && validations.isEmpty()) {
            log.info("No governance assets or request validations found in {}", packages);
            return;
        }
        log.info(
                "Publishing {} assets and {} request validations for {}",
                assets.size(),
                validations.size(),
                serviceName
        );

        client.syncSafe(new GovernanceSyncRequest(serviceName, assets, validations));

        try {
            ConformanceReport report = client.conformance(serviceName);
            if (report.compliant()) {
                log.info("Governance conformance OK for {}", serviceName);
            } else {
                log.warn("Governance conformance FAILED for {}: {}", serviceName, report.issues());
                if (properties.isFailOnNonCompliant()) {
                    throw new IllegalStateException(
                            "Governance conformance failed for " + serviceName + ": " + report.issues()
                    );
                }
            }
        } catch (RuntimeException ex) {
            if (ex instanceof IllegalStateException && properties.isFailOnNonCompliant()) {
                throw ex;
            }
            if (properties.isFailOnError()) {
                throw ex;
            }
            log.warn("Governance conformance check skipped/failed: {}", ex.getMessage());
        }
    }

    private Set<String> scanPackages() {
        Set<String> packages = new LinkedHashSet<>();
        String configured = environment.getProperty("app.governance.catalog.scan-packages");
        if (configured != null && !configured.isBlank()) {
            for (String p : configured.split(",")) {
                if (!p.isBlank()) {
                    packages.add(p.trim());
                }
            }
            return packages;
        }
        packages.add("com.example");
        return packages;
    }
}
