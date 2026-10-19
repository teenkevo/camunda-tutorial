package com.example.governance.runtime.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

class GovernanceRequestValidationScannerTest {

    private final GovernanceRequestValidationScanner scanner = new GovernanceRequestValidationScanner();

    @Test
    void scan_discoversBuiltInAndCustomConstraintsOnValidRequestBody() {
        List<GovernanceValidationDto> found = scanner.scan(
                "refund-service",
                Set.of(SampleRefundController.class.getPackageName())
        );

        assertThat(found)
                .extracting(GovernanceValidationDto::getValidationId)
                .contains(
                        "refund-service.SampleRefundRequest.firstName.NotBlank",
                        "refund-service.SampleRefundRequest.lastName.NotBlank",
                        "refund-service.SampleRefundRequest.taxTypes.NotEmpty",
                        "refund-service.SampleRefundRequest.taxTypes.SampleSupportedTaxTypes"
                );

        assertThat(found)
                .filteredOn(v -> "refund-service.SampleRefundRequest.firstName.NotBlank".equals(v.getValidationId()))
                .first()
                .satisfies(v -> {
                    assertThat(v.getFieldName()).isEqualTo("firstName");
                    assertThat(v.getConstraintName()).isEqualTo("NotBlank");
                    assertThat(v.getHttpMethod()).isEqualTo("POST");
                    assertThat(v.getRequestPath()).isEqualTo("/api/sample-refunds");
                    assertThat(v.getControllerMethod()).isEqualTo("start");
                    assertThat(v.getMessage()).contains("firstName is required");
                });
    }

    @Documented
    @Constraint(validatedBy = SampleSupportedTaxTypesValidator.class)
    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface SampleSupportedTaxTypes {
        String message() default "unsupported tax type";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    static class SampleSupportedTaxTypesValidator
            implements ConstraintValidator<SampleSupportedTaxTypes, Collection<String>> {
        @Override
        public boolean isValid(Collection<String> value, ConstraintValidatorContext context) {
            return true;
        }
    }

    @Data
    static class SampleRefundRequest {
        @NotBlank(message = "firstName is required")
        private String firstName;

        @NotBlank(message = "lastName is required")
        private String lastName;

        @NotEmpty
        @SampleSupportedTaxTypes
        private List<String> taxTypes;
    }

    @RestController
    @RequestMapping("/api/sample-refunds")
    static class SampleRefundController {

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public void start(@Valid @RequestBody SampleRefundRequest request) {
        }
    }
}
