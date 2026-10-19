package com.example.document.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentEventRepository extends JpaRepository<DocumentEventEntity, Long> {

    List<DocumentEventEntity> findByDrnOrderByCreatedAtAsc(String drn);
}
