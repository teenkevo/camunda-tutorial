package com.example.camundatutorial.taxpayer;

import java.util.List;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("validateTaxpayerDelegate")
public class ValidateTaxpayerDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ValidateTaxpayerDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        String taxpayerName = asText(execution.getVariable("taxpayerName"));
        String nationalId = asText(execution.getVariable("nationalId"));
        @SuppressWarnings("unchecked")
        List<String> taxTypes = (List<String>) execution.getVariable("taxTypes");

        boolean valid = taxpayerName != null
                && nationalId != null
                && nationalId.length() >= 5
                && taxTypes != null
                && !taxTypes.isEmpty();

        String validationMessage = valid
                ? "Taxpayer data looks valid"
                : "Taxpayer name, a national/business ID (min 5 chars), and at least one tax type are required";

        execution.setVariable("valid", valid);
        execution.setVariable("validationMessage", validationMessage);
        log.info("Validation for '{}' ({}): {} - {}", taxpayerName, nationalId, valid, validationMessage);
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
