package com.example.governance.service.repository;

import com.example.governance.service.entity.GovernanceValidationEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GovernanceValidationRepository extends JpaRepository<GovernanceValidationEntity, Long> {

    Optional<GovernanceValidationEntity> findByValidationId(String validationId);

    List<GovernanceValidationEntity> findByServiceNameOrderByValidationIdAsc(String serviceName);

    List<GovernanceValidationEntity> findAllByOrderByServiceNameAscValidationIdAsc();
}
