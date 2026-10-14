package com.example.refund.controller;

import com.example.camunda.client.CamundaProcessClient;
import com.example.refund.dto.RefundApplicationRequest;
import com.example.refund.dto.RefundApplicationResponse;
import com.example.refund.dto.RefundApplicationStartResponse;
import com.example.refund.mapper.RefundApplicationMapper;
import com.example.refund.service.RefundApplicationService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
    public RefundApplicationStartResponse start(@RequestBody RefundApplicationRequest request) {
        if (request.getFirstName() == null || request.getFirstName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "firstName is required");
        }
        if (request.getLastName() == null || request.getLastName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lastName is required");
        }

        String firstName = request.getFirstName().trim();
        String lastName = request.getLastName().trim();

        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", firstName);
        variables.put("lastName", lastName);
        variables.put("taxpayerName", firstName + " " + lastName);

        String processInstanceId = camundaProcessClient.startProcess("refund-application", variables);

        RefundApplicationStartResponse response = new RefundApplicationStartResponse();
        response.setProcessInstanceId(processInstanceId);
        response.setProcessDefinitionKey("refund-application");
        response.setFirstName(firstName);
        response.setLastName(lastName);
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
}
