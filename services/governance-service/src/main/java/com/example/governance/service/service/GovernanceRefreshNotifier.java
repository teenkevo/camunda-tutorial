package com.example.governance.service.service;

import com.example.governance.service.config.OpenMetadataProperties;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Fans out registry change notifications to domain-service refresh endpoints.
 */
@Component
public class GovernanceRefreshNotifier {

    private static final Logger log = LoggerFactory.getLogger(GovernanceRefreshNotifier.class);

    private final RestTemplate restTemplate;
    private final OpenMetadataProperties properties;

    public GovernanceRefreshNotifier(RestTemplateBuilder builder, OpenMetadataProperties properties) {
        this.restTemplate = builder.build();
        this.properties = properties;
    }

    public void notifyAssetUpdated(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return;
        }
        Map<String, String> urls = properties.getServiceRefreshUrls();
        if (urls == null || urls.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : urls.entrySet()) {
            String base = entry.getValue();
            if (base == null || base.isBlank()) {
                continue;
            }
            String url = UriComponentsBuilder
                    .fromHttpUrl(trimSlash(base) + "/internal/governance/refresh")
                    .queryParam("assetId", assetId.trim())
                    .toUriString();
            try {
                restTemplate.postForLocation(url, Map.of("assetId", assetId.trim()));
                log.info("Notified {} to refresh governance cache for {}", entry.getKey(), assetId);
            } catch (RestClientException ex) {
                log.warn("Failed to notify {} ({}): {}", entry.getKey(), url, ex.getMessage());
            }
        }
    }

    private static String trimSlash(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
