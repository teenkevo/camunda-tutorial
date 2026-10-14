package com.example.refund.dto;

import com.example.governance.annotations.Sensitive;
import com.example.governance.annotations.SensitiveType;
import java.util.List;
import lombok.Data;

@Data
public class CreateRefundProcessRequest {
    private String processInstanceId;

    @Sensitive(SensitiveType.PII)
    private String firstName;

    @Sensitive(SensitiveType.PII)
    private String lastName;

    private List<String> taxTypes;
}
