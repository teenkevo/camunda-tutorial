package com.example.camunda.delegate.registration.firsttime;

import com.example.camunda.client.DomainServiceClient;
import java.util.HashMap;
import java.util.Map;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("regValidateIdentityDelegate")
public class RegValidateIdentityDelegate implements JavaDelegate {

    private final DomainServiceClient domainServiceClient;

    public RegValidateIdentityDelegate(DomainServiceClient domainServiceClient) {
        this.domainServiceClient = domainServiceClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Map<String, Object> body = baseBody(execution);
        Map<?, ?> result = domainServiceClient.postRegistration("/internal/reg-first-time/validate-identity", body);
        execution.setVariable("identityMatched", result.get("identityMatched"));
        execution.setVariable("valid", result.get("valid"));
        execution.setVariable("validationMessage", result.get("validationMessage"));
        if (result.get("taxpayerName") != null) {
            execution.setVariable("taxpayerName", result.get("taxpayerName"));
        }
        if (result.get("email") != null) {
            execution.setVariable("email", result.get("email"));
        }
        if (result.get("telephone") != null) {
            execution.setVariable("telephone", result.get("telephone"));
        }
    }

    static Map<String, Object> baseBody(DelegateExecution execution) {
        Map<String, Object> body = new HashMap<>();
        body.put("drn", execution.getVariable("drn"));
        body.put("processInstanceId", execution.getProcessInstanceId());
        body.put("identifierType", execution.getVariable("identifierType"));
        body.put("identifierValue", execution.getVariable("identifierValue"));
        body.put("taxpayerName", execution.getVariable("taxpayerName"));
        body.put("dateOfBirthOrIncorp", execution.getVariable("dateOfBirthOrIncorp"));
        body.put("email", execution.getVariable("email"));
        body.put("telephone", execution.getVariable("telephone"));
        body.put("taxTypes", execution.getVariable("taxTypes"));
        body.put("taxTypesInput", execution.getVariable("taxTypesInput"));
        body.put("registeredTaxTypes", execution.getVariable("registeredTaxTypes"));
        return body;
    }
}
