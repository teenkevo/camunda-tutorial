package com.example.camundatutorial.taxpayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("prepareTaxTypesDelegate")
public class PrepareTaxTypesDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(PrepareTaxTypesDelegate.class);

    private static final Set<String> SUPPORTED = Set.of("VAT", "PAYE", "CIT", "WHT");

    @Override
    public void execute(DelegateExecution execution) {
        // Prefer values confirmed on the capture form; fall back to API-provided list
        List<String> taxTypes = normalizeTaxTypes(execution.getVariable("taxTypesInput"));
        if (taxTypes.isEmpty()) {
            taxTypes = normalizeTaxTypes(execution.getVariable("taxTypes"));
        }

        if (taxTypes.isEmpty()) {
            throw new IllegalArgumentException("At least one tax type is required (VAT, PAYE, CIT, WHT)");
        }

        List<String> unsupported = taxTypes.stream()
                .filter(type -> !SUPPORTED.contains(type))
                .toList();
        if (!unsupported.isEmpty()) {
            throw new IllegalArgumentException("Unsupported tax types: " + unsupported + ". Supported: " + SUPPORTED);
        }

        execution.setVariable("taxTypes", taxTypes);
        execution.setVariable("registeredTaxTypes", new ArrayList<String>());
        log.info("Prepared tax types for {}: {}", execution.getVariable("taxpayerName"), taxTypes);
    }

    @SuppressWarnings("unchecked")
    private List<String> normalizeTaxTypes(Object raw) {
        if (raw == null) {
            return List.of();
        }

        Collection<String> values;
        if (raw instanceof Collection<?> collection) {
            values = collection.stream().map(String::valueOf).collect(Collectors.toList());
        } else {
            values = Arrays.asList(String.valueOf(raw).split(","));
        }

        return values.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new
                ));
    }
}
