package com.example.governance.runtime.catalog;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.governance.catalog")
public class GovernanceCatalogProperties {

    /** When true, scan entities and push metadata to the governance service on startup. */
    private boolean publishOnStartup = true;

    /** Logical service name registered in the catalog. */
    private String serviceName = "unknown-service";

    /** Base URL of the governance microservice, e.g. http://localhost:8081 */
    private String baseUrl = "http://localhost:8081";

    /** Fail application startup if catalog sync or conformance fails. */
    private boolean failOnError = false;

    /** Fail startup when conformance report is non-compliant. */
    private boolean failOnNonCompliant = false;

    /**
     * How many times to retry catalog sync when governance-service is not reachable yet
     * (common when domain services boot in parallel).
     */
    private int syncMaxAttempts = 30;

    /** Delay between sync retry attempts. */
    private long syncRetryDelayMs = 2000L;
}

