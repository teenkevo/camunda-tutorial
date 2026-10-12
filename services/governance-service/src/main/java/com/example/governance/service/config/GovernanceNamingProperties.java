package com.example.governance.service.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Allow-list of data domains and their table naming conventions.
 * Entities outside this list (or with non-matching table names) fail conformance.
 */
@Data
@ConfigurationProperties(prefix = "app.governance.naming")
public class GovernanceNamingProperties {

    /**
     * When true, entity-level assets must use a configured domain and a table name
     * that matches that domain's convention.
     */
    private boolean enforce = true;

    private List<GovernanceDomainNamingRule> domains = new ArrayList<>();

    public Optional<GovernanceDomainNamingRule> findDomain(String domainId) {
        if (domainId == null || domainId.isBlank()) {
            return Optional.empty();
        }
        String needle = domainId.trim();
        return domains.stream()
                .filter(d -> d.getId() != null && d.getId().equalsIgnoreCase(needle))
                .findFirst();
    }
}
