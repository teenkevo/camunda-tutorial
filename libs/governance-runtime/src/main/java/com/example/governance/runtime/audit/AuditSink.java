package com.example.governance.runtime.audit;

import java.util.List;

/** Writes audit history into the schema owned by the current service. */
public interface AuditSink {

    /** Physical history table, for example {@code DOCUMENT_AUDIT}. */
    String auditTable();

    void append(List<AuditEvent> events);
}
