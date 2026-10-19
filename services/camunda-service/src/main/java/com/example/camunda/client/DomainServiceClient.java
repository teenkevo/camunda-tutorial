package com.example.camunda.client;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class DomainServiceClient {

    private final RestTemplate restTemplate;
    private final String refundBaseUrl;
    private final String registrationBaseUrl;
    private final String documentBaseUrl;

    public DomainServiceClient(
            RestTemplate restTemplate,
            @Value("${app.services.refund-base-url}") String refundBaseUrl,
            @Value("${app.services.registration-base-url}") String registrationBaseUrl,
            @Value("${app.services.document-base-url}") String documentBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.refundBaseUrl = trim(refundBaseUrl);
        this.registrationBaseUrl = trim(registrationBaseUrl);
        this.documentBaseUrl = trim(documentBaseUrl);
    }

    public Map<?, ?> postRefund(String path, Object body) {
        return restTemplate.postForObject(refundBaseUrl + path, body, Map.class);
    }

    public Map<?, ?> postRegistration(String path, Object body) {
        return restTemplate.postForObject(registrationBaseUrl + path, body, Map.class);
    }

    public Map<?, ?> postDocument(String path, Object body) {
        return restTemplate.postForObject(documentBaseUrl + path, body, Map.class);
    }

    private static String trim(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
