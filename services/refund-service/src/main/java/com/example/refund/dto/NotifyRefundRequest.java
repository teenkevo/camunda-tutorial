package com.example.refund.dto;

import com.example.governance.annotations.Sensitive;
import com.example.governance.annotations.SensitiveType;
import lombok.Data;

@Data
public class NotifyRefundRequest {

    @Sensitive(SensitiveType.PII)
    private String firstName;

    @Sensitive(SensitiveType.PII)
    private String lastName;

    private String drn;

    private String station;
    private boolean approved;

    @Sensitive(SensitiveType.PII)
    private String reviewComment;
}
