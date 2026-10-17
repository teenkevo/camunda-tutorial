package com.example.registration.audit;

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
        name = "REGISTRATION_AUDIT",
        indexes = @Index(name = "IX_REGISTRATION_AUDIT_AUDIT_ID", columnList = "AUDIT_ID")
)
@Domain("taxpayer-registration")
@Comment("History of a taxpayer or tax obligation, keyed by its audit id")
public class RegistrationAuditEntity extends AuditRecord {
}
