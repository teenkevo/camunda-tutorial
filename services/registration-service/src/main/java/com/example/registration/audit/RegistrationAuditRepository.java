package com.example.registration.audit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationAuditRepository extends JpaRepository<RegistrationAuditEntity, Long> {

    List<RegistrationAuditEntity> findByAuditIdOrderByOccurredAtAscIdAsc(String auditId);
}
