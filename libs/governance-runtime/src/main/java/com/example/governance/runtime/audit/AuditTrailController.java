package com.example.governance.runtime.audit;

import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/audit")
public class AuditTrailController {

    private final ObjectProvider<AuditLookup> lookup;

    public AuditTrailController(ObjectProvider<AuditLookup> lookup) {
        this.lookup = lookup;
    }

    @GetMapping("/{auditId}")
    public List<AuditEntryResponse> history(@PathVariable("auditId") String auditId) {
        AuditLookup service = lookup.getIfAvailable();
        if (service == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit history is not available");
        }
        List<AuditEntryResponse> rows = service.find(auditId);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No audit history for " + auditId);
        }
        return rows;
    }
}
