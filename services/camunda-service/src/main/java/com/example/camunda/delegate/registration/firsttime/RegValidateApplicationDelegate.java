package com.example.camunda.delegate.registration.firsttime;

import com.example.camunda.client.DomainServiceClient;
import java.util.Map;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("regValidateApplicationDelegate")
public class RegValidateApplicationDelegate implements JavaDelegate {

    private final DomainServiceClient domainServiceClient;

    public RegValidateApplicationDelegate(DomainServiceClient domainServiceClient) {
        this.domainServiceClient = domainServiceClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Map<?, ?> result = domainServiceClient.postRegistration(
                "/internal/reg-first-time/validate",
                RegValidateIdentityDelegate.baseBody(execution)
        );
        execution.setVariable("valid", result.get("valid"));
        execution.setVariable("validationMessage", result.get("validationMessage"));
    }
}
