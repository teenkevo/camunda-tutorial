package com.example.camunda.delegate.refund;

import com.example.camunda.client.DomainServiceClient;
import java.util.HashMap;
import java.util.Map;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("notifyTaxpayerDelegate")
public class NotifyTaxpayerDelegate implements JavaDelegate {

    private final DomainServiceClient domainServiceClient;

    public NotifyTaxpayerDelegate(DomainServiceClient domainServiceClient) {
        this.domainServiceClient = domainServiceClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String applicationId = String.valueOf(execution.getVariable("applicationId"));
        Object approvedVar = execution.getVariable("approved");
        boolean approved = approvedVar instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(approvedVar));

        Map<String, Object> body = new HashMap<>();
        body.put("firstName", execution.getVariable("firstName"));
        body.put("lastName", execution.getVariable("lastName"));
        body.put("drn", execution.getVariable("drn"));
        body.put("station", execution.getVariable("station"));
        body.put("approved", approved);
        body.put("reviewComment", execution.getVariable("reviewComment"));

        Map<?, ?> result = domainServiceClient.postRefund(
                "/internal/refund-process/" + applicationId + "/notify",
                body
        );
        execution.setVariable("notificationMessage", result.get("notificationMessage"));
        execution.setVariable("applicationStatus", result.get("applicationStatus"));
        execution.setVariable("decision", result.get("decision"));
    }
}
