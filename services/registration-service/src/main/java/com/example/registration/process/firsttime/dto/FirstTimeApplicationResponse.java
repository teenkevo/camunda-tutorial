package com.example.registration.process.firsttime.dto;

import com.example.governance.annotations.Sensitive;
import com.example.governance.annotations.SensitiveType;
import com.example.governance.runtime.SensitivitySupport;
import com.example.registration.shared.domain.IdentifierType;
import java.time.Instant;
import java.util.List;
import lombok.Data;

@Data
public class FirstTimeApplicationResponse {

    private String drn;
    private String processInstanceId;
    private IdentifierType identifierType;

    @Sensitive(SensitiveType.PII)
    private String identifierValue;

    @Sensitive(SensitiveType.PII)
    private String taxpayerName;

    @Sensitive(SensitiveType.PII)
    private String email;

    @Sensitive(SensitiveType.PII)
    private String telephone;

    private List<String> taxTypes;
    private List<String> registeredTaxTypes;
    private String status;
    private Boolean identityMatched;
    private String identityAuthority;
    private String validationMessage;
    private Boolean approved;
    private Instant createdAt;
    private Instant updatedAt;

    @Override
    public String toString() {
        return SensitivitySupport.sensitiveToString(this);
    }
}
