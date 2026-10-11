package com.example.camunda.delegate.refund;

import com.example.camunda.client.DomainServiceClient;
import java.util.Map;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("checkRiskLevelDelegate")
public class CheckRiskLevelDelegate implements JavaDelegate {

    private final DomainServiceClient domainServiceClient;

    public CheckRiskLevelDelegate(DomainServiceClient domainServiceClient) {
        this.domainServiceClient = domainServiceClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String applicationId = String.valueOf(execution.getVariable("applicationId"));
        Map<?, ?> result = domainServiceClient.postRefund(
                "/internal/refund-process/" + applicationId + "/check-risk",
                Map.of()
        );
        execution.setVariable("riskLevel", result.get("riskLevel"));
        execution.setVariable("applicationStatus", result.get("applicationStatus"));
    }
}
