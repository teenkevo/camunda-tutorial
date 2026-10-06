package com.example.camundatutorial.refund;

import java.time.Instant;
import java.util.UUID;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("createApplicationDelegate")
public class CreateApplicationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(CreateApplicationDelegate.class);

    private final RefundApplicationService refundApplicationService;

    public CreateApplicationDelegate(RefundApplicationService refundApplicationService) {
        this.refundApplicationService = refundApplicationService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String firstName = String.valueOf(execution.getVariable("firstName"));
        String lastName = String.valueOf(execution.getVariable("lastName"));

        String applicationId = "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Instant createdAt = Instant.now();

        refundApplicationService.create(
                applicationId,
                execution.getProcessInstanceId(),
                firstName,
                lastName
        );

        execution.setVariable("applicationId", applicationId);
        execution.setVariable("applicationStatus", "CREATED");
        execution.setVariable("createdAt", createdAt.toString());

        log.info("Created refund application {} for {} {} (persisted in H2)", applicationId, firstName, lastName);
    }
}
