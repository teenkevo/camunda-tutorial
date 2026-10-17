package com.example.document.persistence;

import com.example.governance.annotations.Domain;
import com.example.governance.runtime.SensitivitySupport;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Data;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Table(name = "DOCUMENT_RECORD")
@Domain("documents")
@Comment("Application document with a DRN and a JSON payload")
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Surrogate primary key")
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    @Comment("Document reference number used by every process step")
    private String drn;

    @Column(nullable = false, length = 64)
    @Comment("Document type (INDIVIDUAL_REGISTRATION, NON_INDIVIDUAL_REGISTRATION, REFUND_APPLICATION, ...)")
    private String documentType;

    @Column(nullable = false)
    @Comment("Payload schema version")
    private Integer schemaVersion;

    @Column(nullable = false, length = 32)
    @Comment("Document lifecycle status")
    private String status;

    @Comment("Taxpayer master id after an approved registration")
    private Long taxpayerId;

    @Column(length = 64)
    @Comment("Camunda process instance that owns this document")
    private String processInstanceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    @Comment("Document-type JSON payload")
    private String payload;

    @Column(length = 128)
    @Comment("Actor that created the document")
    private String createdBy;

    @Comment("When the document was created")
    private Instant createdAt;

    @Comment("When the document was last updated")
    private Instant updatedAt;

    @Comment("When the document was submitted into a process")
    private Instant submittedAt;

    @Version
    @Comment("Optimistic lock version")
    private Long versionNo;

    @Override
    public String toString() {
        return SensitivitySupport.sensitiveToString(this);
    }
}
