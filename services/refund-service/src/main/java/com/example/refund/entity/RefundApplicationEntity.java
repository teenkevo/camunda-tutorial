package com.example.refund.entity;

import com.example.governance.annotations.Domain;
import com.example.governance.annotations.Sensitive;
import com.example.governance.annotations.SensitiveType;
import com.example.governance.runtime.SensitivitySupport;
import com.example.refund.persistence.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.hibernate.annotations.Comment;

@Data
@Entity
@Table(name = "RFND_APPL")
@Domain("refunds")
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

    @Convert(converter = StringListConverter.class)
    @Column(name = "tax_types", length = 120)
    @Comment("Tax types claimed on the refund (VAT, PAYE, CIT, WHT)")
    private List<String> taxTypes = new ArrayList<>();

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
