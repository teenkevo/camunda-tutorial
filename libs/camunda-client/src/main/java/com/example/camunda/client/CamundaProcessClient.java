package com.example.camunda.client;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Thin HTTP adapter for starting process instances on shared camunda-service.
 */
public class CamundaProcessClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public CamundaProcessClient(RestTemplateBuilder builder, CamundaClientProperties properties) {
        this.restTemplate = builder.build();
        String url = properties.getBaseUrl() == null ? "http://localhost:8080" : properties.getBaseUrl();
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public String startProcess(String processKey, Map<String, Object> variables) {
        Map<String, Object> body = new HashMap<>();
        body.put("processKey", processKey);
        body.put("variables", variables == null ? Map.of() : variables);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/api/process-instances",
                body,
                Map.class
        );
        Map<?, ?> payload = response.getBody();
        if (payload == null || payload.get("processInstanceId") == null) {
            throw new IllegalStateException("Camunda did not return a processInstanceId");
        }
        return String.valueOf(payload.get("processInstanceId"));
    }
}
