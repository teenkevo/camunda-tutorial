package com.example.governance.service.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.governance.openmetadata")
public class OpenMetadataProperties {

    /** When false, registry completeness checks are skipped and OM calls are best-effort. */
    private boolean enabled = true;

    /** Fail conformance when OM attributes are missing (when enabled). */
    private boolean requireRegistryAttributes = true;

    private String baseUrl = "http://localhost:8585/api";

    private String username = "admin";

    private String password = "admin";

    /** Glossary that holds governed asset terms. */
    private String glossaryName = "GovernedAssets";

    /** Seed glossary + demo terms on startup (off by default; manage assets in OpenMetadata UI). */
    private boolean seed = false;

    /**
     * serviceName → base URL used to POST /internal/governance/refresh after OM changes.
     */
    private Map<String, String> serviceRefreshUrls = defaultRefreshUrls();

    private List<SeedAsset> seedAssets = defaultSeedAssets();

    @Data
    public static class SeedAsset {
        private String assetId;
        private String owner;
        private String steward;
        private String classification;
        private String retentionPolicy;
        private String description;
    }

    private static Map<String, String> defaultRefreshUrls() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("refund-service", "http://localhost:8082");
        map.put("registration-service", "http://localhost:8083");
        map.put("document-service", "http://localhost:8085");
        return map;
    }

    private static List<SeedAsset> defaultSeedAssets() {
        List<SeedAsset> assets = new ArrayList<>();
        assets.add(seed(
                "refunds.application",
                "tax-ops",
                "Refund Data Management",
                "PII",
                "REFUND_RETENTION",
                "Refund application business record"
        ));
        assets.add(seed(
                "taxpayer-registration.taxpayer",
                "tax-ops",
                "Registration Data Management",
                "PII",
                "TAXPAYER_RETENTION",
                "Taxpayer registration business record"
        ));
        return assets;
    }

    private static SeedAsset seed(
            String assetId,
            String owner,
            String steward,
            String classification,
            String retentionPolicy,
            String description
    ) {
        SeedAsset asset = new SeedAsset();
        asset.setAssetId(assetId);
        asset.setOwner(owner);
        asset.setSteward(steward);
        asset.setClassification(classification);
        asset.setRetentionPolicy(retentionPolicy);
        asset.setDescription(description);
        return asset;
    }
}
