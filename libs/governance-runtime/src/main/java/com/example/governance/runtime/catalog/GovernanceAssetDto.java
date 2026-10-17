package com.example.governance.runtime.catalog;

import lombok.Data;

@Data
public class GovernanceAssetDto {

    private String serviceName;
    private String assetType;
    private String entityName;
    private String fieldName = "";
    private String tableName;
    private String columnName;
    /** Optional OpenMetadata asset id when linked, e.g. {@code refunds.application}. */
    private String assetId;
    private String classification;
    private String owner;
    private String steward;
    private String domain;
    private String retentionPolicy;
    private String description;
    private Long metadataVersion;

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName == null ? "" : fieldName;
    }
}
