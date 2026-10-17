package com.example.registration.audit;

import com.example.governance.runtime.audit.AuditEntries;
import com.example.governance.runtime.audit.AuditEntryResponse;
import com.example.governance.runtime.audit.AuditEvent;
import com.example.governance.runtime.audit.AuditLookup;
import com.example.governance.runtime.audit.AuditSink;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RegistrationAuditStore implements AuditSink, AuditLookup {

    private final RegistrationAuditRepository repository;

    public RegistrationAuditStore(RegistrationAuditRepository repository) {
        this.repository = repository;
    }

    @Override
    public void append(List<AuditEvent> events) {
        for (AuditEvent event : events) {
            RegistrationAuditEntity row = new RegistrationAuditEntity();
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
