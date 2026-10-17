package com.example.registration.process.firsttime.dto;

import com.example.governance.annotations.Sensitive;
import com.example.governance.annotations.SensitiveType;
import com.example.governance.runtime.SensitivitySupport;
import com.example.registration.shared.domain.IdentifierType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FirstTimeIdentityRequest {

    @NotNull
    private IdentifierType identifierType;

    @NotBlank
    @Size(min = 5, max = 64)
    @Sensitive(SensitiveType.PII)
    private String identifierValue;

    /** Date of birth for NIN, or date of incorporation for ERN. */
    @NotBlank
    @Sensitive(SensitiveType.PII)
    private String dateOfBirthOrIncorp;

    @Override
    public String toString() {
        return SensitivitySupport.sensitiveToString(this);
    }
}
