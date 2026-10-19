package com.example.camunda.delegate.refund;

import com.example.camunda.client.DomainServiceClient;
import java.util.HashMap;
import java.util.Map;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("createRefundDocumentDelegate")
public class CreateRefundDocumentDelegate implements JavaDelegate {

    private final DomainServiceClient domainServiceClient;

    public CreateRefundDocumentDelegate(DomainServiceClient domainServiceClient) {
        this.domainServiceClient = domainServiceClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("firstName", execution.getVariable("firstName"));
        payload.put("lastName", execution.getVariable("lastName"));
        payload.put("taxpayerName", execution.getVariable("taxpayerName"));
        payload.put("taxTypes", execution.getVariable("taxTypes"));

        Map<String, Object> body = new HashMap<>();
        body.put("documentType", "REFUND_APPLICATION");
        body.put("processInstanceId", execution.getProcessInstanceId());
        body.put("createdBy", "refund-application");
        body.put("payload", payload);

        Map<?, ?> result = domainServiceClient.postDocument("/internal/documents", body);
        execution.setVariable("drn", result.get("drn"));
        execution.setVariable("documentId", result.get("id"));
        execution.setVariable("applicationStatus", result.get("status"));
    }
}
