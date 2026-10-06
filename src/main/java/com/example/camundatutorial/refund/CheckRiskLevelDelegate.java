package com.example.camundatutorial.refund;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("checkRiskLevelDelegate")
public class CheckRiskLevelDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(CheckRiskLevelDelegate.class);

    private static final List<String> RISK_LEVELS = List.of("low", "medium", "high");

    private final RefundApplicationService refundApplicationService;

    public CheckRiskLevelDelegate(RefundApplicationService refundApplicationService) {
        this.refundApplicationService = refundApplicationService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String applicationId = String.valueOf(execution.getVariable("applicationId"));
        String taxpayerName = String.valueOf(execution.getVariable("taxpayerName"));

        String riskLevel = RISK_LEVELS.get(ThreadLocalRandom.current().nextInt(RISK_LEVELS.size()));
        String status = "low".equals(riskLevel) ? "RISK_CHECKED" : "PENDING_REVIEW";

        refundApplicationService.update(applicationId, entity -> {
            entity.setRiskLevel(riskLevel);
            entity.setStatus(status);
        });

        execution.setVariable("riskLevel", riskLevel);
        execution.setVariable("applicationStatus", status);

        log.info("Risk level for application {} ({}): {}", applicationId, taxpayerName, riskLevel);
    }
}
