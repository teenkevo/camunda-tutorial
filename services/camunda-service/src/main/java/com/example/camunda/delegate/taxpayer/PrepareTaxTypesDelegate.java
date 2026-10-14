package com.example.camunda.delegate.taxpayer;

import com.example.camunda.client.DomainServiceClient;
import java.util.HashMap;
import java.util.Map;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("prepareTaxTypesDelegate")
public class PrepareTaxTypesDelegate implements JavaDelegate {

    private final DomainServiceClient domainServiceClient;

    public PrepareTaxTypesDelegate(DomainServiceClient domainServiceClient) {
        this.domainServiceClient = domainServiceClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Map<String, Object> body = new HashMap<>();
        body.put("taxpayerName", execution.getVariable("taxpayerName"));
        body.put("taxTypesInput", execution.getVariable("taxTypesInput"));
        body.put("taxTypes", execution.getVariable("taxTypes"));

        Map<?, ?> result = domainServiceClient.postTaxpayer("/internal/taxpayer-process/prepare-tax-types", body);
        execution.setVariable("taxTypes", result.get("taxTypes"));
        execution.setVariable("registeredTaxTypes", result.get("registeredTaxTypes"));
    }
}
