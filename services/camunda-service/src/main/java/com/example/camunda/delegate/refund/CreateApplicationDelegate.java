package com.example.camunda.delegate.refund;

import com.example.camunda.client.DomainServiceClient;
import java.util.HashMap;
import java.util.Map;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("createApplicationDelegate")
public class CreateApplicationDelegate implements JavaDelegate {

    private final DomainServiceClient domainServiceClient;

    public CreateApplicationDelegate(DomainServiceClient domainServiceClient) {
        this.domainServiceClient = domainServiceClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Map<String, Object> body = new HashMap<>();
        body.put("processInstanceId", execution.getProcessInstanceId());
        body.put("firstName", execution.getVariable("firstName"));
        body.put("lastName", execution.getVariable("lastName"));

        Map<?, ?> result = domainServiceClient.postRefund("/internal/refund-process/create", body);
        execution.setVariable("applicationId", result.get("applicationId"));
        execution.setVariable("applicationStatus", result.get("applicationStatus"));
        execution.setVariable("createdAt", result.get("createdAt"));
    }
}
