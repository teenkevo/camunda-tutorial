package com.example.camunda.delegate.taxpayer;

import com.example.camunda.client.DomainServiceClient;
import java.util.HashMap;
import java.util.Map;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("finalizeRegistrationDelegate")
public class FinalizeRegistrationDelegate implements JavaDelegate {

    private final DomainServiceClient domainServiceClient;

    public FinalizeRegistrationDelegate(DomainServiceClient domainServiceClient) {
        this.domainServiceClient = domainServiceClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Map<String, Object> body = new HashMap<>();
        body.put("processInstanceId", execution.getProcessInstanceId());
        body.put("taxpayerName", execution.getVariable("taxpayerName"));
        body.put("nationalId", execution.getVariable("nationalId"));
        body.put("registeredTaxTypes", execution.getVariable("registeredTaxTypes"));

        Map<?, ?> result = domainServiceClient.postTaxpayer("/internal/taxpayer-process/finalize", body);
        execution.setVariable("tin", result.get("tin"));
        execution.setVariable("registrationStatus", result.get("registrationStatus"));
    }
}
