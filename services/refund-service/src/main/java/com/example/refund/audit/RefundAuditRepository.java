package com.example.refund.audit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundAuditRepository extends JpaRepository<RefundAuditEntity, Long> {

    List<RefundAuditEntity> findByAuditIdOrderByOccurredAtAscIdAsc(String auditId);
}
