package com.example.camunda.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.camunda")
public class CamundaClientProperties {

    /**
     * Base URL of shared camunda-service (no trailing slash required).
     */
    private String baseUrl = "http://localhost:8080";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
