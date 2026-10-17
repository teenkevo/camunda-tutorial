package com.example.refund.service;

import com.example.refund.client.DocumentClient;
import com.example.refund.dto.NotifyRefundRequest;
import com.example.refund.dto.RefundApplicationResponse;
import com.example.refund.dto.RefundProcessStepResponse;
import com.example.refund.entity.RefundApplicationEntity;
import com.example.refund.repository.RefundApplicationRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundApplicationService {

    static final String DOCUMENT_TYPE = "REFUND_APPLICATION";

    private static final List<String> STATIONS = List.of(
            "STATION-NORTH", "STATION-SOUTH", "STATION-EAST", "STATION-WEST", "STATION-HQ"
    );
    private static final List<String> RISK_LEVELS = List.of("low", "medium", "high");

    private final DocumentClient documentClient;
    private final RefundApplicationRepository refundApplicationRepository;

    public RefundApplicationService(
            DocumentClient documentClient,
            RefundApplicationRepository refundApplicationRepository
    ) {
        this.documentClient = documentClient;
        this.refundApplicationRepository = refundApplicationRepository;
    }

    public RefundProcessStepResponse assignStation(String drn) {
        Map<String, Object> document = documentClient.getByDrn(drn);
        Map<String, Object> payload = payload(document);
        String station = text(payload, "station");
        if (station == null) {
            station = STATIONS.get(ThreadLocalRandom.current().nextInt(STATIONS.size()));
            merge(drn, Map.of("station", station), "STATION_ASSIGNED");
        }
        RefundProcessStepResponse response = new RefundProcessStepResponse();
        response.setDrn(drn);
        response.setStation(station);
        response.setApplicationStatus("ASSIGNED");
        record(drn, "ASSIGNED");
        return response;
    }

    public RefundProcessStepResponse checkRisk(String drn) {
        Map<String, Object> payload = payload(documentClient.getByDrn(drn));
        String riskLevel = text(payload, "riskLevel");
        if (riskLevel == null) {
            riskLevel = RISK_LEVELS.get(ThreadLocalRandom.current().nextInt(RISK_LEVELS.size()));
            merge(drn, Map.of("riskLevel", riskLevel), "RISK_CHECKED");
        }
        RefundProcessStepResponse response = new RefundProcessStepResponse();
        response.setDrn(drn);
        response.setRiskLevel(riskLevel);
        String status = "low".equals(riskLevel) ? "RISK_CHECKED" : "PENDING_REVIEW";
        response.setApplicationStatus(status);
        record(drn, status);
        return response;
    }

    public RefundProcessStepResponse autoApprove(String drn) {
        String reviewComment = "Automatically approved due to low risk level";
        merge(drn, Map.of("approved", true, "reviewComment", reviewComment), "AUTO_APPROVED");
        RefundProcessStepResponse response = new RefundProcessStepResponse();
        response.setDrn(drn);
        response.setApproved(true);
        response.setReviewComment(reviewComment);
        response.setApplicationStatus("AUTO_APPROVED");
        record(drn, "AUTO_APPROVED");
        return response;
    }

    public RefundProcessStepResponse notifyTaxpayer(String drn, NotifyRefundRequest request) {
        Map<String, Object> document = documentClient.getByDrn(drn);
        String decision = request.isApproved() ? "APPROVED" : "REJECTED";
        if (!decision.equals(text(document, "status"))) {
            String reviewComment = request.getReviewComment() == null ? "" : request.getReviewComment();
            String message = request.isApproved()
                    ? "Dear %s %s, your refund (DRN %s, station %s) has been APPROVED. %s"
                    .formatted(request.getFirstName(), request.getLastName(), drn, request.getStation(), reviewComment)
                    : "Dear %s %s, your refund (DRN %s, station %s) has been REJECTED. %s"
                    .formatted(request.getFirstName(), request.getLastName(), drn, request.getStation(), reviewComment);

            Map<String, Object> body = new HashMap<>();
            body.put("status", decision);
            body.put("eventType", decision);
            body.put("payload", Map.of(
                    "approved", request.isApproved(),
                    "reviewComment", reviewComment,
                    "notificationMessage", message,
                    "decision", decision
            ));
            documentClient.update(drn, body);

            RefundProcessStepResponse response = new RefundProcessStepResponse();
            response.setDrn(drn);
            response.setNotificationMessage(message);
            response.setApplicationStatus(decision);
            response.setDecision(decision);
            record(drn, decision);
            return response;
        }

        Map<String, Object> payload = payload(document);
        RefundProcessStepResponse response = new RefundProcessStepResponse();
        response.setDrn(drn);
        response.setNotificationMessage(text(payload, "notificationMessage"));
        response.setApplicationStatus(decision);
        response.setDecision(decision);
        record(drn, decision);
        return response;
    }

    @Transactional
    public void record(Map<String, Object> document) {
        recordDocument(document, null);
    }

    public RefundApplicationResponse getByDrn(String drn) {
        return toResponse(documentClient.getByDrn(drn));
    }

    public List<RefundApplicationResponse> findAll() {
        List<Map<String, Object>> documents = documentClient.list(DOCUMENT_TYPE);
        if (documents == null) {
            return List.of();
        }
        return documents.stream().map(this::toResponse).toList();
    }

    public Map<String, Object> findByProcessInstanceId(String processInstanceId) {
        return documentClient.getByProcessInstanceId(processInstanceId);
    }

    private void record(String drn, String status) {
        recordDocument(documentClient.getByDrn(drn), status);
    }

    @Transactional
    protected void recordDocument(Map<String, Object> document, String status) {
        String drn = text(document, "drn");
        if (drn == null) {
            return;
        }
        Map<String, Object> payload = payload(document);
        RefundApplicationEntity entity = refundApplicationRepository.findByDrn(drn).orElseGet(() -> {
            RefundApplicationEntity created = new RefundApplicationEntity();
            created.setDrn(drn);
            created.setCreatedAt(Instant.now());
            return created;
        });
        entity.setProcessInstanceId(text(document, "processInstanceId"));
        entity.setFirstName(text(payload, "firstName"));
        entity.setLastName(text(payload, "lastName"));
        entity.setTaxTypes(new ArrayList<>(stringList(payload.get("taxTypes"))));
        entity.setStation(text(payload, "station"));
        entity.setRiskLevel(text(payload, "riskLevel"));
        Object approved = payload.get("approved");
        if (approved instanceof Boolean value) {
            entity.setApproved(value);
        }
        String decision = text(payload, "decision");
        entity.setStatus(status != null ? status : decision != null ? decision : text(document, "status"));
        entity.setReviewComment(text(payload, "reviewComment"));
        entity.setNotificationMessage(text(payload, "notificationMessage"));
        entity.setDecision(decision);
        entity.setUpdatedAt(Instant.now());
        refundApplicationRepository.save(entity);
    }

    private Map<String, Object> merge(String drn, Map<String, Object> payloadPatch, String eventType) {
        Map<String, Object> body = new HashMap<>();
        body.put("eventType", eventType);
        body.put("payload", payloadPatch);
        return documentClient.update(drn, body);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> payload(Map<String, Object> document) {
        Object payload = document.get("payload");
        if (payload instanceof Map<?, ?> map) {
            return new HashMap<>((Map<String, Object>) map);
        }
        return new HashMap<>();
    }

    private RefundApplicationResponse toResponse(Map<String, Object> document) {
        Map<String, Object> payload = payload(document);
        RefundApplicationResponse response = new RefundApplicationResponse();
        Object id = document.get("id");
        if (id instanceof Number number) {
            response.setId(number.longValue());
        }
        response.setDrn(text(document, "drn"));
        response.setProcessInstanceId(text(document, "processInstanceId"));
        response.setFirstName(text(payload, "firstName"));
        response.setLastName(text(payload, "lastName"));
        response.setTaxTypes(stringList(payload.get("taxTypes")));
        response.setStation(text(payload, "station"));
        response.setRiskLevel(text(payload, "riskLevel"));
        Object approved = payload.get("approved");
        if (approved instanceof Boolean value) {
            response.setApproved(value);
        }
        response.setStatus(text(document, "status"));
        response.setReviewComment(text(payload, "reviewComment"));
        response.setNotificationMessage(text(payload, "notificationMessage"));
        response.setDecision(text(payload, "decision"));
        response.setCreatedAt(instant(document.get("createdAt")));
        response.setUpdatedAt(instant(document.get("updatedAt")));
        refundApplicationRepository.findByDrn(response.getDrn())
                .map(RefundApplicationEntity::getAuditId)
                .ifPresent(response::setRefundApplicationAud);
        return response;
    }

    @SuppressWarnings("unchecked")
    private List<String> stringList(Object raw) {
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return new ArrayList<>();
    }

    private String text(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() || "null".equals(text) ? null : text;
    }

    private Instant instant(Object value) {
        return value == null ? null : Instant.parse(String.valueOf(value));
    }
}
