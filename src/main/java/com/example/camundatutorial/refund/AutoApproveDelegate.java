package com.example.camundatutorial.refund;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("autoApproveDelegate")
public class AutoApproveDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(AutoApproveDelegate.class);

    private final RefundApplicationService refundApplicationService;

    public AutoApproveDelegate(RefundApplicationService refundApplicationService) {
        this.refundApplicationService = refundApplicationService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String applicationId = String.valueOf(execution.getVariable("applicationId"));
        String reviewComment = "Automatically approved due to low risk level";

        refundApplicationService.update(applicationId, entity -> {
            entity.setApproved(true);
            entity.setReviewComment(reviewComment);
            entity.setStatus("AUTO_APPROVED");
        });

        execution.setVariable("approved", true);
        execution.setVariable("applicationStatus", "AUTO_APPROVED");
        execution.setVariable("reviewComment", reviewComment);

        log.info("Auto-approved low-risk refund application {}", applicationId);
    }
}
