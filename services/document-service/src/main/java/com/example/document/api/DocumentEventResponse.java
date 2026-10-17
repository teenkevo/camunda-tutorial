package com.example.document.api;

import java.time.Instant;
import lombok.Data;

@Data
public class DocumentEventResponse {

    private Long id;
    private String drn;
    private String eventType;
    private String fromStatus;
    private String toStatus;
    private String message;
    private Instant createdAt;
}
