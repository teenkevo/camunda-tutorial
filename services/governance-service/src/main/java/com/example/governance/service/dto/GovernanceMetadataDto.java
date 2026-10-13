package com.example.governance.service.dto;

/**
 * Resolved governance attributes for a logical asset (from OpenMetadata + identity catalog).
 */
public record GovernanceMetadataDto(
        String assetId,
        Long version,
        String owner,
        String steward,
        String domain,
        String classification,
        String retentionPolicy,
        String description
) {
}
