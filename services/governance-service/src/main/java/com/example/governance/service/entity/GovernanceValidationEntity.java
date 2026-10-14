package com.example.governance.service.entity;

import com.example.governance.annotations.GovernedAsset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Data;
import org.hibernate.annotations.Comment;

/**
 * Catalog row for a Bean Validation constraint discovered from {@code @Valid}
 * request DTOs. Stable id example:
 * {@code refund-service.RefundApplicationRequest.firstName.NotBlank}.
 */
@Data
@Entity
@Table(
        name = "GOV_VALIDATION",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_gov_validation_id",
                columnNames = {"validation_id"}
        )
)
@GovernedAsset("governance.validation")
@Comment("Catalog of Bean Validation constraints sniffed from @Valid request DTOs")
public class GovernanceValidationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Surrogate primary key")
    private Long id;

    @Column(name = "validation_id", nullable = false, length = 512)
    @Comment("Stable id: {service}.{DtoSimpleName}.{propertyPath}.{ConstraintSimpleName}")
    private String validationId;

    @Column(name = "service_name", nullable = false, length = 120)
    @Comment("Domain microservice that published this validation")
    private String serviceName;

    @Column(name = "dto_type", length = 255)
    @Comment("Fully qualified request DTO class that declares the constraint")
    private String dtoType;

    @Column(name = "field_name", length = 255)
    @Comment("Validated property path on the DTO")
    private String fieldName;

    @Column(name = "constraint_name", length = 120)
    @Comment("Simple constraint annotation name, e.g. NotBlank")
    private String constraintName;

    @Column(name = "constraint_type", length = 255)
    @Comment("Fully qualified constraint annotation type")
    private String constraintType;

    @Column(length = 1000)
    @Comment("Constraint message template or configured message")
    private String message;

    @Column(name = "controller_type", length = 255)
    @Comment("Controller class that exposes the @Valid request body")
    private String controllerType;

    @Column(name = "controller_method", length = 120)
    @Comment("Controller handler method name")
    private String controllerMethod;

    @Column(name = "http_method", length = 16)
    @Comment("HTTP method of the validated endpoint")
    private String httpMethod;

    @Column(name = "request_path", length = 512)
    @Comment("HTTP path of the validated endpoint")
    private String requestPath;

    @Column(name = "last_seen_at", nullable = false)
    @Comment("When this validation was last published by a domain service sync")
    private Instant lastSeenAt = Instant.now();
}
