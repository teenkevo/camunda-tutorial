package com.example.camundatutorial.taxpayer;

import java.util.ArrayList;
import java.util.List;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("registerTaxTypeDelegate")
public class RegisterTaxTypeDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(RegisterTaxTypeDelegate.class);

    @Override
    @SuppressWarnings("unchecked")
    public void execute(DelegateExecution execution) {
        String taxType = String.valueOf(execution.getVariable("taxType"));
        String taxpayerName = String.valueOf(execution.getVariable("taxpayerName"));
        String nationalId = String.valueOf(execution.getVariable("nationalId"));

        // Placeholder for a real tax-type registration call
        String registrationRef = nationalId + "-" + taxType;

        List<String> registeredTaxTypes = (List<String>) execution.getVariable("registeredTaxTypes");
        if (registeredTaxTypes == null) {
            registeredTaxTypes = new ArrayList<>();
        }
        registeredTaxTypes.add(taxType);
        execution.setVariable("registeredTaxTypes", registeredTaxTypes);
        execution.setVariable("lastRegistrationRef", registrationRef);

        log.info("Registered tax type {} for {} ({}) -> {}", taxType, taxpayerName, nationalId, registrationRef);
    }
}
