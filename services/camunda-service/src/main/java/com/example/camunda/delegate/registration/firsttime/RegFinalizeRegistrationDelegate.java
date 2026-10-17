package com.example.camunda.delegate.registration.firsttime;

import com.example.camunda.client.DomainServiceClient;
import java.util.Map;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("regFinalizeRegistrationDelegate")
public class RegFinalizeRegistrationDelegate implements JavaDelegate {

    private final DomainServiceClient domainServiceClient;

    public RegFinalizeRegistrationDelegate(DomainServiceClient domainServiceClient) {
        this.domainServiceClient = domainServiceClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Map<?, ?> result = domainServiceClient.postRegistration(
                "/internal/reg-first-time/finalize",
                RegValidateIdentityDelegate.baseBody(execution)
        );
        execution.setVariable("registrationStatus", result.get("registrationStatus"));
        execution.setVariable("taxpayerId", result.get("taxpayerId"));
    }
}
