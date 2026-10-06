package com.example.camundatutorial.refund;

import com.example.camundatutorial.governance.Sensitive;
import com.example.camundatutorial.governance.SensitiveType;
import lombok.Data;

@Data
public class RefundApplicationRequest {

    @Sensitive(SensitiveType.PII)
    private String firstName;

    @Sensitive(SensitiveType.PII)
    private String lastName;
}
