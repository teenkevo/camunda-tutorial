package com.example.governance.runtime.catalog;

import lombok.Data;

/**
 * Discovered Bean Validation constraint. Stable id format:
 * {@code {service}.{DtoSimpleName}.{propertyPath}.{ConstraintSimpleName}}
 * e.g. {@code refund-service.RefundApplicationRequest.firstName.NotBlank}.
 */
@Data
public class GovernanceValidationDto {

    private String serviceName;
    private String validationId;
    /** Fully qualified DTO / request type. */
    private String dtoType;
    private String fieldName;
    /** Simple constraint name, e.g. {@code NotBlank}. */
    private String constraintName;
    /** Fully qualified constraint annotation type. */
    private String constraintType;
    private String message;
    private String controllerType;
    private String controllerMethod;
    private String httpMethod;
    private String requestPath;
}
