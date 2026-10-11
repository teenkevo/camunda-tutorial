package com.example.taxpayer.entity;

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

@Data
@Entity
@Table(name = "taxpayer_registration")
@DataOwner("tax-ops")
@DataDomain("taxpayer-registration")
public class TaxpayerRegistrationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String processInstanceId;

    @Sensitive(SensitiveType.PII)
    private String taxpayerName;

    @Sensitive(SensitiveType.PII)
    private String nationalId;

    private String tin;
    private String status;
    private Instant createdAt;

    @Override
    public String toString() {
        return SensitivitySupport.sensitiveToString(this);
    }
}
