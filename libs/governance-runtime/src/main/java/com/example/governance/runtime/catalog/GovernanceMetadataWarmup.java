package com.example.governance.runtime.catalog;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * After catalog publish, warm the local registry cache for declared {@code @GovernedAsset} ids.
 */
@Component
@Order(200)
@ConditionalOnProperty(prefix = "app.governance.catalog", name = "publish-on-startup", havingValue = "true", matchIfMissing = true)
public class GovernanceMetadataWarmup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GovernanceMetadataWarmup.class);

    private final GovernanceCatalogProperties properties;
    private final GovernanceMetadataScanner scanner;
    private final GovernanceMetadataService metadataService;
    private final Environment environment;

    public GovernanceMetadataWarmup(
            GovernanceCatalogProperties properties,
            GovernanceMetadataScanner scanner,
            GovernanceMetadataService metadataService,
            Environment environment
    ) {
        this.properties = properties;
        this.scanner = scanner;
        this.metadataService = metadataService;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<String> packages = scanPackages();
        List<String> assetIds = scanner.scanAssetIds(packages);
        if (assetIds.isEmpty()) {
            return;
        }
        metadataService.warm(assetIds);
        log.info("Warmed governance metadata cache for {} asset(s) in {}", assetIds.size(), properties.getServiceName());
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
