package com.example.governance.service.service;

import com.example.governance.service.config.GovernanceDomainNamingRule;
import com.example.governance.service.config.GovernanceNamingProperties;
import com.example.governance.service.config.OpenMetadataProperties;
import com.example.governance.service.dto.ConformanceIssue;
import com.example.governance.service.dto.ConformanceReport;
import com.example.governance.service.dto.GovernanceAssetDto;
import com.example.governance.service.dto.GovernanceColumnMetadataDto;
import com.example.governance.service.dto.GovernanceMetadataDto;
import com.example.governance.service.dto.GovernanceSyncRequest;
import com.example.governance.service.dto.GovernanceTableMetadataDto;
import com.example.governance.service.dto.GovernanceValidationDto;
import com.example.governance.service.entity.GovernanceAssetEntity;
import com.example.governance.service.entity.GovernanceValidationEntity;
import com.example.governance.service.openmetadata.OpenMetadataClient;
import com.example.governance.service.repository.GovernanceAssetRepository;
import com.example.governance.service.repository.GovernanceValidationRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
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
    private final GovernanceValidationRepository validationRepository;
    private final GovernanceNamingProperties namingProperties;
    private final OpenMetadataProperties openMetadataProperties;
    private final OpenMetadataClient openMetadataClient;
    private final GovernanceRefreshNotifier refreshNotifier;

    public GovernanceCatalogService(
            GovernanceAssetRepository repository,
            GovernanceValidationRepository validationRepository,
            GovernanceNamingProperties namingProperties,
            OpenMetadataProperties openMetadataProperties,
            OpenMetadataClient openMetadataClient,
            GovernanceRefreshNotifier refreshNotifier
    ) {
        this.repository = repository;
        this.validationRepository = validationRepository;
        this.namingProperties = namingProperties;
        this.openMetadataProperties = openMetadataProperties;
        this.openMetadataClient = openMetadataClient;
        this.refreshNotifier = refreshNotifier;
    }

    @Transactional
    public List<GovernanceAssetEntity> sync(GovernanceSyncRequest request) {
        if (request == null || request.serviceName() == null || request.serviceName().isBlank()) {
            throw new IllegalArgumentException("serviceName is required");
        }
        String serviceName = request.serviceName().trim();
        Instant now = Instant.now();
        List<GovernanceAssetEntity> saved = new ArrayList<>();
        if (request.assets() != null) {
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
                entity.setAssetId(blankToNull(dto.getAssetId()));
                entity.setDomain(blankToNull(dto.getDomain()));
                entity.setDescription(blankToNull(dto.getDescription()));
                // Do not overwrite mirrored OM attributes from annotation sync (identity only)
                if (dto.getMetadataVersion() != null) {
                    entity.setMetadataVersion(dto.getMetadataVersion());
                }
                entity.setLastSeenAt(now);
                saved.add(repository.save(entity));
            }
        }
        syncValidations(serviceName, request.validations(), now);
        return saved;
    }

    @Transactional
    public List<GovernanceValidationEntity> syncValidations(
            String serviceName,
            List<GovernanceValidationDto> validations,
            Instant now
    ) {
        List<GovernanceValidationEntity> saved = new ArrayList<>();
        if (validations == null || validations.isEmpty()) {
            return saved;
        }
        Instant seenAt = now == null ? Instant.now() : now;
        for (GovernanceValidationDto dto : validations) {
            String validationId = blankToNull(dto.getValidationId());
            if (validationId == null) {
                throw new IllegalArgumentException("validationId is required");
            }
            GovernanceValidationEntity entity = validationRepository
                    .findByValidationId(validationId)
                    .orElseGet(GovernanceValidationEntity::new);
            entity.setValidationId(validationId);
            entity.setServiceName(serviceName);
            entity.setDtoType(blankToNull(dto.getDtoType()));
            entity.setFieldName(blankToNull(dto.getFieldName()));
            entity.setConstraintName(blankToNull(dto.getConstraintName()));
            entity.setConstraintType(blankToNull(dto.getConstraintType()));
            entity.setMessage(blankToNull(dto.getMessage()));
            entity.setControllerType(blankToNull(dto.getControllerType()));
            entity.setControllerMethod(blankToNull(dto.getControllerMethod()));
            entity.setHttpMethod(blankToNull(dto.getHttpMethod()));
            entity.setRequestPath(blankToNull(dto.getRequestPath()));
            entity.setLastSeenAt(seenAt);
            saved.add(validationRepository.save(entity));
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<GovernanceValidationEntity> listValidations(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) {
            return validationRepository.findAllByOrderByServiceNameAscValidationIdAsc();
        }
        return validationRepository.findByServiceNameOrderByValidationIdAsc(serviceName.trim());
    }

    @Transactional(readOnly = true)
    public Optional<GovernanceValidationEntity> findValidation(String validationId) {
        if (validationId == null || validationId.isBlank()) {
            return Optional.empty();
        }
        return validationRepository.findByValidationId(validationId.trim());
    }

    @Transactional(readOnly = true)
    public List<GovernanceAssetEntity> list(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) {
            return repository.findAllByOrderByServiceNameAscEntityNameAscFieldNameAsc();
        }
        return repository.findByServiceNameOrderByEntityNameAscFieldNameAsc(serviceName.trim());
    }

    /**
     * Resolve authoritative metadata for an asset id from OpenMetadata, optionally
     * mirroring into identity rows that share that assetId.
     */
    @Transactional
    public GovernanceMetadataDto resolveAsset(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            throw new IllegalArgumentException("assetId is required");
        }
        String id = assetId.trim();
        Optional<GovernanceMetadataDto> fromOm = openMetadataClient.fetchAsset(id);
        if (fromOm.isPresent()) {
            GovernanceMetadataDto meta = fromOm.get();
            mirrorToIdentity(meta);
            return meta;
        }
        // Fallback: last-known projection from identity catalog
        return repository.findAllByOrderByServiceNameAscEntityNameAscFieldNameAsc().stream()
                .filter(a -> id.equals(a.getAssetId()) && isEntityLevel(a))
                .findFirst()
                .map(a -> new GovernanceMetadataDto(
                        a.getAssetId(),
                        a.getMetadataVersion() == null ? 0L : a.getMetadataVersion(),
                        a.getOwner(),
                        a.getSteward(),
                        a.getDomain(),
                        a.getClassification(),
                        a.getRetentionPolicy(),
                        a.getDescription()
                ))
                .orElseThrow(() -> new IllegalArgumentException("Unknown governed asset: " + id));
    }

    @Transactional
    public void onRegistryAssetUpdated(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return;
        }
        try {
            GovernanceMetadataDto meta = resolveAsset(assetId.trim());
            refreshNotifier.notifyAssetUpdated(meta.assetId());
        } catch (IllegalArgumentException ex) {
            // unknown asset — still try fan-out for declared id
            refreshNotifier.notifyAssetUpdated(assetId.trim());
        }
    }

    private void mirrorToIdentity(GovernanceMetadataDto meta) {
        List<GovernanceAssetEntity> rows = repository.findAllByOrderByServiceNameAscEntityNameAscFieldNameAsc();
        Instant now = Instant.now();
        for (GovernanceAssetEntity row : rows) {
            if (!meta.assetId().equals(row.getAssetId()) || !isEntityLevel(row)) {
                continue;
            }
            row.setOwner(blankToNull(meta.owner()));
            row.setSteward(blankToNull(meta.steward()));
            row.setClassification(blankToNull(meta.classification()));
            row.setRetentionPolicy(blankToNull(meta.retentionPolicy()));
            row.setDomain(blankToNull(meta.domain()));
            if (meta.description() != null && !meta.description().isBlank()) {
                row.setDescription(meta.description().trim());
            }
            row.setMetadataVersion(meta.version());
            row.setLastSeenAt(now);
            repository.save(row);
        }
    }

    @Transactional(readOnly = true)
    public List<GovernanceTableMetadataDto> listTables(String serviceName) {
        List<GovernanceAssetEntity> assets = list(serviceName);
        Map<String, GovernanceTableMetadataDto> tables = new LinkedHashMap<>();

        for (GovernanceAssetEntity asset : assets) {
            if (isEntityLevel(asset)) {
                String key = tableKey(asset);
                GovernanceTableMetadataDto table = tables.computeIfAbsent(key, k -> newTable(asset));
                enrichTableFromEntity(table, asset);
                // Overlay live OM metadata when available
                if (asset.getAssetId() != null) {
                    openMetadataClient.fetchAsset(asset.getAssetId()).ifPresent(meta -> {
                        table.setOwner(meta.owner());
                        table.setSteward(meta.steward());
                        table.setClassification(meta.classification());
                        table.setRetentionPolicy(meta.retentionPolicy());
                        table.setDomain(meta.domain());
                        table.setMetadataVersion(meta.version());
                        if (meta.description() != null) {
                            table.setDescription(meta.description());
                        }
                    });
                }
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

    private static void enrichTableFromEntity(GovernanceTableMetadataDto table, GovernanceAssetEntity asset) {
        table.setServiceName(asset.getServiceName());
        table.setTableName(asset.getTableName());
        table.setEntityName(asset.getEntityName());
        table.setAssetId(asset.getAssetId());
        table.setOwner(asset.getOwner());
        table.setSteward(asset.getSteward());
        table.setDomain(asset.getDomain());
        table.setClassification(asset.getClassification());
        table.setRetentionPolicy(asset.getRetentionPolicy());
        table.setDescription(asset.getDescription());
        table.setMetadataVersion(asset.getMetadataVersion());
    }

    private static GovernanceTableMetadataDto newTable(GovernanceAssetEntity asset) {
        GovernanceTableMetadataDto table = new GovernanceTableMetadataDto();
        table.setServiceName(asset.getServiceName());
        table.setTableName(asset.getTableName());
        table.setEntityName(asset.getEntityName());
        table.setAssetId(asset.getAssetId());
        return table;
    }

    private static String tableKey(GovernanceAssetEntity asset) {
        return asset.getServiceName() + "|" + asset.getEntityName();
    }

    /**
     * App-level: {@code @Domain} + naming conventions.
     * Optional registry checks when an {@code assetId} is present and OM is enabled.
     */
    @Transactional(readOnly = true)
    public ConformanceReport conformance(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("serviceName is required");
        }
        String service = serviceName.trim();
        List<GovernanceAssetEntity> assets =
                repository.findByServiceNameOrderByEntityNameAscFieldNameAsc(service);
        Map<String, GovernanceAssetEntity> entitiesByName = new HashMap<>();
        for (GovernanceAssetEntity asset : assets) {
            if (isEntityLevel(asset)) {
                entitiesByName.put(asset.getEntityName(), asset);
            }
        }

        List<ConformanceIssue> issues = new ArrayList<>();
        for (GovernanceAssetEntity asset : assets) {
            if (isEntityLevel(asset)) {
                issues.addAll(entityIdentityIssues(asset));
                if (!isBlank(asset.getDomain()) && namingProperties.isEnforce()) {
                    issues.addAll(namingIssues(service, asset));
                }
                if (openMetadataProperties.isEnabled() && openMetadataProperties.isRequireRegistryAttributes()
                        && !isBlank(asset.getAssetId())) {
                    issues.addAll(registryAttributeIssues(asset));
                }
                continue;
            }

            if (!isBlank(asset.getClassification())) {
                GovernanceAssetEntity parent = entitiesByName.get(asset.getEntityName());
                String domain = firstNonBlank(asset.getDomain(), parent == null ? null : parent.getDomain());
                if (isBlank(domain)) {
                    issues.add(new ConformanceIssue(
                            asset.getEntityName(),
                            asset.getFieldName(),
                            "DOMAIN_REQUIRED_FOR_CLASSIFIED_FIELD",
                            "Classified field has no domain on field or parent entity @Domain"
                    ));
                }
            }
        }
        return new ConformanceReport(service, issues.isEmpty(), issues);
    }

    private List<ConformanceIssue> entityIdentityIssues(GovernanceAssetEntity asset) {
        List<ConformanceIssue> issues = new ArrayList<>();
        if (isBlank(asset.getDomain())) {
            issues.add(new ConformanceIssue(
                    asset.getEntityName(),
                    "",
                    "DOMAIN_REQUIRED",
                    "Entity is missing @Domain"
            ));
        }
        return issues;
    }

    private List<ConformanceIssue> registryAttributeIssues(GovernanceAssetEntity asset) {
        List<ConformanceIssue> issues = new ArrayList<>();
        Optional<GovernanceMetadataDto> meta = openMetadataClient.fetchAsset(asset.getAssetId());
        if (meta.isEmpty()) {
            issues.add(new ConformanceIssue(
                    asset.getEntityName(),
                    "",
                    "REGISTRY_ASSET_MISSING",
                    "OpenMetadata asset '" + asset.getAssetId() + "' is not registered"
            ));
            return issues;
        }
        GovernanceMetadataDto m = meta.get();
        if (isBlank(m.owner())) {
            issues.add(new ConformanceIssue(
                    asset.getEntityName(), "", "OWNER_REQUIRED",
                    "OpenMetadata asset '" + asset.getAssetId() + "' is missing owner"
            ));
        }
        if (isBlank(m.steward())) {
            issues.add(new ConformanceIssue(
                    asset.getEntityName(), "", "STEWARD_REQUIRED",
                    "OpenMetadata asset '" + asset.getAssetId() + "' is missing steward"
            ));
        }
        if (isBlank(m.classification())) {
            issues.add(new ConformanceIssue(
                    asset.getEntityName(), "", "CLASSIFICATION_REQUIRED",
                    "OpenMetadata asset '" + asset.getAssetId() + "' is missing classification"
            ));
        }
        if (isBlank(m.retentionPolicy())) {
            issues.add(new ConformanceIssue(
                    asset.getEntityName(), "", "RETENTION_POLICY_REQUIRED",
                    "OpenMetadata asset '" + asset.getAssetId() + "' is missing retentionPolicy"
            ));
        }
        return issues;
    }

    private List<ConformanceIssue> namingIssues(String serviceName, GovernanceAssetEntity entity) {
        List<ConformanceIssue> issues = new ArrayList<>();
        Optional<GovernanceDomainNamingRule> ruleOpt = namingProperties.findDomain(entity.getDomain());
        if (ruleOpt.isEmpty()) {
            issues.add(new ConformanceIssue(
                    entity.getEntityName(),
                    "",
                    "DOMAIN_NOT_ALLOWED",
                    "Domain '" + entity.getDomain() + "' is not in app.governance.naming.domains"
            ));
            return issues;
        }

        GovernanceDomainNamingRule rule = ruleOpt.get();
        if (rule.getServices() != null && !rule.getServices().isEmpty()
                && rule.getServices().stream().noneMatch(s -> s.equalsIgnoreCase(serviceName))) {
            issues.add(new ConformanceIssue(
                    entity.getEntityName(),
                    "",
                    "DOMAIN_SERVICE_MISMATCH",
                    "Domain '" + entity.getDomain() + "' is not allowed for service '" + serviceName + "'"
            ));
        }

        if (isBlank(entity.getTableName())) {
            issues.add(new ConformanceIssue(
                    entity.getEntityName(),
                    "",
                    "TABLE_NAME_REQUIRED",
                    "Entity is missing @Table name / tableName in catalog"
            ));
            return issues;
        }

        String physical = entity.getTableName().trim().toUpperCase(Locale.ROOT);
        String pattern = resolveTablePattern(rule);
        try {
            if (!Pattern.compile(pattern).matcher(physical).matches()) {
                issues.add(new ConformanceIssue(
                        entity.getEntityName(),
                        "",
                        "TABLE_NAME_CONVENTION",
                        "Table '" + entity.getTableName() + "' does not match domain '"
                                + rule.getId() + "' pattern " + pattern
                ));
            }
        } catch (PatternSyntaxException ex) {
            issues.add(new ConformanceIssue(
                    entity.getEntityName(),
                    "",
                    "TABLE_NAME_PATTERN_INVALID",
                    "Configured table-pattern for domain '" + rule.getId() + "' is invalid: " + pattern
            ));
        }
        return issues;
    }

    public static String resolveTablePattern(GovernanceDomainNamingRule rule) {
        if (rule.getTablePattern() != null && !rule.getTablePattern().isBlank()) {
            return rule.getTablePattern().trim();
        }
        String prefix = rule.getTablePrefix() == null ? "" : rule.getTablePrefix().trim().toUpperCase(Locale.ROOT);
        return "^" + Pattern.quote(prefix) + "[A-Z0-9_]+$";
    }

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
