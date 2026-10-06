package com.example.camundatutorial.taxpayer;

import java.util.List;
import java.util.UUID;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("finalizeRegistrationDelegate")
public class FinalizeRegistrationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(FinalizeRegistrationDelegate.class);

    @Override
    @SuppressWarnings("unchecked")
    public void execute(DelegateExecution execution) {
        String nationalId = String.valueOf(execution.getVariable("nationalId"));
        List<String> registeredTaxTypes = (List<String>) execution.getVariable("registeredTaxTypes");

        String tin = "TIN-" + nationalId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        execution.setVariable("tin", tin);
        execution.setVariable("registrationStatus", "ACTIVE");

        log.info("Issued TIN {} for nationalId={} with tax types {}", tin, nationalId, registeredTaxTypes);
    }
}
