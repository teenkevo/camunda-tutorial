package com.example.refund.repository;

import com.example.refund.entity.RefundApplicationEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundApplicationRepository extends JpaRepository<RefundApplicationEntity, Long> {

    Optional<RefundApplicationEntity> findByApplicationId(String applicationId);
}
