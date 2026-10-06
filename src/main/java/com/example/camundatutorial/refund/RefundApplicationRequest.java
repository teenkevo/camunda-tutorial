package com.example.camundatutorial.refund;

import com.example.camundatutorial.governance.Sensitive;
import com.example.camundatutorial.governance.SensitiveType;
import com.example.camundatutorial.governance.SensitivitySupport;
import lombok.Data;

@Data
public class RefundApplicationRequest {

    @Sensitive(SensitiveType.PII)
    private String firstName;

    @Sensitive(SensitiveType.PII)
    private String lastName;

    @Override
    public String toString() {
        return SensitivitySupport.sensitiveToString(this);
    }
}
