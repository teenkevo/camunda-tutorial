package com.example.camunda.delegate.taxpayer;

import com.example.camunda.client.DomainServiceClient;
import java.util.HashMap;
import java.util.Map;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("registerTaxTypeDelegate")
public class RegisterTaxTypeDelegate implements JavaDelegate {

    private final DomainServiceClient domainServiceClient;

    public RegisterTaxTypeDelegate(DomainServiceClient domainServiceClient) {
        this.domainServiceClient = domainServiceClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Map<String, Object> body = new HashMap<>();
        body.put("taxType", execution.getVariable("taxType"));
        body.put("taxpayerName", execution.getVariable("taxpayerName"));
        body.put("nationalId", execution.getVariable("nationalId"));
        body.put("registeredTaxTypes", execution.getVariable("registeredTaxTypes"));

        Map<?, ?> result = domainServiceClient.postTaxpayer("/internal/taxpayer-process/register-tax-type", body);
        execution.setVariable("registeredTaxTypes", result.get("registeredTaxTypes"));
        execution.setVariable("lastRegistrationRef", result.get("lastRegistrationRef"));
    }
}
