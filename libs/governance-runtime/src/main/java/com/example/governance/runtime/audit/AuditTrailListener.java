package com.example.governance.runtime.audit;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import java.util.UUID;

/**
 * Assigns a stable audit id and appends a history row for each insert and update.
 * The history row is written in the same transaction, after the business flush.
 */
public class AuditTrailListener {

    @PrePersist
    @PreUpdate
    public void assignId(Object entity) {
        if (entity instanceof Audited audited && (audited.getAuditId() == null || audited.getAuditId().isBlank())) {
            audited.setAuditId(UUID.randomUUID().toString());
        }
    }

    @PostPersist
    public void created(Object entity) {
        AuditTrailSupport.record(entity, "CREATED");
    }

    @PostUpdate
    public void updated(Object entity) {
        AuditTrailSupport.record(entity, "UPDATED");
    }
}
