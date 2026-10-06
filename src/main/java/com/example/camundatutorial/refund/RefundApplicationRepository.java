package com.example.camundatutorial.refund;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundApplicationRepository extends JpaRepository<RefundApplicationEntity, Long> {

    Optional<RefundApplicationEntity> findByApplicationId(String applicationId);

    Optional<RefundApplicationEntity> findByProcessInstanceId(String processInstanceId);
}
