package com.example.refund.audit;

import com.example.governance.runtime.audit.AuditEntries;
import com.example.governance.runtime.audit.AuditEntryResponse;
import com.example.governance.runtime.audit.AuditEvent;
import com.example.governance.runtime.audit.AuditLookup;
import com.example.governance.runtime.audit.AuditSink;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RefundAuditStore implements AuditSink, AuditLookup {

    private final RefundAuditRepository repository;

    public RefundAuditStore(RefundAuditRepository repository) {
        this.repository = repository;
    }

    @Override
    public String auditTable() {
        return "REFUND_AUDIT";
    }

    @Override
    public void append(List<AuditEvent> events) {
        for (AuditEvent event : events) {
            RefundAuditEntity row = new RefundAuditEntity();
            row.apply(event);
            repository.save(row);
        }
    }

    @Override
    public List<AuditEntryResponse> find(String auditId) {
        return repository.findByAuditIdOrderByOccurredAtAscIdAsc(auditId).stream()
                .map(AuditEntries::toResponse)
                .toList();
    }
}
