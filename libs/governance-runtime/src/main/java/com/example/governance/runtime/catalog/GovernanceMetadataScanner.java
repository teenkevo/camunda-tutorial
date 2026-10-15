package com.example.governance.runtime.catalog;

import com.example.governance.annotations.DataDomain;
import com.example.governance.annotations.DataGovernance;
import com.example.governance.annotations.DataOwner;
import com.example.governance.annotations.Domain;
import com.example.governance.annotations.Sensitive;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.hibernate.annotations.Comment;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

/**
 * Scans JPA {@link Entity} classes for domain identity ({@link Domain})
 * and structural field annotations used by the governance catalog.
 */
@Component
public class GovernanceMetadataScanner {

    public List<GovernanceAssetDto> scan(String serviceName, Set<String> basePackages) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        List<GovernanceAssetDto> assets = new ArrayList<>();
        for (String basePackage : basePackages) {
            for (BeanDefinition definition : scanner.findCandidateComponents(basePackage)) {
                try {
                    Class<?> type = Class.forName(definition.getBeanClassName());
                    assets.addAll(describeEntity(serviceName, type));
                } catch (ClassNotFoundException ex) {
                    throw new IllegalStateException("Failed to load entity " + definition.getBeanClassName(), ex);
                }
            }
        }
        return assets;
    }

    private List<GovernanceAssetDto> describeEntity(String serviceName, Class<?> type) {
        List<GovernanceAssetDto> assets = new ArrayList<>();
        Table table = type.getAnnotation(Table.class);
        String tableName = table != null && !table.name().isBlank() ? table.name() : type.getSimpleName();

        Domain domainAnn = type.getAnnotation(Domain.class);
        DataGovernance legacy = type.getAnnotation(DataGovernance.class);
        Comment typeComment = type.getAnnotation(Comment.class);

        GovernanceAssetDto entityAsset = new GovernanceAssetDto();
        entityAsset.setServiceName(serviceName);
        entityAsset.setAssetType("ENTITY");
        entityAsset.setEntityName(type.getName());
        entityAsset.setFieldName("");
        entityAsset.setTableName(tableName);

        if (domainAnn != null && !domainAnn.value().isBlank()) {
            entityAsset.setDomain(domainAnn.value().trim());
        } else if (legacy != null) {
            // Temporary bridge while migrating off @DataGovernance
            entityAsset.setDomain(legacy.domain());
            entityAsset.setOwner(legacy.owner());
            entityAsset.setSteward(legacy.steward());
            entityAsset.setClassification(legacy.classification().name());
            entityAsset.setRetentionPolicy(legacy.retentionPolicy());
        }

        if (typeComment != null && !typeComment.value().isBlank()) {
            entityAsset.setDescription(typeComment.value().trim());
        }
        assets.add(entityAsset);

        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                Sensitive sensitive = field.getAnnotation(Sensitive.class);
                DataOwner fieldOwner = field.getAnnotation(DataOwner.class);
                DataDomain fieldDomain = field.getAnnotation(DataDomain.class);
                Comment fieldComment = field.getAnnotation(Comment.class);
                if (sensitive == null && fieldOwner == null && fieldDomain == null && fieldComment == null) {
                    continue;
                }
                Column column = field.getAnnotation(Column.class);
                String columnName = column != null && !column.name().isBlank()
                        ? column.name()
                        : toSnakeCase(field.getName());

                GovernanceAssetDto fieldAsset = new GovernanceAssetDto();
                fieldAsset.setServiceName(serviceName);
                fieldAsset.setAssetType("FIELD");
                fieldAsset.setEntityName(type.getName());
                fieldAsset.setFieldName(field.getName());
                fieldAsset.setTableName(tableName);
                fieldAsset.setColumnName(columnName);
                fieldAsset.setDomain(entityAsset.getDomain());
                if (sensitive != null) {
                    fieldAsset.setClassification(sensitive.value().name());
                }
                if (fieldOwner != null) {
                    fieldAsset.setOwner(fieldOwner.value());
                }
                if (fieldDomain != null) {
                    fieldAsset.setDomain(fieldDomain.value());
                }
                if (fieldComment != null && !fieldComment.value().isBlank()) {
                    fieldAsset.setDescription(fieldComment.value().trim());
                }
                assets.add(fieldAsset);
            }
            current = current.getSuperclass();
        }
        return assets;
    }

    /** {@code refunds.application} → {@code refunds} (OpenMetadata asset id helper). */
    public static String domainFromAssetId(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return null;
        }
        int dot = assetId.indexOf('.');
        if (dot <= 0) {
            return assetId.trim();
        }
        return assetId.substring(0, dot).trim();
    }

    private static String toSnakeCase(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
