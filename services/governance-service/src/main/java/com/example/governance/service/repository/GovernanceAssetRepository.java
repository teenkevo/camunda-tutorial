package com.example.governance.service.repository;

import com.example.governance.service.entity.GovernanceAssetEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GovernanceAssetRepository extends JpaRepository<GovernanceAssetEntity, Long> {

    List<GovernanceAssetEntity> findByServiceNameOrderByEntityNameAscFieldNameAsc(String serviceName);

    Optional<GovernanceAssetEntity> findByServiceNameAndEntityNameAndFieldName(
            String serviceName,
            String entityName,
            String fieldName
    );

    List<GovernanceAssetEntity> findAllByOrderByServiceNameAscEntityNameAscFieldNameAsc();
}
