package com.example.governance.runtime.catalog;

import com.example.governance.annotations.Domain;
import com.example.governance.annotations.Sensitive;
import com.example.governance.annotations.SensitiveType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

@Entity
@Table(name = "sample_thing")
@Domain("refunds")
@Comment("Sample table")
class SampleCommentedEntity {

    @Sensitive(SensitiveType.PII)
    private String fullName;

    @Comment("Business key")
    private String code;
}
