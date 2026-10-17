package com.example.registration.shared.entity;

import com.example.governance.annotations.Domain;
import com.example.governance.annotations.Sensitive;
import com.example.governance.annotations.SensitiveType;
import com.example.governance.runtime.SensitivitySupport;
import com.example.registration.shared.domain.IdentifierType;
import com.example.registration.shared.domain.LifecycleState;
import com.example.registration.shared.domain.TaxpayerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Data;
import org.hibernate.annotations.Comment;

@Data
@Entity
@Table(
        name = "REGISTRATION_TAXPAYER",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_REGISTRATION_TAXPAYER_IDENTIFIER",
                columnNames = {"identifier_type", "identifier_value"}
        )
)
@Domain("taxpayer-registration")
@Comment("Registered taxpayer master record")
public class TaxpayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Surrogate primary key")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "taxpayer_type", nullable = false, length = 24)
    @Comment("INDIVIDUAL or NON_INDIVIDUAL")
    private TaxpayerType taxpayerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "identifier_type", nullable = false, length = 16)
    @Comment("Primary identifier type (NIN, ERN, AIN, SYS)")
    private IdentifierType identifierType;

    @Sensitive(SensitiveType.PII)
    @Column(name = "identifier_value", nullable = false, length = 64)
    @Comment("Primary identifier value issued by NIRA, URSB, or eTax2")
    private String identifierValue;

    @Sensitive(SensitiveType.PII)
    @Column(nullable = false)
    @Comment("Registered taxpayer display name")
    private String displayName;

    @Sensitive(SensitiveType.PII)
    @Comment("Contact email address")
    private String email;

    @Sensitive(SensitiveType.PII)
    @Comment("Contact telephone number")
    private String telephone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    @Comment("Taxpayer lifecycle state")
    private LifecycleState lifecycleState;

    @Comment("When the taxpayer record was created")
    private Instant createdAt;

    @Comment("When the taxpayer record was last updated")
    private Instant updatedAt;

    @Override
    public String toString() {
        return SensitivitySupport.sensitiveToString(this);
    }
}
