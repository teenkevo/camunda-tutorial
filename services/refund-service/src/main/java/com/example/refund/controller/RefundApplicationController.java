package com.example.refund.controller;

import com.example.camunda.client.CamundaProcessClient;
import com.example.refund.dto.RefundApplicationRequest;
import com.example.refund.dto.RefundApplicationResponse;
import com.example.refund.dto.RefundApplicationStartResponse;
import com.example.refund.mapper.RefundApplicationMapper;
import com.example.refund.service.RefundApplicationService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/refund-applications")
public class RefundApplicationController {

    private final CamundaProcessClient camundaProcessClient;
    private final RefundApplicationService refundApplicationService;
    private final RefundApplicationMapper refundApplicationMapper;

    public RefundApplicationController(
            CamundaProcessClient camundaProcessClient,
            RefundApplicationService refundApplicationService,
            RefundApplicationMapper refundApplicationMapper
    ) {
        this.camundaProcessClient = camundaProcessClient;
        this.refundApplicationService = refundApplicationService;
        this.refundApplicationMapper = refundApplicationMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RefundApplicationStartResponse start(@Valid @RequestBody RefundApplicationRequest request) {
        String firstName = request.getFirstName().trim();
        String lastName = request.getLastName().trim();
        List<String> taxTypes = normalizeTaxTypes(request.getTaxTypes());

        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", firstName);
        variables.put("lastName", lastName);
        variables.put("taxpayerName", firstName + " " + lastName);
        variables.put("taxTypes", taxTypes);

        String processInstanceId = camundaProcessClient.startProcess("refund-application", variables);

        RefundApplicationStartResponse response = new RefundApplicationStartResponse();
        response.setProcessInstanceId(processInstanceId);
        response.setProcessDefinitionKey("refund-application");
        response.setFirstName(firstName);
        response.setLastName(lastName);
        response.setTaxTypes(taxTypes);
        response.setMessage("Process started on shared Camunda. Query /api/refund-applications for DB state.");
        return response;
    }

    @GetMapping
    public List<RefundApplicationResponse> list() {
        return refundApplicationMapper.toResponseList(refundApplicationService.findAll());
    }

    @GetMapping("/{applicationId}")
    public RefundApplicationResponse get(@PathVariable("applicationId") String applicationId) {
        return refundApplicationMapper.toResponse(refundApplicationService.getByApplicationId(applicationId));
    }

    private static List<String> normalizeTaxTypes(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        return raw.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toUpperCase(Locale.ROOT))
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf
                ));
    }
}
