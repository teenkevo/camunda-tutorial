package com.example.taxpayer.service;

import com.example.taxpayer.dto.TaxpayerProcessRequest;
import com.example.taxpayer.dto.TaxpayerProcessStepResponse;
import com.example.taxpayer.entity.TaxpayerRegistrationEntity;
import com.example.taxpayer.repository.TaxpayerRegistrationRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaxpayerRegistrationService {

    private static final Set<String> SUPPORTED = Set.of("VAT", "PAYE", "CIT", "WHT");

    private final TaxpayerRegistrationRepository repository;

    public TaxpayerRegistrationService(TaxpayerRegistrationRepository repository) {
        this.repository = repository;
    }

    public TaxpayerProcessStepResponse prepareTaxTypes(TaxpayerProcessRequest request) {
        List<String> taxTypes = normalizeTaxTypes(request.getTaxTypesInput());
        if (taxTypes.isEmpty()) {
            taxTypes = normalizeTaxTypes(request.getTaxTypes());
        }
        if (taxTypes.isEmpty()) {
            throw new IllegalArgumentException("At least one tax type is required (VAT, PAYE, CIT, WHT)");
        }
        List<String> unsupported = taxTypes.stream().filter(type -> !SUPPORTED.contains(type)).toList();
        if (!unsupported.isEmpty()) {
            throw new IllegalArgumentException("Unsupported tax types: " + unsupported + ". Supported: " + SUPPORTED);
        }

        TaxpayerProcessStepResponse response = new TaxpayerProcessStepResponse();
        response.setTaxTypes(taxTypes);
        response.setRegisteredTaxTypes(new ArrayList<>());
        return response;
    }

    public TaxpayerProcessStepResponse validate(TaxpayerProcessRequest request) {
        String taxpayerName = asText(request.getTaxpayerName());
        String nationalId = asText(request.getNationalId());
        List<String> taxTypes = normalizeTaxTypes(request.getTaxTypes());

        boolean valid = taxpayerName != null
                && nationalId != null
                && nationalId.length() >= 5
                && !taxTypes.isEmpty();

        TaxpayerProcessStepResponse response = new TaxpayerProcessStepResponse();
        response.setValid(valid);
        response.setValidationMessage(valid
                ? "Taxpayer data looks valid"
                : "Taxpayer name, a national/business ID (min 5 chars), and at least one tax type are required");
        return response;
    }

    public TaxpayerProcessStepResponse registerTaxType(TaxpayerProcessRequest request) {
        String taxType = String.valueOf(request.getTaxType());
        String nationalId = String.valueOf(request.getNationalId());
        String registrationRef = nationalId + "-" + taxType;

        List<String> registered = request.getRegisteredTaxTypes() == null
                ? new ArrayList<>()
                : new ArrayList<>(request.getRegisteredTaxTypes());
        registered.add(taxType);

        TaxpayerProcessStepResponse response = new TaxpayerProcessStepResponse();
        response.setRegisteredTaxTypes(registered);
        response.setLastRegistrationRef(registrationRef);
        return response;
    }

    @Transactional
    public TaxpayerProcessStepResponse finalizeRegistration(TaxpayerProcessRequest request) {
        String nationalId = String.valueOf(request.getNationalId());
        String taxpayerName = String.valueOf(request.getTaxpayerName());
        String tin = "TIN-" + nationalId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        TaxpayerRegistrationEntity entity = repository.findByProcessInstanceId(request.getProcessInstanceId())
                .orElseGet(TaxpayerRegistrationEntity::new);
        entity.setProcessInstanceId(request.getProcessInstanceId());
        entity.setTaxpayerName(taxpayerName);
        entity.setNationalId(nationalId);
        entity.setTin(tin);
        entity.setStatus("ACTIVE");
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        repository.save(entity);

        TaxpayerProcessStepResponse response = new TaxpayerProcessStepResponse();
        response.setTin(tin);
        response.setRegistrationStatus("ACTIVE");
        return response;
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

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
