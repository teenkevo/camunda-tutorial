package com.example.document.api;

import java.time.Instant;
import java.util.Map;
import lombok.Data;

@Data
public class DocumentResponse {

    private Long id;
    private String drn;
    private String documentType;
    private Integer schemaVersion;
    private String status;
    private Long taxpayerId;
    private String processInstanceId;
    private Map<String, Object> payload;
    private Instant createdAt;
    private Instant updatedAt;
    private Long versionNo;
}
