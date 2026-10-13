package com.example.governance.service.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Table-centric governance view: table metadata plus annotated columns.
 */
@Data
public class GovernanceTableMetadataDto {

    private String serviceName;
    private String tableName;
    private String entityName;
    private String assetId;
    private String owner;
    private String steward;
    private String domain;
    private String classification;
    private String retentionPolicy;
    private String description;
    private Long metadataVersion;
    private List<GovernanceColumnMetadataDto> columns = new ArrayList<>();
}
