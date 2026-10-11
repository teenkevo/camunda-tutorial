package com.example.governance.service.service;

import com.example.governance.service.dto.ConformanceIssue;
import com.example.governance.service.dto.ConformanceReport;
import com.example.governance.service.dto.GovernanceAssetDto;
import com.example.governance.service.dto.GovernanceSyncRequest;
import com.example.governance.service.entity.GovernanceAssetEntity;
import com.example.governance.service.repository.GovernanceAssetRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GovernanceCatalogService {

    public static final String ASSET_ENTITY = "ENTITY";
    public static final String ASSET_FIELD = "FIELD";

    private final GovernanceAssetRepository repository;

    public GovernanceCatalogService(GovernanceAssetRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<GovernanceAssetEntity> sync(GovernanceSyncRequest request) {
        if (request == null || request.serviceName() == null || request.serviceName().isBlank()) {
            throw new IllegalArgumentException("serviceName is required");
        }
        String serviceName = request.serviceName().trim();
        Instant now = Instant.now();
        List<GovernanceAssetEntity> saved = new ArrayList<>();
        if (request.assets() == null) {
            return saved;
        }
        for (GovernanceAssetDto dto : request.assets()) {
            String fieldName = dto.getFieldName() == null ? "" : dto.getFieldName();
            GovernanceAssetEntity entity = repository
                    .findByServiceNameAndEntityNameAndFieldName(serviceName, dto.getEntityName(), fieldName)
                    .orElseGet(GovernanceAssetEntity::new);
            entity.setServiceName(serviceName);
            entity.setAssetType(dto.getAssetType() == null ? inferType(fieldName) : dto.getAssetType());
            entity.setEntityName(dto.getEntityName());
            entity.setFieldName(fieldName);
            entity.setTableName(dto.getTableName());
            entity.setColumnName(dto.getColumnName());
            entity.setClassification(blankToNull(dto.getClassification()));
            entity.setOwner(blankToNull(dto.getOwner()));
            entity.setDomain(blankToNull(dto.getDomain()));
            entity.setLastSeenAt(now);
            saved.add(repository.save(entity));
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<GovernanceAssetEntity> list(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) {
            return repository.findAllByOrderByServiceNameAscEntityNameAscFieldNameAsc();
        }
        return repository.findByServiceNameOrderByEntityNameAscFieldNameAsc(serviceName.trim());
    }

    /**
     * Enforces catalog completeness:
     * - every ENTITY must have owner + domain
     * - every FIELD with classification must resolve an owner + domain (field or parent entity)
     */
    @Transactional(readOnly = true)
    public ConformanceReport conformance(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("serviceName is required");
        }
        List<GovernanceAssetEntity> assets =
                repository.findByServiceNameOrderByEntityNameAscFieldNameAsc(serviceName.trim());
        Map<String, GovernanceAssetEntity> entitiesByName = new HashMap<>();
        for (GovernanceAssetEntity asset : assets) {
            if (ASSET_ENTITY.equals(asset.getAssetType()) || asset.getFieldName().isEmpty()) {
                entitiesByName.put(asset.getEntityName(), asset);
            }
        }

        List<ConformanceIssue> issues = new ArrayList<>();
        for (GovernanceAssetEntity asset : assets) {
            if (ASSET_ENTITY.equals(asset.getAssetType()) || asset.getFieldName().isEmpty()) {
                if (isBlank(asset.getOwner())) {
                    issues.add(new ConformanceIssue(
                            asset.getEntityName(),
                            "",
                            "OWNER_REQUIRED",
                            "Entity is missing @DataOwner / owner in catalog"
                    ));
                }
                if (isBlank(asset.getDomain())) {
                    issues.add(new ConformanceIssue(
                            asset.getEntityName(),
                            "",
                            "DOMAIN_REQUIRED",
                            "Entity is missing @DataDomain / domain in catalog"
                    ));
                }
                continue;
            }

            if (!isBlank(asset.getClassification())) {
                GovernanceAssetEntity parent = entitiesByName.get(asset.getEntityName());
                String owner = firstNonBlank(asset.getOwner(), parent == null ? null : parent.getOwner());
                String domain = firstNonBlank(asset.getDomain(), parent == null ? null : parent.getDomain());
                if (isBlank(owner)) {
                    issues.add(new ConformanceIssue(
                            asset.getEntityName(),
                            asset.getFieldName(),
                            "OWNER_REQUIRED_FOR_CLASSIFIED_FIELD",
                            "Classified field has no owner on field or parent entity"
                    ));
                }
                if (isBlank(domain)) {
                    issues.add(new ConformanceIssue(
                            asset.getEntityName(),
                            asset.getFieldName(),
                            "DOMAIN_REQUIRED_FOR_CLASSIFIED_FIELD",
                            "Classified field has no domain on field or parent entity"
                    ));
                }
            }
        }
        return new ConformanceReport(serviceName.trim(), issues.isEmpty(), issues);
    }

    private static String inferType(String fieldName) {
        return fieldName == null || fieldName.isEmpty() ? ASSET_ENTITY : ASSET_FIELD;
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (!isBlank(primary)) {
            return primary;
        }
        return isBlank(fallback) ? null : fallback;
    }
}
