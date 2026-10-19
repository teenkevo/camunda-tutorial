package com.example.registration.process.firsttime.dto;

import com.example.governance.annotations.Sensitive;
import com.example.governance.annotations.SensitiveType;
import com.example.governance.runtime.SensitivitySupport;
import com.example.registration.shared.domain.IdentifierType;
import lombok.Data;

@Data
public class FirstTimeIdentityResponse {

    private IdentifierType identifierType;

    @Sensitive(SensitiveType.PII)
    private String identifierValue;

    @Sensitive(SensitiveType.PII)
    private String dateOfBirthOrIncorp;

    private Boolean identityMatched;
    private String identityAuthority;

    @Sensitive(SensitiveType.PII)
    private String taxpayerName;

    @Sensitive(SensitiveType.PII)
    private String email;

    @Sensitive(SensitiveType.PII)
    private String telephone;

    private boolean emailFromAuthority;
    private boolean telephoneFromAuthority;
    private boolean contactsRequired;
    private boolean otpSent;
    private String message;

    @Override
    public String toString() {
        return SensitivitySupport.sensitiveToString(this);
    }
}
