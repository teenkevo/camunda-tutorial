package com.example.refund.audit;

import com.example.governance.annotations.Domain;
import com.example.governance.runtime.audit.AuditRecord;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

@Getter
@Setter
@Entity
@Table(
        name = "REFUND_AUDIT",
        indexes = @Index(name = "IX_REFUND_AUDIT_AUDIT_ID", columnList = "AUDIT_ID")
)
@Domain("refunds")
@Comment("History of a refund application, keyed by its audit id")
public class RefundAuditEntity extends AuditRecord {
}
