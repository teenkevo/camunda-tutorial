package com.example.governance.service.entity;

import com.example.governance.annotations.GovernedAsset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Data;
import org.hibernate.annotations.Comment;

/**
 * Identity catalog row: which service/entity maps to which governed asset id.
 * Authoritative owner/steward/classification/retention live in OpenMetadata;
 * mirrored columns are a last-known projection for browsing when OM is unreachable.
 */
@Data
@Entity
@Table(
        name = "GOV_ASSET",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_gov_asset",
                columnNames = {"service_name", "entity_name", "field_name"}
        )
)
@GovernedAsset("governance.asset")
@Comment("Catalog of governed asset identities synced from domain services")
public class GovernanceAssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Surrogate primary key")
    private Long id;

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

    /** Logical asset id from {@code @GovernedAsset}, e.g. {@code refunds.application}. */
    @Column(name = "asset_id", length = 255)
    @Comment("Logical governed asset id from @GovernedAsset")
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
    @Comment("Data domain derived from asset id or naming policy")
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
