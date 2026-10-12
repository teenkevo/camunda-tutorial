package com.example.governance.service.dto;

import lombok.Data;

@Data
public class GovernanceAssetDto {

    private String serviceName;
    private String assetType;
    private String entityName;
    private String fieldName;
    private String tableName;
    private String columnName;
    private String classification;
    private String owner;
    private String domain;
    private String description;
}
