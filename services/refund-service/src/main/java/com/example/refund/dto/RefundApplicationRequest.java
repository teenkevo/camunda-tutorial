package com.example.refund.dto;

import com.example.governance.annotations.Sensitive;
import com.example.governance.annotations.SensitiveType;
import com.example.governance.runtime.SensitivitySupport;
import com.example.refund.validation.SupportedTaxTypes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class RefundApplicationRequest {

    @NotBlank(message = "firstName is required")
    @Sensitive(SensitiveType.PII)
    private String firstName;

    @NotBlank(message = "lastName is required")
    @Sensitive(SensitiveType.PII)
    private String lastName;

    @NotEmpty(message = "At least one tax type is required")
    @SupportedTaxTypes
    private List<String> taxTypes;

    @Override
    public String toString() {
        return SensitivitySupport.sensitiveToString(this);
    }
}
