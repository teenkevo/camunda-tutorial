package com.example.taxpayer.repository;

import com.example.taxpayer.entity.TaxpayerRegistrationEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxpayerRegistrationRepository extends JpaRepository<TaxpayerRegistrationEntity, Long> {

    Optional<TaxpayerRegistrationEntity> findByProcessInstanceId(String processInstanceId);
}
