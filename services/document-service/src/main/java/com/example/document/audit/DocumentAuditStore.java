package com.example.document.audit;

import com.example.governance.runtime.audit.AuditEntries;
import com.example.governance.runtime.audit.AuditEntryResponse;
import com.example.governance.runtime.audit.AuditEvent;
import com.example.governance.runtime.audit.AuditLookup;
import com.example.governance.runtime.audit.AuditSink;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DocumentAuditStore implements AuditSink, AuditLookup {

    private final DocumentAuditRepository repository;

    public DocumentAuditStore(DocumentAuditRepository repository) {
        this.repository = repository;
    }

    @Override
    public String auditTable() {
        return "DOCUMENT_AUDIT";
    }

    @Override
    public void append(List<AuditEvent> events) {
        for (AuditEvent event : events) {
            DocumentAuditEntity row = new DocumentAuditEntity();
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
