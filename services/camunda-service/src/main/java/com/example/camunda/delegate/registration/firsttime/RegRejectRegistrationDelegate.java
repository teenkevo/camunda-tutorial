package com.example.camunda.delegate.registration.firsttime;

import com.example.camunda.client.DomainServiceClient;
import java.util.Map;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("regRejectRegistrationDelegate")
public class RegRejectRegistrationDelegate implements JavaDelegate {

    private final DomainServiceClient domainServiceClient;

    public RegRejectRegistrationDelegate(DomainServiceClient domainServiceClient) {
        this.domainServiceClient = domainServiceClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Map<String, Object> body = RegValidateIdentityDelegate.baseBody(execution);
        body.put("approved", false);
        body.put("reviewComment", execution.getVariable("reviewComment"));
        if (body.get("reviewComment") == null) {
            body.put("reviewComment", execution.getVariable("validationMessage"));
        }
        Map<?, ?> result = domainServiceClient.postRegistration("/internal/reg-first-time/reject", body);
        execution.setVariable("registrationStatus", result.get("registrationStatus"));
    }
}
