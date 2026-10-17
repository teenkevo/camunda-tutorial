package com.example.camunda.delegate.registration.firsttime;

import com.example.camunda.client.DomainServiceClient;
import java.util.Map;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("regPrepareTaxTypesDelegate")
public class RegPrepareTaxTypesDelegate implements JavaDelegate {

    private final DomainServiceClient domainServiceClient;

    public RegPrepareTaxTypesDelegate(DomainServiceClient domainServiceClient) {
        this.domainServiceClient = domainServiceClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Map<?, ?> result = domainServiceClient.postRegistration(
                "/internal/reg-first-time/prepare-tax-types",
                RegValidateIdentityDelegate.baseBody(execution)
        );
        execution.setVariable("taxTypes", result.get("taxTypes"));
        execution.setVariable("registeredTaxTypes", result.get("registeredTaxTypes"));
    }
}
