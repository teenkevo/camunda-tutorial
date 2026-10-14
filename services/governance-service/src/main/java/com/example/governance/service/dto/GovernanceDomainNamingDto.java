package com.example.governance.service.dto;

import java.util.List;
import lombok.Data;

@Data
public class GovernanceDomainNamingDto {

    private String id;
    private String label;
    private String tablePrefix;
    private String tablePattern;
    private List<String> services;
}
