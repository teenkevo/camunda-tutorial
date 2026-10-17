package com.example.camunda.delegate.registration.firsttime;

import com.example.camunda.client.DomainServiceClient;
import java.util.HashMap;
import java.util.Map;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("regCreateDocumentDelegate")
public class RegCreateDocumentDelegate implements JavaDelegate {

    private final DomainServiceClient domainServiceClient;

    public RegCreateDocumentDelegate(DomainServiceClient domainServiceClient) {
        this.domainServiceClient = domainServiceClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("identifierType", execution.getVariable("identifierType"));
        payload.put("identifierValue", execution.getVariable("identifierValue"));
        payload.put("dateOfBirthOrIncorp", execution.getVariable("dateOfBirthOrIncorp"));
        payload.put("email", execution.getVariable("email"));
        payload.put("telephone", execution.getVariable("telephone"));
        payload.put("taxpayerName", execution.getVariable("taxpayerName"));
        payload.put("taxTypes", execution.getVariable("taxTypes"));
        payload.put("registeredTaxTypes", java.util.List.of());
        payload.put("otpVerified", true);

        Map<String, Object> body = new HashMap<>();
        body.put("documentType", documentType(execution.getVariable("identifierType")));
        body.put("processInstanceId", execution.getProcessInstanceId());
        body.put("createdBy", "reg-first-time-taxpayer");
        body.put("payload", payload);

        Map<?, ?> result = domainServiceClient.postDocument("/internal/documents", body);
        execution.setVariable("drn", result.get("drn"));
        execution.setVariable("documentId", result.get("id"));
        execution.setVariable("documentStatus", result.get("status"));
    }

    private static String documentType(Object identifierType) {
        return "ERN".equals(String.valueOf(identifierType))
                ? "NON_INDIVIDUAL_REGISTRATION"
                : "INDIVIDUAL_REGISTRATION";
    }
}
