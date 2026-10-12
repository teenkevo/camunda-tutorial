package com.example.governance.service.controller;

import com.example.governance.service.config.GovernanceDomainNamingRule;
import com.example.governance.service.config.GovernanceNamingProperties;
import com.example.governance.service.dto.ConformanceReport;
import com.example.governance.service.dto.GovernanceAssetDto;
import com.example.governance.service.dto.GovernanceDomainNamingDto;
import com.example.governance.service.dto.GovernanceSyncRequest;
import com.example.governance.service.dto.GovernanceTableMetadataDto;
import com.example.governance.service.entity.GovernanceAssetEntity;
import com.example.governance.service.service.GovernanceCatalogService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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

    /**
     * Table-centric metadata: owner, domain, description, sensitivity per column.
     * Prefer this when browsing or extending governance attributes.
     */
    @GetMapping("/tables")
    public List<GovernanceTableMetadataDto> tables(@RequestParam(value = "service", required = false) String service) {
        return catalogService.listTables(service);
    }

    @GetMapping("/conformance")
    public ConformanceReport conformance(@RequestParam("service") String service) {
        try {
            return catalogService.conformance(service);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    /** Configured allow-list of domains and table-naming conventions. */
    @GetMapping("/naming-domains")
    public List<GovernanceDomainNamingDto> namingDomains() {
        return namingProperties.getDomains().stream().map(this::toNamingDto).toList();
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
        dto.setClassification(entity.getClassification());
        dto.setOwner(entity.getOwner());
        dto.setSteward(entity.getSteward());
        dto.setDomain(entity.getDomain());
        dto.setRetentionPolicy(entity.getRetentionPolicy());
        dto.setDescription(entity.getDescription());
        return dto;
    }
}
