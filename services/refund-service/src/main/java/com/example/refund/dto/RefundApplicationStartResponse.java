package com.example.refund.dto;

import com.example.governance.annotations.Sensitive;
import com.example.governance.annotations.SensitiveType;
import com.example.governance.runtime.SensitivitySupport;
import lombok.Data;

@Data
public class RefundApplicationStartResponse {

    private String processInstanceId;
    private String processDefinitionKey;

    @Sensitive(SensitiveType.PII)
    private String firstName;

    @Sensitive(SensitiveType.PII)
    private String lastName;

    private String message;

    @Override
    public String toString() {
        return SensitivitySupport.sensitiveToString(this);
    }
}
