package com.example.camundatutorial.refund;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("notifyTaxpayerDelegate")
public class NotifyTaxpayerDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(NotifyTaxpayerDelegate.class);

    private final RefundApplicationService refundApplicationService;

    public NotifyTaxpayerDelegate(RefundApplicationService refundApplicationService) {
        this.refundApplicationService = refundApplicationService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String firstName = String.valueOf(execution.getVariable("firstName"));
        String lastName = String.valueOf(execution.getVariable("lastName"));
        String applicationId = String.valueOf(execution.getVariable("applicationId"));
        String drn = String.valueOf(execution.getVariable("drn"));
        String station = String.valueOf(execution.getVariable("station"));
        Object approvedVar = execution.getVariable("approved");
        boolean approved = approvedVar instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(approvedVar));
        Object commentVar = execution.getVariable("reviewComment");
        String reviewComment = commentVar == null ? "" : String.valueOf(commentVar);

        String decision = approved ? "APPROVED" : "REJECTED";
        String message = approved
                ? "Dear %s %s, your refund application %s (DRN %s, station %s) has been APPROVED. %s"
                    .formatted(firstName, lastName, applicationId, drn, station, reviewComment)
                : "Dear %s %s, your refund application %s (DRN %s, station %s) has been REJECTED. %s"
                    .formatted(firstName, lastName, applicationId, drn, station, reviewComment);

        refundApplicationService.update(applicationId, entity -> {
            entity.setApproved(approved);
            entity.setReviewComment(reviewComment);
            entity.setNotificationMessage(message);
            entity.setDecision(decision);
            entity.setStatus(decision);
        });

        execution.setVariable("notificationMessage", message);
        execution.setVariable("applicationStatus", decision);
        execution.setVariable("decision", decision);

        log.info("Message to taxpayer (also stored in H2): {}", message);
    }
}
