package com.example.document.service;

import com.example.document.api.CreateDocumentRequest;
import com.example.document.api.DocumentEventResponse;
import com.example.document.api.DocumentResponse;
import com.example.document.api.UpdateDocumentRequest;
import com.example.document.persistence.DocumentEntity;
import com.example.document.persistence.DocumentEventEntity;
import com.example.document.persistence.DocumentEventRepository;
import com.example.document.persistence.DocumentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentService {

    private static final Set<String> TERMINAL = Set.of("APPROVED", "REJECTED", "COMPLETED");

    private final DocumentRepository documentRepository;
    private final DocumentEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentEventRepository eventRepository,
            ObjectMapper objectMapper
    ) {
        this.documentRepository = documentRepository;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DocumentResponse create(CreateDocumentRequest request) {
        return documentRepository.findByProcessInstanceId(request.getProcessInstanceId())
                .filter(existing -> request.getDocumentType().equals(existing.getDocumentType()))
                .map(this::toResponse)
                .orElseGet(() -> toResponse(insert(request)));
    }

    @Transactional(readOnly = true)
    public DocumentResponse getByDrn(String drn) {
        return toResponse(require(drn));
    }

    @Transactional(readOnly = true)
    public DocumentResponse getByProcessInstanceId(String processInstanceId) {
        return documentRepository.findByProcessInstanceId(processInstanceId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document not found for processInstanceId=" + processInstanceId
                ));
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(String documentType) {
        List<DocumentEntity> rows = documentType == null || documentType.isBlank()
                ? documentRepository.findAll()
                : documentRepository.findByDocumentTypeOrderByCreatedAtDesc(documentType);
        return rows.stream().map(this::toResponse).toList();
    }

    @Transactional
    public DocumentResponse update(String drn, UpdateDocumentRequest request) {
        DocumentEntity entity = require(drn);
        String from = entity.getStatus();
        if (request.getStatus() != null && !request.getStatus().isBlank()
                && !request.getStatus().equals(from)) {
            assertTransition(from, request.getStatus());
            entity.setStatus(request.getStatus());
        }
        if (request.getTaxpayerId() != null) {
            entity.setTaxpayerId(request.getTaxpayerId());
        }
        if (request.getPayload() != null && !request.getPayload().isEmpty()) {
            Map<String, Object> merged = new LinkedHashMap<>(readPayload(entity.getPayload()));
            merged.putAll(request.getPayload());
            entity.setPayload(writePayload(merged));
        }
        entity.setUpdatedAt(Instant.now());
        documentRepository.save(entity);

        String eventType = eventType(request.getEventType(), from, entity.getStatus());
        recordEvent(entity, eventType, from, entity.getStatus(), eventMessage(eventType));
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<DocumentEventResponse> history(String drn) {
        require(drn);
        return eventRepository.findByDrnOrderByCreatedAtAsc(drn).stream().map(event -> {
            DocumentEventResponse response = new DocumentEventResponse();
            response.setId(event.getId());
            response.setDrn(event.getDrn());
            response.setEventType(event.getEventType());
            response.setFromStatus(event.getFromStatus());
            response.setToStatus(event.getToStatus());
            response.setMessage(event.getMessage());
            response.setCreatedAt(event.getCreatedAt());
            return response;
        }).toList();
    }

    private DocumentEntity insert(CreateDocumentRequest request) {
        Instant now = Instant.now();
        DocumentEntity entity = new DocumentEntity();
        entity.setDrn(newDrn(request.getDocumentType()));
        entity.setDocumentType(request.getDocumentType().trim());
        entity.setSchemaVersion(1);
        entity.setStatus("IN_PROGRESS");
        entity.setProcessInstanceId(request.getProcessInstanceId().trim());
        entity.setPayload(writePayload(request.getPayload() == null ? Map.of() : request.getPayload()));
        entity.setCreatedBy(request.getCreatedBy() == null ? "process" : request.getCreatedBy());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setSubmittedAt(now);
        documentRepository.save(entity);
        recordEvent(entity, "CREATED", null, "IN_PROGRESS", "DRN " + entity.getDrn());
        return entity;
    }

    private static String eventType(String requested, String from, String to) {
        if (!from.equals(to) && ("APPROVED".equals(to) || "REJECTED".equals(to) || "COMPLETED".equals(to))) {
            return to;
        }
        if (requested != null && !requested.isBlank()) {
            return requested;
        }
        return from.equals(to) ? "PAYLOAD_UPDATED" : "STATUS_CHANGED";
    }

    private static String eventMessage(String eventType) {
        return switch (eventType) {
            case "APPROVED" -> "Document approved";
            case "REJECTED" -> "Document rejected";
            case "COMPLETED" -> "Document completed";
            default -> null;
        };
    }

    private void assertTransition(String from, String to) {
        if (TERMINAL.contains(from)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Document status " + from + " cannot change");
        }
        if (!Set.of("IN_PROGRESS", "APPROVED", "REJECTED", "COMPLETED").contains(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported status " + to);
        }
    }

    private DocumentEntity require(String drn) {
        return documentRepository.findByDrn(drn)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + drn));
    }

    private void recordEvent(DocumentEntity entity, String eventType, String from, String to, String message) {
        DocumentEventEntity event = new DocumentEventEntity();
        event.setDocumentId(entity.getId());
        event.setDrn(entity.getDrn());
        event.setEventType(eventType);
        event.setFromStatus(from);
        event.setToStatus(to);
        event.setMessage(message);
        event.setCreatedAt(Instant.now());
        eventRepository.save(event);
    }

    private String newDrn(String documentType) {
        String prefix = switch (documentType) {
            case "INDIVIDUAL_REGISTRATION", "NON_INDIVIDUAL_REGISTRATION" -> "REG";
            case "REFUND_APPLICATION" -> "RFD";
            default -> "DOC";
        };
        return "DRN-" + prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Map<String, Object> readPayload(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Document payload is not valid JSON");
        }
    }

    private String writePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document payload cannot be stored");
        }
    }

    private DocumentResponse toResponse(DocumentEntity entity) {
        DocumentResponse response = new DocumentResponse();
        response.setId(entity.getId());
        response.setDrn(entity.getDrn());
        response.setDocumentType(entity.getDocumentType());
        response.setSchemaVersion(entity.getSchemaVersion());
        response.setStatus(entity.getStatus());
        response.setTaxpayerId(entity.getTaxpayerId());
        response.setProcessInstanceId(entity.getProcessInstanceId());
        response.setPayload(readPayload(entity.getPayload()));
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setVersionNo(entity.getVersionNo());
        return response;
    }
}
