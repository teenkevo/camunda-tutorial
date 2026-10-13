package com.example.governance.runtime.catalog;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal hook used by governance-service (or webhooks) to invalidate/refresh
 * the local governance metadata cache without restarting the microservice.
 */
@RestController
@RequestMapping("/internal/governance")
public class GovernanceRefreshController {

    private final GovernanceMetadataService metadataService;

    public GovernanceRefreshController(GovernanceMetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void refresh(
            @RequestParam(value = "assetId", required = false) String assetId,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        String id = assetId;
        if ((id == null || id.isBlank()) && body != null && body.get("assetId") != null) {
            id = String.valueOf(body.get("assetId"));
        }
        if (id == null || id.isBlank()) {
            // Refresh everything currently cached
            metadataService.snapshot().keySet().forEach(metadataService::refresh);
            return;
        }
        metadataService.refresh(id.trim());
    }
}
