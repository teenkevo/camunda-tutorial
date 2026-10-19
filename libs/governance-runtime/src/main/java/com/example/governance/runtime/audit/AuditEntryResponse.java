package com.example.governance.runtime.audit;

import java.time.Instant;
import lombok.Data;

@Data
public class AuditEntryResponse {

    private Long id;
    private String auditId;
    private String tableName;
    private String rowId;
    private String action;
    private Object snapshot;
    private Instant occurredAt;
}
