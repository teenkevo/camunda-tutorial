package com.example.refund.entity;

import com.example.governance.annotations.GovernedAsset;
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
@Table(name = "RFND_APPL")
@GovernedAsset("refunds.application")
@Comment("Refund application business record")
public class RefundApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Surrogate primary key")
    private Long id;

    @Column(nullable = false, unique = true)
    @Comment("Business identifier for the refund application")
    private String applicationId;

    @Comment("Camunda process instance id that owns this application")
    private String processInstanceId;

    @Sensitive(SensitiveType.PII)
    @Comment("Taxpayer given name")
    private String firstName;

    @Sensitive(SensitiveType.PII)
    @Comment("Taxpayer family name")
    private String lastName;

    @Comment("Document reference number assigned during processing")
    private String drn;

    @Comment("Tax station assigned to handle the application")
    private String station;

    @Comment("Computed taxpayer risk level (low, medium, high)")
    private String riskLevel;

    @Comment("Whether the refund was approved")
    private Boolean approved;

    @Comment("Lifecycle status of the application")
    private String status;

    @Sensitive(SensitiveType.PII)
    @Comment("Reviewer or auto-approval comment")
    private String reviewComment;

    @Sensitive(SensitiveType.PII)
    @Column(length = 1000)
    @Comment("Notification message sent to the taxpayer")
    private String notificationMessage;

    @Comment("Final decision outcome (APPROVED or REJECTED)")
    private String decision;

    @Comment("When the application record was created")
    private Instant createdAt;

    @Comment("When the application record was last updated")
    private Instant updatedAt;

    @Override
    public String toString() {
        return SensitivitySupport.sensitiveToString(this);
    }
}
