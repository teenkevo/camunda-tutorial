package com.example.governance.service.entity;

import com.example.governance.annotations.Domain;
import com.example.governance.runtime.audit.Audited;
import com.example.governance.runtime.audit.AuditTrailListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Data;
import org.hibernate.annotations.Comment;

/**
 * Catalog row for entities/fields synced from domain services.
 * Domain comes from {@code @Domain}; optional {@code assetId} may link to OpenMetadata.
 */
@Data
@Entity
@EntityListeners(AuditTrailListener.class)
@Table(
        name = "GOVERNANCE_ASSET",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_GOVERNANCE_ASSET",
                columnNames = {"service_name", "entity_name", "field_name"}
        )
)
@Domain("governance")
@Comment("Catalog of entity/field metadata synced from domain services")
public class GovernanceAssetEntity implements Audited {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Surrogate primary key")
    private Long id;

    @Column(name = "GOVERNANCE_ASSET_AUD", unique = true, length = 36)
    @Comment("Audit id for this catalog row. Look up GOVERNANCE_AUDIT.AUDIT_ID for its history")
    private String auditId;

    @Column(name = "service_name", nullable = false, length = 120)
    @Comment("Domain microservice that published this asset row")
    private String serviceName;

    /** ENTITY or FIELD */
    @Column(name = "asset_type", nullable = false, length = 32)
    @Comment("Row kind: ENTITY or FIELD")
    private String assetType;

    @Column(name = "entity_name", nullable = false, length = 255)
    @Comment("Fully qualified Java entity class name")
    private String entityName;

    /**
     * Field name for FIELD assets. Entity-level rows use {@code __ENTITY__}
     * (Oracle treats empty string as NULL, so "" cannot be stored in a NOT NULL column).
     */
    @Column(name = "field_name", nullable = false, length = 255)
    @Comment("Java field name, or __ENTITY__ for entity-level rows")
    private String fieldName = "__ENTITY__";

    @Column(name = "table_name", length = 255)
    @Comment("Physical database table name")
    private String tableName;

    @Column(name = "column_name", length = 255)
    @Comment("Physical database column name for FIELD rows")
    private String columnName;

    /** Optional OpenMetadata glossary term id when linked, e.g. {@code refunds.application}. */
    @Column(name = "asset_id", length = 255)
    @Comment("Optional OpenMetadata asset id when linked")
    private String assetId;

    /** Mirrored from OpenMetadata (last-known). */
    @Column(length = 64)
    @Comment("Last-known classification mirrored from OpenMetadata")
    private String classification;

    @Column(length = 120)
    @Comment("Last-known owner mirrored from OpenMetadata")
    private String owner;

    @Column(length = 120)
    @Comment("Last-known steward mirrored from OpenMetadata")
    private String steward;

    @Column(length = 120)
    @Comment("Data domain from @Domain / naming policy")
    private String domain;

    @Column(name = "retention_policy", length = 120)
    @Comment("Last-known retention policy mirrored from OpenMetadata")
    private String retentionPolicy;

    @Column(length = 1000)
    @Comment("Human-readable description from @Comment or OpenMetadata")
    private String description;

    /** OpenMetadata entity version last mirrored. */
    @Column(name = "metadata_version")
    @Comment("OpenMetadata entity version last mirrored into this row")
    private Long metadataVersion;

    @Column(name = "last_seen_at", nullable = false)
    @Comment("When this asset was last published by a domain service sync")
    private Instant lastSeenAt = Instant.now();
}
