package com.example.document.api;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import lombok.Data;

@Data
public class CreateDocumentRequest {

    @NotBlank
    private String documentType;

    @NotBlank
    private String processInstanceId;

    private String createdBy;

    private Map<String, Object> payload;
}
