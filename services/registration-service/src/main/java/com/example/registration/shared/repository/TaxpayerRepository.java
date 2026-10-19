package com.example.registration.shared.repository;

import com.example.registration.shared.domain.IdentifierType;
import com.example.registration.shared.entity.TaxpayerEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxpayerRepository extends JpaRepository<TaxpayerEntity, Long> {

    Optional<TaxpayerEntity> findByIdentifierTypeAndIdentifierValue(
            IdentifierType identifierType,
            String identifierValue
    );

    boolean existsByIdentifierTypeAndIdentifierValue(IdentifierType identifierType, String identifierValue);
}
