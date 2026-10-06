package com.example.camundatutorial.refund;

import com.example.camundatutorial.governance.Sensitive;
import com.example.camundatutorial.governance.SensitiveType;
import com.example.camundatutorial.governance.SensitivitySupport;
import java.time.Instant;
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
