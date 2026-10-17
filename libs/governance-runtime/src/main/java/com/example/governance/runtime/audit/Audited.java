package com.example.governance.runtime.audit;

/**
 * A persisted row that carries a stable audit id.
 * The id is stored in a table-specific column such as {@code DOCUMENT_AUD} or {@code TAXPAYER_AUD}
 * and is the key for that row's history.
 */
public interface Audited {

    String getAuditId();

    void setAuditId(String auditId);
}
