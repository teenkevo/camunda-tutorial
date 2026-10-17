package com.example.registration.integration.nira;

import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
public class NiraClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public NiraClient(
            RestTemplate restTemplate,
            @Value("${app.nira.base-url:http://localhost:8086}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = stripTrailingSlash(baseUrl);
    }

    public Optional<NiraPerson> lookup(String nationalIdentificationNumber, String dateOfBirth, String identifierType) {
        try {
            NiraPerson person = restTemplate.postForObject(
                    baseUrl + "/api/nira/persons/lookup",
                    Map.of(
                            "nationalIdentificationNumber", nationalIdentificationNumber,
                            "dateOfBirth", dateOfBirth,
                            "identifierType", identifierType
                    ),
                    NiraPerson.class
            );
            return Optional.ofNullable(person);
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
