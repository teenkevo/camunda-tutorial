package com.example.governance.runtime.audit;

import java.util.List;

/** Reads audit history for one audit id from the current service schema. */
public interface AuditLookup {

    List<AuditEntryResponse> find(String auditId);
}
