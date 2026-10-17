package com.example.governance.runtime.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class AuditEntries {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private AuditEntries() {
    }

    public static AuditEntryResponse toResponse(AuditRecord row) {
        AuditEntryResponse response = new AuditEntryResponse();
        response.setId(row.getId());
        response.setAuditId(row.getAuditId());
        response.setTableName(row.getTableName());
        response.setRowId(row.getRowId());
        response.setAction(row.getAction());
        response.setOccurredAt(row.getOccurredAt());
        response.setSnapshot(readSnapshot(row.getSnapshot()));
        return response;
    }

    private static Object readSnapshot(String snapshot) {
        if (snapshot == null || snapshot.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(snapshot, new TypeReference<Object>() {});
        } catch (Exception exception) {
            return snapshot;
        }
    }
}
