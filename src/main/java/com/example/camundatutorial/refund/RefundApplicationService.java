package com.example.camundatutorial.refund;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RefundApplicationService {

    private final RefundApplicationRepository repository;

    public RefundApplicationService(RefundApplicationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public RefundApplicationEntity create(
            String applicationId,
            String processInstanceId,
            String firstName,
            String lastName
    ) {
        Instant now = Instant.now();
        RefundApplicationEntity entity = new RefundApplicationEntity();
        entity.setApplicationId(applicationId);
        entity.setProcessInstanceId(processInstanceId);
        entity.setFirstName(firstName);
        entity.setLastName(lastName);
        entity.setStatus("CREATED");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return repository.save(entity);
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
