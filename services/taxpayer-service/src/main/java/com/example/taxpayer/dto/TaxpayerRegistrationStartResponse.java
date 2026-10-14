package com.example.taxpayer.dto;

import com.example.governance.annotations.Sensitive;
import com.example.governance.annotations.SensitiveType;
import com.example.governance.runtime.SensitivitySupport;
import java.util.List;
import lombok.Data;

@Data
public class TaxpayerRegistrationStartResponse {

    private String processInstanceId;
    private String processDefinitionKey;

    @Sensitive(SensitiveType.PII)
    private String taxpayerName;

    private List<String> taxTypes;

    @Override
    public String toString() {
        return SensitivitySupport.sensitiveToString(this);
    }
}
