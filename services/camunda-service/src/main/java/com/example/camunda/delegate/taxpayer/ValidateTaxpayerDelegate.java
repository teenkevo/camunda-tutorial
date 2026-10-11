package com.example.camunda.delegate.taxpayer;

import com.example.camunda.client.DomainServiceClient;
import java.util.HashMap;
import java.util.Map;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("validateTaxpayerDelegate")
public class ValidateTaxpayerDelegate implements JavaDelegate {

    private final DomainServiceClient domainServiceClient;

    public ValidateTaxpayerDelegate(DomainServiceClient domainServiceClient) {
        this.domainServiceClient = domainServiceClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Map<String, Object> body = new HashMap<>();
        body.put("taxpayerName", execution.getVariable("taxpayerName"));
        body.put("nationalId", execution.getVariable("nationalId"));
        body.put("taxTypes", execution.getVariable("taxTypes"));

        Map<?, ?> result = domainServiceClient.postTaxpayer("/internal/taxpayer-process/validate", body);
        execution.setVariable("valid", result.get("valid"));
        execution.setVariable("validationMessage", result.get("validationMessage"));
    }
}
