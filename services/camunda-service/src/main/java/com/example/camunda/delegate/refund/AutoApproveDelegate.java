package com.example.camunda.delegate.refund;

import com.example.camunda.client.DomainServiceClient;
import java.util.Map;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("autoApproveDelegate")
public class AutoApproveDelegate implements JavaDelegate {

    private final DomainServiceClient domainServiceClient;

    public AutoApproveDelegate(DomainServiceClient domainServiceClient) {
        this.domainServiceClient = domainServiceClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String drn = String.valueOf(execution.getVariable("drn"));
        Map<?, ?> result = domainServiceClient.postRefund(
                "/internal/refund-process/" + drn + "/auto-approve",
                Map.of()
        );
        execution.setVariable("approved", result.get("approved"));
        execution.setVariable("reviewComment", result.get("reviewComment"));
        execution.setVariable("applicationStatus", result.get("applicationStatus"));
    }
}
