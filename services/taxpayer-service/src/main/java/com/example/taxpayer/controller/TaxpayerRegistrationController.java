package com.example.taxpayer.controller;

import com.example.camunda.client.CamundaProcessClient;
import com.example.taxpayer.dto.TaxpayerRegistrationRequest;
import com.example.taxpayer.dto.TaxpayerRegistrationStartResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/taxpayer-registrations")
public class TaxpayerRegistrationController {

    private final CamundaProcessClient camundaProcessClient;

    public TaxpayerRegistrationController(CamundaProcessClient camundaProcessClient) {
        this.camundaProcessClient = camundaProcessClient;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaxpayerRegistrationStartResponse start(@RequestBody TaxpayerRegistrationRequest request) {
        if (request.getTaxpayerName() == null || request.getTaxpayerName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "taxpayerName is required");
        }
        if (request.getNationalId() == null || request.getNationalId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nationalId is required");
        }
        if (request.getTaxTypes() == null || request.getTaxTypes().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one tax type is required");
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("taxpayerName", request.getTaxpayerName().trim());
        variables.put("nationalId", request.getNationalId().trim());
        variables.put("taxTypes", request.getTaxTypes());
        variables.put(
                "taxTypesInput",
                request.getTaxTypes().stream().map(String::trim).collect(Collectors.joining(", "))
        );

        String processInstanceId = camundaProcessClient.startProcess("taxpayer-registration", variables);

        TaxpayerRegistrationStartResponse response = new TaxpayerRegistrationStartResponse();
        response.setProcessInstanceId(processInstanceId);
        response.setProcessDefinitionKey("taxpayer-registration");
        response.setTaxpayerName(request.getTaxpayerName().trim());
        response.setTaxTypes(request.getTaxTypes());
        return response;
    }
}
