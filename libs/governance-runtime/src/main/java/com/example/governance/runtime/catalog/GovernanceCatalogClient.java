package com.example.governance.runtime.catalog;

import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GovernanceCatalogClient {

    private static final Logger log = LoggerFactory.getLogger(GovernanceCatalogClient.class);

    private final RestTemplate restTemplate;
    private final GovernanceCatalogProperties properties;

    public GovernanceCatalogClient(RestTemplateBuilder builder, GovernanceCatalogProperties properties) {
        this.restTemplate = builder.build();
        this.properties = properties;
    }

    public void sync(GovernanceSyncRequest request) {
        String url = trimSlash(properties.getBaseUrl()) + "/api/governance/sync";
        restTemplate.postForObject(url, request, GovernanceAssetDto[].class);
        log.info("Published {} governance assets for service {}",
                request.assets() == null ? 0 : request.assets().size(),
                request.serviceName());
    }

    public ConformanceReport conformance(String serviceName) {
        String url = UriComponentsBuilder
                .fromHttpUrl(trimSlash(properties.getBaseUrl()) + "/api/governance/conformance")
                .queryParam("service", serviceName)
                .toUriString();
        ConformanceReport report = restTemplate.getForObject(url, ConformanceReport.class);
        if (report == null) {
            throw new IllegalStateException("Empty conformance response from governance-service");
        }
        return report;
    }

    public List<GovernanceAssetDto> list(String serviceName) {
        String url = UriComponentsBuilder
                .fromHttpUrl(trimSlash(properties.getBaseUrl()) + "/api/governance/assets")
                .queryParam("service", serviceName)
                .toUriString();
        GovernanceAssetDto[] body = restTemplate.getForObject(url, GovernanceAssetDto[].class);
        return body == null ? List.of() : Arrays.asList(body);
    }

    public void syncSafe(GovernanceSyncRequest request) {
        try {
            sync(request);
        } catch (RestClientException ex) {
            if (properties.isFailOnError()) {
                throw ex;
            }
            log.warn("Governance catalog sync failed (continuing): {}", ex.getMessage());
        }
    }

    private static String trimSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:8081";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
