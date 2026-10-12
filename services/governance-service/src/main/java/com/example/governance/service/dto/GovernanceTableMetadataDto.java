package com.example.governance.service.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Table-centric governance view: table metadata plus annotated columns.
 * Extend this DTO as new metadata attributes are introduced.
 */
@Data
public class GovernanceTableMetadataDto {

    private String serviceName;
    private String tableName;
    private String entityName;
    private String owner;
    private String domain;
    private String description;
    private List<GovernanceColumnMetadataDto> columns = new ArrayList<>();
}
