package com.example.camunda.client;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class DomainServiceClient {

    private final RestTemplate restTemplate;
    private final String refundBaseUrl;
    private final String taxpayerBaseUrl;

    public DomainServiceClient(
            RestTemplate restTemplate,
            @Value("${app.services.refund-base-url}") String refundBaseUrl,
            @Value("${app.services.taxpayer-base-url}") String taxpayerBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.refundBaseUrl = trim(refundBaseUrl);
        this.taxpayerBaseUrl = trim(taxpayerBaseUrl);
    }

    public Map<?, ?> postRefund(String path, Object body) {
        return restTemplate.postForObject(refundBaseUrl + path, body, Map.class);
    }

    public Map<?, ?> postTaxpayer(String path, Object body) {
        return restTemplate.postForObject(taxpayerBaseUrl + path, body, Map.class);
    }

    private static String trim(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
