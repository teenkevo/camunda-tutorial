package com.example.governance.runtime.audit;

import java.time.Instant;

/** One change to an audited row, waiting to be written in the same transaction. */
public record AuditEvent(
        String auditId,
        String tableName,
        String rowId,
        String action,
        String snapshot,
        Instant occurredAt
) {
}
