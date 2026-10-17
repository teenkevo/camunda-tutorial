package com.example.document.audit;

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
        name = "DOCUMENT_AUDIT",
        indexes = @Index(name = "IX_DOCUMENT_AUDIT_AUDIT_ID", columnList = "AUDIT_ID")
)
@Domain("documents")
@Comment("History of a document or document event, keyed by its audit id")
public class DocumentAuditEntity extends AuditRecord {
}
