package com.example.refund.service;

import com.example.refund.dto.CreateRefundProcessRequest;
import com.example.refund.dto.NotifyRefundRequest;
import com.example.refund.dto.RefundProcessStepResponse;
import com.example.refund.entity.RefundApplicationEntity;
import com.example.refund.repository.RefundApplicationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RefundApplicationService {

    private static final List<String> STATIONS = List.of(
            "STATION-NORTH", "STATION-SOUTH", "STATION-EAST", "STATION-WEST", "STATION-HQ"
    );
    private static final List<String> RISK_LEVELS = List.of("low", "medium", "high");

    private final RefundApplicationRepository repository;

    public RefundApplicationService(RefundApplicationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public RefundProcessStepResponse createFromProcess(CreateRefundProcessRequest request) {
        String applicationId = "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Instant now = Instant.now();
        RefundApplicationEntity entity = new RefundApplicationEntity();
        entity.setApplicationId(applicationId);
        entity.setProcessInstanceId(request.getProcessInstanceId());
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setTaxTypes(request.getTaxTypes() == null ? List.of() : List.copyOf(request.getTaxTypes()));
        entity.setStatus("CREATED");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        repository.save(entity);

        RefundProcessStepResponse response = new RefundProcessStepResponse();
        response.setApplicationId(applicationId);
        response.setApplicationStatus("CREATED");
        response.setCreatedAt(now.toString());
        response.setTaxTypes(entity.getTaxTypes());
        return response;
    }

    @Transactional
    public RefundProcessStepResponse assignDrnAndStation(String applicationId) {
        int sequence = ThreadLocalRandom.current().nextInt(100000, 999999);
        String drn = "DRN-" + sequence;
        String station = STATIONS.get(ThreadLocalRandom.current().nextInt(STATIONS.size()));
        update(applicationId, entity -> {
            entity.setDrn(drn);
            entity.setStation(station);
            entity.setStatus("ASSIGNED");
        });
        RefundProcessStepResponse response = new RefundProcessStepResponse();
        response.setApplicationId(applicationId);
        response.setDrn(drn);
        response.setStation(station);
        response.setApplicationStatus("ASSIGNED");
        return response;
    }

    @Transactional
    public RefundProcessStepResponse checkRisk(String applicationId) {
        String riskLevel = RISK_LEVELS.get(ThreadLocalRandom.current().nextInt(RISK_LEVELS.size()));
        String status = "low".equals(riskLevel) ? "RISK_CHECKED" : "PENDING_REVIEW";
        update(applicationId, entity -> {
            entity.setRiskLevel(riskLevel);
            entity.setStatus(status);
        });
        RefundProcessStepResponse response = new RefundProcessStepResponse();
        response.setApplicationId(applicationId);
        response.setRiskLevel(riskLevel);
        response.setApplicationStatus(status);
        return response;
    }

    @Transactional
    public RefundProcessStepResponse autoApprove(String applicationId) {
        String reviewComment = "Automatically approved due to low risk level";
        update(applicationId, entity -> {
            entity.setApproved(true);
            entity.setReviewComment(reviewComment);
            entity.setStatus("AUTO_APPROVED");
        });
        RefundProcessStepResponse response = new RefundProcessStepResponse();
        response.setApplicationId(applicationId);
        response.setApproved(true);
        response.setReviewComment(reviewComment);
        response.setApplicationStatus("AUTO_APPROVED");
        return response;
    }

    @Transactional
    public RefundProcessStepResponse notifyTaxpayer(String applicationId, NotifyRefundRequest request) {
        String decision = request.isApproved() ? "APPROVED" : "REJECTED";
        String reviewComment = request.getReviewComment() == null ? "" : request.getReviewComment();
        String message = request.isApproved()
                ? "Dear %s %s, your refund application %s (DRN %s, station %s) has been APPROVED. %s"
                .formatted(request.getFirstName(), request.getLastName(), applicationId,
                        request.getDrn(), request.getStation(), reviewComment)
                : "Dear %s %s, your refund application %s (DRN %s, station %s) has been REJECTED. %s"
                .formatted(request.getFirstName(), request.getLastName(), applicationId,
                        request.getDrn(), request.getStation(), reviewComment);

        update(applicationId, entity -> {
            entity.setApproved(request.isApproved());
            entity.setReviewComment(reviewComment);
            entity.setNotificationMessage(message);
            entity.setDecision(decision);
            entity.setStatus(decision);
        });

        RefundProcessStepResponse response = new RefundProcessStepResponse();
        response.setApplicationId(applicationId);
        response.setNotificationMessage(message);
        response.setApplicationStatus(decision);
        response.setDecision(decision);
        return response;
    }

    @Transactional
    public RefundApplicationEntity update(String applicationId, Consumer<RefundApplicationEntity> changes) {
        RefundApplicationEntity entity = repository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalStateException("Refund application not found: " + applicationId));
        changes.accept(entity);
        entity.setUpdatedAt(Instant.now());
        return repository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<RefundApplicationEntity> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public RefundApplicationEntity getByApplicationId(String applicationId) {
        return repository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Refund application not found: " + applicationId
                ));
    }
}
