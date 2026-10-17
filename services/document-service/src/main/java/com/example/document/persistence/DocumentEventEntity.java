package com.example.document.persistence;

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
@Table(name = "DOCUMENT_EVENT")
@Domain("documents")
@Comment("Lifecycle and audit event for a document")
public class DocumentEventEntity implements Audited {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Surrogate primary key")
    private Long id;

    @Column(name = "DOCUMENT_EVENT_AUD", unique = true, length = 36)
    @Comment("Audit id for this event row. Look up DOCUMENT_AUDIT.AUDIT_ID for its history")
    private String auditId;

    @Column(nullable = false)
    @Comment("Document id this event belongs to")
    private Long documentId;

    @Column(nullable = false, length = 64)
    @Comment("Document reference number")
    private String drn;

    @Column(nullable = false, length = 64)
    @Comment("Event type (CREATED, PAYLOAD_UPDATED, STATUS_CHANGED, APPROVED, REJECTED, COMPLETED)")
    private String eventType;

    @Column(length = 32)
    @Comment("Status before the event")
    private String fromStatus;

    @Column(length = 32)
    @Comment("Status after the event")
    private String toStatus;

    @Column(length = 500)
    @Comment("Optional event detail")
    private String message;

    @Comment("When the event was recorded")
    private Instant createdAt;

    @Override
    public String toString() {
        return SensitivitySupport.sensitiveToString(this);
    }
}
