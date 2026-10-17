package com.example.governance.service.audit;

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
        name = "GOVERNANCE_AUDIT",
        indexes = @Index(name = "IX_GOVERNANCE_AUDIT_AUDIT_ID", columnList = "AUDIT_ID")
)
@Domain("governance")
@Comment("History of a governance catalog row, keyed by its audit id")
public class GovernanceAuditEntity extends AuditRecord {
}
