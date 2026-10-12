package com.example.governance.runtime.catalog;

import com.example.governance.annotations.DataDomain;
import com.example.governance.annotations.DataOwner;
import com.example.governance.annotations.Sensitive;
import com.example.governance.annotations.SensitiveType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

@Entity
@Table(name = "sample_thing")
@DataOwner("tax-ops")
@DataDomain("refunds")
@Comment("Sample table")
class SampleCommentedEntity {

    @Sensitive(SensitiveType.PII)
    private String fullName;
}
