package com.example.governance.service.dto;

import lombok.Data;

@Data
public class GovernanceColumnMetadataDto {

    private String fieldName;
    private String columnName;
    private String classification;
    private String owner;
    private String domain;
    private String description;
}
