package com.example.camunda.controller;

import com.example.camunda.dto.StartProcessRequest;
import com.example.camunda.dto.StartProcessResponse;
import java.util.Map;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/process-instances")
public class ProcessInstanceController {

    private final RuntimeService runtimeService;

    public ProcessInstanceController(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StartProcessResponse start(@RequestBody StartProcessRequest request) {
        if (request.getProcessKey() == null || request.getProcessKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "processKey is required");
        }
        Map<String, Object> variables = request.getVariables() == null ? Map.of() : request.getVariables();
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(request.getProcessKey(), variables);

        StartProcessResponse response = new StartProcessResponse();
        response.setProcessInstanceId(instance.getId());
        response.setProcessDefinitionKey(request.getProcessKey());
        return response;
    }
}
