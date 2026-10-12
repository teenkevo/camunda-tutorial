package com.example.governance.service.service;

import com.example.governance.service.dto.ConformanceIssue;
import com.example.governance.service.dto.ConformanceReport;
import com.example.governance.service.dto.GovernanceAssetDto;
import com.example.governance.service.dto.GovernanceColumnMetadataDto;
import com.example.governance.service.dto.GovernanceSyncRequest;
import com.example.governance.service.dto.GovernanceTableMetadataDto;
import com.example.governance.service.entity.GovernanceAssetEntity;
import com.example.governance.service.repository.GovernanceAssetRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GovernanceCatalogService {

    public static final String ASSET_ENTITY = "ENTITY";
    public static final String ASSET_FIELD = "FIELD";

    /**
     * Stored in {@code field_name} for entity-level rows. Oracle converts {@code ""} to NULL.
     */
    public static final String ENTITY_FIELD_MARKER = "__ENTITY__";

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
            String fieldName = normalizeFieldName(dto.getFieldName(), dto.getAssetType());
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
            entity.setDescription(blankToNull(dto.getDescription()));
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
     * Groups flat catalog rows into table → columns for browsing metadata.
     */
    @Transactional(readOnly = true)
    public List<GovernanceTableMetadataDto> listTables(String serviceName) {
        List<GovernanceAssetEntity> assets = list(serviceName);
        Map<String, GovernanceTableMetadataDto> tables = new LinkedHashMap<>();

        for (GovernanceAssetEntity asset : assets) {
            if (isEntityLevel(asset)) {
                String key = tableKey(asset);
                GovernanceTableMetadataDto table = tables.computeIfAbsent(key, k -> newTable(asset));
                table.setServiceName(asset.getServiceName());
                table.setTableName(asset.getTableName());
                table.setEntityName(asset.getEntityName());
                table.setOwner(asset.getOwner());
                table.setDomain(asset.getDomain());
                table.setDescription(asset.getDescription());
            }
        }

        for (GovernanceAssetEntity asset : assets) {
            if (isEntityLevel(asset)) {
                continue;
            }
            String key = tableKey(asset);
            GovernanceTableMetadataDto table = tables.computeIfAbsent(key, k -> newTable(asset));
            GovernanceColumnMetadataDto column = new GovernanceColumnMetadataDto();
            column.setFieldName(toApiFieldName(asset.getFieldName()));
            column.setColumnName(asset.getColumnName());
            column.setClassification(asset.getClassification());
            column.setOwner(asset.getOwner());
            column.setDomain(asset.getDomain());
            column.setDescription(asset.getDescription());
            table.getColumns().add(column);
        }

        return new ArrayList<>(tables.values());
    }

    private static GovernanceTableMetadataDto newTable(GovernanceAssetEntity asset) {
        GovernanceTableMetadataDto table = new GovernanceTableMetadataDto();
        table.setServiceName(asset.getServiceName());
        table.setTableName(asset.getTableName());
        table.setEntityName(asset.getEntityName());
        return table;
    }

    private static String tableKey(GovernanceAssetEntity asset) {
        return asset.getServiceName() + "|" + asset.getEntityName();
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
            if (isEntityLevel(asset)) {
                entitiesByName.put(asset.getEntityName(), asset);
            }
        }

        List<ConformanceIssue> issues = new ArrayList<>();
        for (GovernanceAssetEntity asset : assets) {
            if (isEntityLevel(asset)) {
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

    /** API-facing field name: marker/blank → empty string. */
    public static String toApiFieldName(String storedFieldName) {
        return isEntityFieldName(storedFieldName) ? "" : storedFieldName;
    }

    static String normalizeFieldName(String fieldName, String assetType) {
        if (ASSET_ENTITY.equals(assetType) || isEntityFieldName(fieldName)) {
            return ENTITY_FIELD_MARKER;
        }
        return fieldName.trim();
    }

    private static boolean isEntityLevel(GovernanceAssetEntity asset) {
        return ASSET_ENTITY.equals(asset.getAssetType()) || isEntityFieldName(asset.getFieldName());
    }

    private static boolean isEntityFieldName(String fieldName) {
        return isBlank(fieldName) || ENTITY_FIELD_MARKER.equals(fieldName);
    }

    private static String inferType(String fieldName) {
        return isEntityFieldName(fieldName) ? ASSET_ENTITY : ASSET_FIELD;
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
