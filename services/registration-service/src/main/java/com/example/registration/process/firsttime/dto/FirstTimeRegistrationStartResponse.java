package com.example.registration.process.firsttime.dto;

import com.example.governance.annotations.Sensitive;
import com.example.governance.annotations.SensitiveType;
import com.example.governance.runtime.SensitivitySupport;
import com.example.registration.shared.domain.IdentifierType;
import java.util.List;
import lombok.Data;

@Data
public class FirstTimeRegistrationStartResponse {

    private String drn;
    private String processInstanceId;
    private String processDefinitionKey;
    private IdentifierType identifierType;

    @Sensitive(SensitiveType.PII)
    private String identifierValue;

    @Sensitive(SensitiveType.PII)
    private String taxpayerName;

    private List<String> taxTypes;
    private String message;

    @Override
    public String toString() {
        return SensitivitySupport.sensitiveToString(this);
    }
}
