package com.example.refund.entity;

import com.example.governance.annotations.DataDomain;
import com.example.governance.annotations.DataOwner;
import com.example.governance.annotations.Sensitive;
import com.example.governance.annotations.SensitiveType;
import com.example.governance.runtime.SensitivitySupport;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;
import org.hibernate.annotations.Comment;

@Data
@Entity
@Table(name = "refund_application")
@DataOwner("tax-ops")
@DataDomain("refunds")
@Comment("Refund application business record")
public class RefundApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
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
    @Column(length = 1000)
    private String notificationMessage;

    private String decision;
    private Instant createdAt;
    private Instant updatedAt;

    @Override
    public String toString() {
        return SensitivitySupport.sensitiveToString(this);
    }
}
