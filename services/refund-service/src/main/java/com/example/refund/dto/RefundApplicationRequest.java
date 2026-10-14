package com.example.refund.dto;

import com.example.governance.annotations.Sensitive;
import com.example.governance.annotations.SensitiveType;
import com.example.governance.runtime.SensitivitySupport;
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
