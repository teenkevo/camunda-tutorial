package com.example.camundatutorial.taxpayer;

import com.example.camundatutorial.governance.Sensitive;
import com.example.camundatutorial.governance.SensitiveType;
import com.example.camundatutorial.governance.SensitivitySupport;
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
