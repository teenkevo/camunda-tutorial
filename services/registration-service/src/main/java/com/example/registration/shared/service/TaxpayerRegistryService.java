package com.example.registration.shared.service;

import com.example.registration.shared.domain.IdentifierType;
import com.example.registration.shared.domain.LifecycleState;
import com.example.registration.shared.domain.TaxpayerType;
import com.example.registration.shared.entity.TaxObligationEntity;
import com.example.registration.shared.entity.TaxpayerEntity;
import com.example.registration.shared.repository.TaxObligationRepository;
import com.example.registration.shared.repository.TaxpayerRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaxpayerRegistryService {

    private final TaxpayerRepository taxpayerRepository;
    private final TaxObligationRepository taxObligationRepository;

    public TaxpayerRegistryService(
            TaxpayerRepository taxpayerRepository,
            TaxObligationRepository taxObligationRepository
    ) {
        this.taxpayerRepository = taxpayerRepository;
        this.taxObligationRepository = taxObligationRepository;
    }

    public boolean isAlreadyActive(IdentifierType type, String value) {
        return taxpayerRepository.findByIdentifierTypeAndIdentifierValue(type, value)
                .map(t -> t.getLifecycleState() == LifecycleState.ACTIVE)
                .orElse(false);
    }

    @Transactional
    public TaxpayerEntity activateTaxpayer(
            IdentifierType identifierType,
            String identifierValue,
            String displayName,
            String email,
            String telephone,
            List<String> taxTypes
    ) {
        TaxpayerEntity taxpayer = taxpayerRepository
                .findByIdentifierTypeAndIdentifierValue(identifierType, identifierValue)
                .orElseGet(TaxpayerEntity::new);

        Instant now = Instant.now();
        taxpayer.setTaxpayerType(TaxpayerType.from(identifierType));
        taxpayer.setIdentifierType(identifierType);
        taxpayer.setIdentifierValue(identifierValue);
        taxpayer.setDisplayName(displayName);
        taxpayer.setEmail(email);
        taxpayer.setTelephone(telephone);
        taxpayer.setLifecycleState(LifecycleState.ACTIVE);
        if (taxpayer.getCreatedAt() == null) {
            taxpayer.setCreatedAt(now);
        }
        taxpayer.setUpdatedAt(now);
        taxpayer = taxpayerRepository.save(taxpayer);

        List<String> existingTypes = taxObligationRepository.findByTaxpayerId(taxpayer.getId()).stream()
                .map(TaxObligationEntity::getTaxType)
                .toList();
        List<TaxObligationEntity> obligations = new ArrayList<>();
        for (String taxType : taxTypes) {
            if (existingTypes.contains(taxType)) {
                continue;
            }
            TaxObligationEntity obligation = new TaxObligationEntity();
            obligation.setTaxpayerId(taxpayer.getId());
            obligation.setTaxType(taxType);
            obligation.setStatus("ACTIVE");
            obligation.setRegistrationRef(identifierValue + "-" + taxType);
            obligation.setCreatedAt(now);
            obligations.add(obligation);
        }
        if (!obligations.isEmpty()) {
            taxObligationRepository.saveAll(obligations);
        }
        return taxpayer;
    }
}
