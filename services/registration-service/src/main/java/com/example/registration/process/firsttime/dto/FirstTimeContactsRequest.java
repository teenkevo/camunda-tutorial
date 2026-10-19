package com.example.registration.process.firsttime.dto;

import com.example.governance.annotations.Sensitive;
import com.example.governance.annotations.SensitiveType;
import com.example.governance.runtime.SensitivitySupport;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FirstTimeContactsRequest extends FirstTimeIdentityRequest {

    @Sensitive(SensitiveType.PII)
    private String email;

    @Sensitive(SensitiveType.PII)
    private String telephone;

    @Override
    public String toString() {
        return SensitivitySupport.sensitiveToString(this);
    }
}
