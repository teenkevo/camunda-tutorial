package com.example.governance.service.audit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GovernanceAuditRepository extends JpaRepository<GovernanceAuditEntity, Long> {

    List<GovernanceAuditEntity> findByAuditIdOrderByOccurredAtAscIdAsc(String auditId);
}
