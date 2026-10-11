package com.example.camunda.dto;

import java.util.Map;
import lombok.Data;

@Data
public class StartProcessRequest {
    private String processKey;
    private Map<String, Object> variables;
}
