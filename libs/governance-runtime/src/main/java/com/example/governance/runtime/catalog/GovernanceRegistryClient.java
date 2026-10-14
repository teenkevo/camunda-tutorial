package com.example.governance.runtime.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Fetches authoritative governance metadata from governance-service
 * (which resolves attributes from OpenMetadata).
 */
@Component
public class GovernanceRegistryClient {

    private static final Logger log = LoggerFactory.getLogger(GovernanceRegistryClient.class);

    private final RestTemplate restTemplate;
    private final GovernanceCatalogProperties properties;

    public GovernanceRegistryClient(RestTemplateBuilder builder, GovernanceCatalogProperties properties) {
        this.restTemplate = builder.build();
        this.properties = properties;
    }

    public GovernanceMetadata fetch(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            throw new IllegalArgumentException("assetId is required");
        }
        String url = org.springframework.web.util.UriComponentsBuilder
                .fromHttpUrl(trimSlash(properties.getBaseUrl()) + "/api/governance/assets")
                .pathSegment(assetId.trim())
                .toUriString();
        GovernanceMetadataResponse body = restTemplate.getForObject(url, GovernanceMetadataResponse.class);
        if (body == null || body.assetId() == null) {
            throw new IllegalStateException("Empty registry response for asset " + assetId);
        }
        return body.toMetadata();
    }

    public GovernanceMetadata fetchSafe(String assetId) {
        try {
            return fetch(assetId);
        } catch (RestClientException | IllegalStateException | IllegalArgumentException ex) {
            log.warn("Failed to fetch governance metadata for {}: {}", assetId, ex.getMessage());
            return null;
        }
    }

    private static String trimSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:8081";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /** Wire format from governance-service resolve API. */
    public record GovernanceMetadataResponse(
            String assetId,
            Long version,
            String owner,
            String steward,
            String domain,
            String classification,
            String retentionPolicy,
            String description
    ) {
        GovernanceMetadata toMetadata() {
            return new GovernanceMetadata(
                    assetId,
                    version == null ? 0L : version,
                    owner,
                    steward,
                    domain,
                    classification,
                    retentionPolicy,
                    description
            );
        }
    }
}
