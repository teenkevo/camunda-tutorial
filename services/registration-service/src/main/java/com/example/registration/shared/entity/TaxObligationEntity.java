package com.example.registration.shared.entity;

import com.example.governance.annotations.Domain;
import com.example.governance.runtime.SensitivitySupport;
import com.example.governance.runtime.audit.Audited;
import com.example.governance.runtime.audit.AuditTrailListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;
import org.hibernate.annotations.Comment;

@Data
@Entity
@EntityListeners(AuditTrailListener.class)
@Table(name = "REGISTRATION_TAX_OBLIGATION")
@Domain("taxpayer-registration")
@Comment("Tax type obligation registered against a taxpayer")
public class TaxObligationEntity implements Audited {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Surrogate primary key")
    private Long id;

    @Column(name = "TAX_OBLIGATION_AUD", unique = true, length = 36)
    @Comment("Audit id for this obligation. Look up REGISTRATION_AUDIT.AUDIT_ID for its history")
    private String auditId;

    @Column(nullable = false)
    @Comment("Foreign key to REGISTRATION_TAXPAYER.id")
    private Long taxpayerId;

    @Column(nullable = false, length = 32)
    @Comment("Tax type code (INCOME_TAX, VAT, PAYE, WHT, …)")
    private String taxType;

    @Column(nullable = false, length = 24)
    @Comment("Obligation status (ACTIVE, INACTIVE, SUSPENDED)")
    private String status;

    @Column(length = 128)
    @Comment("Registration reference for this tax type")
    private String registrationRef;

    @Comment("When the obligation was created")
    private Instant createdAt;

    @Override
    public String toString() {
        return SensitivitySupport.sensitiveToString(this);
    }
}
