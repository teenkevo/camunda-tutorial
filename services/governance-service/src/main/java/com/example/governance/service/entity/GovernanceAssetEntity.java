package com.example.governance.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Data;

/**
 * Canonical governance catalog row. Lives only in the governance service database —
 * not as SQL comments on business tables.
 */
@Data
@Entity
@Table(
        name = "governance_asset",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_governance_asset",
                columnNames = {"service_name", "entity_name", "field_name"}
        )
)
public class GovernanceAssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_name", nullable = false, length = 120)
    private String serviceName;

    /** ENTITY or FIELD */
    @Column(name = "asset_type", nullable = false, length = 32)
    private String assetType;

    @Column(name = "entity_name", nullable = false, length = 255)
    private String entityName;

    /** Empty string for entity-level rows (unique constraint friendly). */
    @Column(name = "field_name", nullable = false, length = 255)
    private String fieldName = "";

    @Column(name = "table_name", length = 255)
    private String tableName;

    @Column(name = "column_name", length = 255)
    private String columnName;

    /** PII / SENSITIVE / INTERNAL */
    @Column(length = 64)
    private String classification;

    @Column(length = 120)
    private String owner;

    @Column(length = 120)
    private String domain;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName == null ? "" : fieldName;
    }
}
