package com.example.taxpayer.dto;

import com.example.governance.annotations.Sensitive;
import com.example.governance.annotations.SensitiveType;
import com.example.governance.runtime.SensitivitySupport;
import java.util.List;
import lombok.Data;

@Data
public class TaxpayerRegistrationRequest {

    @Sensitive(SensitiveType.PII)
    private String taxpayerName;

    @Sensitive(SensitiveType.PII)
    private String nationalId;

    private List<String> taxTypes;

    @Override
    public String toString() {
        return SensitivitySupport.sensitiveToString(this);
    }
}
