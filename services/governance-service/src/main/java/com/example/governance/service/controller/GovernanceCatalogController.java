package com.example.governance.service.controller;

import com.example.governance.service.config.GovernanceDomainNamingRule;
import com.example.governance.service.config.GovernanceNamingProperties;
import com.example.governance.service.dto.ConformanceReport;
import com.example.governance.service.dto.GovernanceAssetDto;
import com.example.governance.service.dto.GovernanceDomainNamingDto;
import com.example.governance.service.dto.GovernanceMetadataDto;
import com.example.governance.service.dto.GovernanceSyncRequest;
import com.example.governance.service.dto.GovernanceTableMetadataDto;
import com.example.governance.service.dto.GovernanceValidationDto;
import com.example.governance.service.entity.GovernanceAssetEntity;
import com.example.governance.service.entity.GovernanceValidationEntity;
import com.example.governance.service.service.GovernanceCatalogService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/governance")
public class GovernanceCatalogController {

    private final GovernanceCatalogService catalogService;
    private final GovernanceNamingProperties namingProperties;

    public GovernanceCatalogController(
            GovernanceCatalogService catalogService,
            GovernanceNamingProperties namingProperties
    ) {
        this.catalogService = catalogService;
        this.namingProperties = namingProperties;
    }

    @PostMapping("/sync")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public List<GovernanceAssetDto> sync(@RequestBody GovernanceSyncRequest request) {
        try {
            return catalogService.sync(request).stream().map(this::toDto).toList();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping("/assets")
    public List<GovernanceAssetDto> assets(@RequestParam(value = "service", required = false) String service) {
        return catalogService.list(service).stream().map(this::toDto).toList();
    }

    /** Authoritative registry metadata for a logical asset id (OpenMetadata-backed). */
    @GetMapping("/assets/{assetId:.+}")
    public GovernanceMetadataDto assetById(@PathVariable("assetId") String assetId) {
        try {
            return catalogService.resolveAsset(assetId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @GetMapping("/tables")
    public List<GovernanceTableMetadataDto> tables(@RequestParam(value = "service", required = false) String service) {
        return catalogService.listTables(service);
    }

    @GetMapping("/validations")
    public List<GovernanceValidationDto> validations(
            @RequestParam(value = "service", required = false) String service
    ) {
        return catalogService.listValidations(service).stream().map(this::toValidationDto).toList();
    }

    @GetMapping("/validations/{validationId:.+}")
    public GovernanceValidationDto validationById(@PathVariable("validationId") String validationId) {
        return catalogService.findValidation(validationId)
                .map(this::toValidationDto)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Unknown validation: " + validationId));
    }

    @GetMapping("/conformance")
    public ConformanceReport conformance(@RequestParam("service") String service) {
        try {
            return catalogService.conformance(service);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping("/naming-domains")
    public List<GovernanceDomainNamingDto> namingDomains() {
        return namingProperties.getDomains().stream().map(this::toNamingDto).toList();
    }

    /**
     * OpenMetadata alert / generic webhook destination. Accepts flexible JSON and
     * extracts an asset id from common fields, then fans out cache refresh.
     */
    @PostMapping("/webhooks/openmetadata")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> openMetadataWebhook(@RequestBody Map<String, Object> payload) {
        String assetId = extractAssetId(payload);
        if (assetId != null) {
            catalogService.onRegistryAssetUpdated(assetId);
        }
        return Map.of(
                "accepted", true,
                "assetId", assetId == null ? "" : assetId
        );
    }

    @SuppressWarnings("unchecked")
    private static String extractAssetId(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        for (String key : List.of("assetId", "asset_id", "fullyQualifiedName", "displayName", "name")) {
            Object v = payload.get(key);
            if (v != null && !String.valueOf(v).isBlank()) {
                return normalizePossibleFqn(String.valueOf(v));
            }
        }
        Object entity = payload.get("entity");
        if (entity instanceof Map<?, ?> map) {
            Object displayName = map.get("displayName");
            if (displayName != null && !String.valueOf(displayName).isBlank()) {
                return normalizePossibleFqn(String.valueOf(displayName));
            }
            Object fqn = map.get("fullyQualifiedName");
            if (fqn != null && !String.valueOf(fqn).isBlank()) {
                return normalizePossibleFqn(String.valueOf(fqn));
            }
        }
        Object change = payload.get("changeEvent");
        if (change instanceof Map<?, ?> changeMap) {
            Object entityFqn = changeMap.get("entityFullyQualifiedName");
            if (entityFqn != null) {
                return normalizePossibleFqn(String.valueOf(entityFqn));
            }
        }
        return null;
    }

    private static String normalizePossibleFqn(String raw) {
        String value = raw.trim();
        // GovernedAssets.refunds_application → refunds.application
        if (value.contains(".")) {
            String after = value.substring(value.indexOf('.') + 1);
            if (after.contains("_") && !after.contains(".")) {
                int idx = after.indexOf('_');
                return after.substring(0, idx) + "." + after.substring(idx + 1);
            }
            if (value.chars().filter(ch -> ch == '.').count() == 1 && !value.startsWith("GovernedAssets")) {
                return value;
            }
        }
        if (value.contains("_") && !value.contains(".")) {
            int idx = value.indexOf('_');
            return value.substring(0, idx) + "." + value.substring(idx + 1);
        }
        return value;
    }

    private GovernanceDomainNamingDto toNamingDto(GovernanceDomainNamingRule rule) {
        GovernanceDomainNamingDto dto = new GovernanceDomainNamingDto();
        dto.setId(rule.getId());
        dto.setLabel(rule.getLabel());
        dto.setTablePrefix(rule.getTablePrefix());
        dto.setTablePattern(GovernanceCatalogService.resolveTablePattern(rule));
        dto.setServices(rule.getServices());
        return dto;
    }

    private GovernanceAssetDto toDto(GovernanceAssetEntity entity) {
        GovernanceAssetDto dto = new GovernanceAssetDto();
        dto.setServiceName(entity.getServiceName());
        dto.setAssetType(entity.getAssetType());
        dto.setEntityName(entity.getEntityName());
        dto.setFieldName(GovernanceCatalogService.toApiFieldName(entity.getFieldName()));
        dto.setTableName(entity.getTableName());
        dto.setColumnName(entity.getColumnName());
        dto.setAssetId(entity.getAssetId());
        dto.setClassification(entity.getClassification());
        dto.setOwner(entity.getOwner());
        dto.setSteward(entity.getSteward());
        dto.setDomain(entity.getDomain());
        dto.setRetentionPolicy(entity.getRetentionPolicy());
        dto.setDescription(entity.getDescription());
        dto.setMetadataVersion(entity.getMetadataVersion());
        return dto;
    }

    private GovernanceValidationDto toValidationDto(GovernanceValidationEntity entity) {
        GovernanceValidationDto dto = new GovernanceValidationDto();
        dto.setServiceName(entity.getServiceName());
        dto.setValidationId(entity.getValidationId());
        dto.setDtoType(entity.getDtoType());
        dto.setFieldName(entity.getFieldName());
        dto.setConstraintName(entity.getConstraintName());
        dto.setConstraintType(entity.getConstraintType());
        dto.setMessage(entity.getMessage());
        dto.setControllerType(entity.getControllerType());
        dto.setControllerMethod(entity.getControllerMethod());
        dto.setHttpMethod(entity.getHttpMethod());
        dto.setRequestPath(entity.getRequestPath());
        return dto;
    }
}
