package com.example.refund.entity;

import com.example.governance.annotations.Domain;
import com.example.governance.annotations.Sensitive;
import com.example.governance.annotations.SensitiveType;
import com.example.governance.runtime.SensitivitySupport;
import com.example.governance.runtime.audit.Audited;
import com.example.governance.runtime.audit.AuditTrailListener;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.hibernate.annotations.Comment;

@Data
@Entity
@EntityListeners(AuditTrailListener.class)
@Table(
        name = "REFUND_APPLICATION",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_REFUND_APPLICATION_DRN",
                columnNames = {"drn"}
        )
)
@Domain("refunds")
@Comment("Refund application business record, keyed by the document DRN")
public class RefundApplicationEntity implements Audited {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Surrogate primary key")
    private Long id;

    @Column(name = "REFUND_APPLICATION_AUD", unique = true, length = 36)
    @Comment("Audit id for this refund application. Look up REFUND_AUDIT.AUDIT_ID for its history")
    private String auditId;

    @Column(nullable = false, length = 64)
    @Comment("Document reference number created for this refund")
    private String drn;

    @Comment("Camunda process instance id that owns this application")
    private String processInstanceId;

    @Sensitive(SensitiveType.PII)
    @Comment("Taxpayer given name")
    private String firstName;

    @Sensitive(SensitiveType.PII)
    @Comment("Taxpayer family name")
    private String lastName;

    @ElementCollection
    @CollectionTable(
            name = "REFUND_APPLICATION_TAX_TYPE",
            joinColumns = @JoinColumn(name = "refund_application_id")
    )
    @Column(name = "tax_type", nullable = false, length = 32)
    @Comment("Tax type claimed on the refund")
    private List<String> taxTypes = new ArrayList<>();

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
