package com.example.refund.client;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class DocumentClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public DocumentClient(
            RestTemplate restTemplate,
            @Value("${app.document.base-url:http://localhost:8085}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public Map<String, Object> getByProcessInstanceId(String processInstanceId) {
        return restTemplate.exchange(
                baseUrl + "/internal/documents/by-process/" + processInstanceId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        ).getBody();
    }

    public Map<String, Object> getByDrn(String drn) {
        return restTemplate.exchange(
                baseUrl + "/internal/documents/" + drn,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        ).getBody();
    }

    public List<Map<String, Object>> list(String documentType) {
        return restTemplate.exchange(
                baseUrl + "/internal/documents?documentType=" + documentType,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();
    }

    public Map<String, Object> update(String drn, Map<String, Object> body) {
        return restTemplate.exchange(
                baseUrl + "/internal/documents/" + drn,
                HttpMethod.PATCH,
                new HttpEntity<>(body),
                new ParameterizedTypeReference<Map<String, Object>>() {}
        ).getBody();
    }
}
