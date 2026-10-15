package com.example.taxpayer.entity;

import com.example.governance.annotations.Domain;
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
@Table(name = "REG_TAX_PAYER_MST")
@Domain("taxpayer-registration")
@Comment("Taxpayer registration business record")
public class TaxpayerRegistrationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Surrogate primary key")
    private Long id;

    @Column(nullable = false, unique = true)
    @Comment("Camunda process instance id for this registration")
    private String processInstanceId;

    @Sensitive(SensitiveType.PII)
    @Comment("Registered taxpayer display name")
    private String taxpayerName;

    @Sensitive(SensitiveType.PII)
    @Comment("National identity number of the taxpayer")
    private String nationalId;

    @Comment("Tax identification number issued on completion")
    private String tin;

    @Comment("Lifecycle status of the registration")
    private String status;

    @Comment("When the registration record was created")
    private Instant createdAt;

    @Override
    public String toString() {
        return SensitivitySupport.sensitiveToString(this);
    }
}
