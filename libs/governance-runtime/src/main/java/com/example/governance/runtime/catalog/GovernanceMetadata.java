package com.example.governance.runtime.catalog;

/**
 * Authoritative governance attributes resolved from the external registry (OpenMetadata)
 * via governance-service. Versioned so cache refresh can be atomic and monotonic.
 */
public record GovernanceMetadata(
        String assetId,
        long version,
        String owner,
        String steward,
        String domain,
        String classification,
        String retentionPolicy,
        String description
) {
}
