package com.example.camunda.dto;

import lombok.Data;

@Data
public class StartProcessResponse {
    private String processInstanceId;
    private String processDefinitionKey;
}
