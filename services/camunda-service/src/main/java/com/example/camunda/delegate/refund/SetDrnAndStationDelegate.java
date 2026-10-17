package com.example.camunda.delegate.refund;

import com.example.camunda.client.DomainServiceClient;
import java.util.Map;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("setDrnAndStationDelegate")
public class SetDrnAndStationDelegate implements JavaDelegate {

    private final DomainServiceClient domainServiceClient;

    public SetDrnAndStationDelegate(DomainServiceClient domainServiceClient) {
        this.domainServiceClient = domainServiceClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String drn = String.valueOf(execution.getVariable("drn"));
        Map<?, ?> result = domainServiceClient.postRefund(
                "/internal/refund-process/" + drn + "/assign-station",
                Map.of()
        );
        execution.setVariable("station", result.get("station"));
        execution.setVariable("applicationStatus", result.get("applicationStatus"));
    }
}
