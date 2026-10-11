package com.example.governance.service.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.governance.service.dto.ConformanceReport;
import com.example.governance.service.dto.GovernanceAssetDto;
import com.example.governance.service.dto.GovernanceSyncRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(GovernanceCatalogService.class)
class GovernanceCatalogServiceTest {

    @Autowired
    private GovernanceCatalogService catalogService;

    @Test
    void syncAndConformance_requiresOwnerAndDomain() {
        GovernanceAssetDto entity = new GovernanceAssetDto();
        entity.setAssetType("ENTITY");
        entity.setEntityName("com.example.refund.entity.RefundApplicationEntity");
        entity.setFieldName("");
        entity.setOwner("tax-ops");
        entity.setDomain("refunds");

        GovernanceAssetDto field = new GovernanceAssetDto();
        field.setAssetType("FIELD");
        field.setEntityName("com.example.refund.entity.RefundApplicationEntity");
        field.setFieldName("firstName");
        field.setClassification("PII");

        catalogService.sync(new GovernanceSyncRequest("refund-service", List.of(entity, field)));

        ConformanceReport report = catalogService.conformance("refund-service");
        assertThat(report.compliant()).isTrue();
        assertThat(catalogService.list("refund-service")).hasSize(2);
    }

    @Test
    void conformance_failsWhenOwnerMissing() {
        GovernanceAssetDto entity = new GovernanceAssetDto();
        entity.setAssetType("ENTITY");
        entity.setEntityName("com.example.refund.entity.RefundApplicationEntity");
        entity.setDomain("refunds");

        catalogService.sync(new GovernanceSyncRequest("refund-service", List.of(entity)));

        ConformanceReport report = catalogService.conformance("refund-service");
        assertThat(report.compliant()).isFalse();
        assertThat(report.issues()).anyMatch(i -> "OWNER_REQUIRED".equals(i.rule()));
    }
}
