package com.example.camunda.delegate.registration.firsttime;

import com.example.camunda.client.DomainServiceClient;
import java.util.Map;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("regRegisterTaxTypeDelegate")
public class RegRegisterTaxTypeDelegate implements JavaDelegate {

    private final DomainServiceClient domainServiceClient;

    public RegRegisterTaxTypeDelegate(DomainServiceClient domainServiceClient) {
        this.domainServiceClient = domainServiceClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Map<String, Object> body = RegValidateIdentityDelegate.baseBody(execution);
        body.put("taxType", execution.getVariable("taxType"));
        Map<?, ?> result = domainServiceClient.postRegistration("/internal/reg-first-time/register-tax-type", body);
        execution.setVariable("registeredTaxTypes", result.get("registeredTaxTypes"));
        execution.setVariable("lastRegistrationRef", result.get("lastRegistrationRef"));
    }
}
