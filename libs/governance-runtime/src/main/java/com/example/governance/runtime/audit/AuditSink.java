package com.example.governance.runtime.audit;

import java.util.List;

/** Writes audit history into the schema owned by the current service. */
public interface AuditSink {

    void append(List<AuditEvent> events);
}
