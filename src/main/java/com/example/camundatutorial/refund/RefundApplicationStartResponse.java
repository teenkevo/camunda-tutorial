package com.example.camundatutorial.refund;

import com.example.camundatutorial.governance.Sensitive;
import com.example.camundatutorial.governance.SensitiveType;
import com.example.camundatutorial.governance.SensitivitySupport;
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
