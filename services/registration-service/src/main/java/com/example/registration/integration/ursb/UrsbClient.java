package com.example.registration.integration.ursb;

import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
public class UrsbClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public UrsbClient(
            RestTemplate restTemplate,
            @Value("${app.ursb.base-url:http://localhost:8086}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = stripTrailingSlash(baseUrl);
    }

    public Optional<UrsbEntity> lookup(String registrationNumber, String registrationDate) {
        try {
            UrsbEntity entity = restTemplate.postForObject(
                    baseUrl + "/api/ursb/entities/lookup",
                    Map.of(
                            "registrationNumber", registrationNumber,
                            "registrationDate", registrationDate
                    ),
                    UrsbEntity.class
            );
            return Optional.ofNullable(entity);
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(404))) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
