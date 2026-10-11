package com.example.governance.service.controller;

import com.example.governance.service.dto.ConformanceReport;
import com.example.governance.service.dto.GovernanceAssetDto;
import com.example.governance.service.dto.GovernanceSyncRequest;
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

    public GovernanceCatalogController(GovernanceCatalogService catalogService) {
        this.catalogService = catalogService;
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

    @GetMapping("/conformance")
    public ConformanceReport conformance(@RequestParam("service") String service) {
        try {
            return catalogService.conformance(service);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    private GovernanceAssetDto toDto(GovernanceAssetEntity entity) {
        GovernanceAssetDto dto = new GovernanceAssetDto();
        dto.setServiceName(entity.getServiceName());
        dto.setAssetType(entity.getAssetType());
        dto.setEntityName(entity.getEntityName());
        dto.setFieldName(entity.getFieldName());
        dto.setTableName(entity.getTableName());
        dto.setColumnName(entity.getColumnName());
        dto.setClassification(entity.getClassification());
        dto.setOwner(entity.getOwner());
        dto.setDomain(entity.getDomain());
        return dto;
    }
}
