package com.example.document.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

    Optional<DocumentEntity> findByDrn(String drn);

    Optional<DocumentEntity> findByProcessInstanceId(String processInstanceId);

    List<DocumentEntity> findByDocumentTypeOrderByCreatedAtDesc(String documentType);
}
