package com.example.refund.dto;

import com.example.governance.annotations.Sensitive;
import com.example.governance.annotations.SensitiveType;
import com.example.governance.runtime.SensitivitySupport;
import java.time.Instant;
import java.util.List;
import lombok.Data;

@Data
public class RefundApplicationResponse {

    private Long id;
    private String applicationId;
    private String processInstanceId;

    @Sensitive(SensitiveType.PII)
    private String firstName;

    @Sensitive(SensitiveType.PII)
    private String lastName;

    private List<String> taxTypes;

    private String drn;
    private String station;
    private String riskLevel;
    private Boolean approved;
    private String status;

    @Sensitive(SensitiveType.PII)
    private String reviewComment;

    @Sensitive(SensitiveType.PII)
    private String notificationMessage;

    private String decision;
    private Instant createdAt;
    private Instant updatedAt;

    @Override
    public String toString() {
        return SensitivitySupport.sensitiveToString(this);
    }
}
