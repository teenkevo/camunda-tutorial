package com.example.document.api;

import java.util.Map;
import lombok.Data;

@Data
public class UpdateDocumentRequest {

    /** Next status. Omitted when only the payload changes. */
    private String status;

    private Long taxpayerId;

    private String eventType;

    /** Shallow-merged into the current JSON payload. */
    private Map<String, Object> payload;
}
