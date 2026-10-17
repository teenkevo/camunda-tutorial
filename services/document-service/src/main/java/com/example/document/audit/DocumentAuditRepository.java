package com.example.document.audit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentAuditRepository extends JpaRepository<DocumentAuditEntity, Long> {

    List<DocumentAuditEntity> findByAuditIdOrderByOccurredAtAscIdAsc(String auditId);
}
