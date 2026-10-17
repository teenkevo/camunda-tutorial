package com.example.registration.shared.repository;

import com.example.registration.shared.entity.TaxObligationEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxObligationRepository extends JpaRepository<TaxObligationEntity, Long> {

    List<TaxObligationEntity> findByTaxpayerId(Long taxpayerId);
}
