package com.example.governance.service.dto;

import lombok.Data;

@Data
public class GovernanceValidationDto {

    private String serviceName;
    private String validationId;
    private String dtoType;
    private String fieldName;
    private String constraintName;
    private String constraintType;
    private String message;
    private String controllerType;
    private String controllerMethod;
    private String httpMethod;
    private String requestPath;
}
