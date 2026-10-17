package com.example.registration.process.firsttime.dto;

import com.example.governance.annotations.Sensitive;
import com.example.governance.annotations.SensitiveType;
import com.example.governance.runtime.SensitivitySupport;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class IndividualIdentityRequest {

    @Size(min = 5, max = 64)
    @Sensitive(SensitiveType.PII)
    private String nin;

    @Size(min = 5, max = 64)
    @Sensitive(SensitiveType.PII)
    private String ain;

    @NotBlank
    @Sensitive(SensitiveType.PII)
    private String dateOfBirth;

    @Override
    public String toString() {
        return SensitivitySupport.sensitiveToString(this);
    }
}
