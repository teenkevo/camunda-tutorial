package com.example.governance.runtime.catalog;

import com.example.governance.annotations.DataGovernance;
import com.example.governance.annotations.Sensitive;
import com.example.governance.annotations.SensitiveType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

@Entity
@Table(name = "sample_thing")
@DataGovernance(
        owner = "tax-ops",
        steward = "Refund Data Management",
        domain = "refunds",
        classification = SensitiveType.PII,
        retentionPolicy = "REFUND_RETENTION"
)
@Comment("Sample table")
class SampleCommentedEntity {

    @Sensitive(SensitiveType.PII)
    private String fullName;

    @Comment("Business key")
    private String code;
}
