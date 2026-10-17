package com.example.governance.runtime.audit;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

/**
 * One history row for an audited business record.
 * Concrete services map this to {@code DOCUMENT_AUDIT}, {@code REGISTRATION_AUDIT},
 * {@code REFUND_AUDIT}, or {@code GOVERNANCE_AUDIT}.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Surrogate primary key")
    private Long id;

    @Column(name = "AUDIT_ID", nullable = false, length = 36)
    @Comment("Audit id copied from the business row, for example DOCUMENT_AUD or TAXPAYER_AUD")
    private String auditId;

    @Column(name = "TABLE_NAME", nullable = false, length = 128)
    @Comment("Business table this history row describes")
    private String tableName;

    @Column(name = "ROW_ID", length = 64)
    @Comment("Primary key of the business row at the time of the change")
    private String rowId;

    @Column(name = "ACTION", nullable = false, length = 16)
    @Comment("CREATED, UPDATED, or BASELINE")
    private String action;

    @Lob
    @Column(name = "SNAPSHOT", nullable = false)
    @Comment("JSON snapshot of the business row after the change")
    private String snapshot;

    @Column(name = "OCCURRED_AT", nullable = false)
    @Comment("When this history row was recorded")
    private Instant occurredAt;

    public void apply(AuditEvent event) {
        setAuditId(event.auditId());
        setTableName(event.tableName());
        setRowId(event.rowId());
        setAction(event.action());
        setSnapshot(event.snapshot());
        setOccurredAt(event.occurredAt());
    }
}
