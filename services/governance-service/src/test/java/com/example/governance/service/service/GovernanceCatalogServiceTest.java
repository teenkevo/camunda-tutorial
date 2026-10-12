package com.example.governance.service.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.governance.service.config.GovernanceNamingProperties;
import com.example.governance.service.dto.ConformanceReport;
import com.example.governance.service.dto.GovernanceAssetDto;
import com.example.governance.service.dto.GovernanceSyncRequest;
import com.example.governance.service.dto.GovernanceTableMetadataDto;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(GovernanceCatalogService.class)
@EnableConfigurationProperties(GovernanceNamingProperties.class)
class GovernanceCatalogServiceTest {

    @Autowired
    private GovernanceCatalogService catalogService;

    @Test
    void syncAndConformance_requiresFullDataGovernance() {
        GovernanceAssetDto entity = validRefundEntity();
        entity.setDescription("Refund application business record");

        GovernanceAssetDto field = new GovernanceAssetDto();
        field.setAssetType("FIELD");
        field.setEntityName("com.example.refund.entity.RefundApplicationEntity");
        field.setFieldName("firstName");
        field.setTableName("RFND_APPL");
        field.setColumnName("first_name");
        field.setClassification("PII");

        catalogService.sync(new GovernanceSyncRequest("refund-service", List.of(entity, field)));

        ConformanceReport report = catalogService.conformance("refund-service");
        assertThat(report.compliant()).isTrue();

        List<GovernanceTableMetadataDto> tables = catalogService.listTables("refund-service");
        assertThat(tables).hasSize(1);
        assertThat(tables.get(0).getSteward()).isEqualTo("Refund Data Management");
        assertThat(tables.get(0).getRetentionPolicy()).isEqualTo("REFUND_RETENTION");
        assertThat(tables.get(0).getClassification()).isEqualTo("PII");
        assertThat(tables.get(0).getColumns()).hasSize(1);
    }

    @Test
    void conformance_failsWhenStewardMissing() {
        GovernanceAssetDto entity = validRefundEntity();
        entity.setSteward(null);

        catalogService.sync(new GovernanceSyncRequest("refund-service", List.of(entity)));

        ConformanceReport report = catalogService.conformance("refund-service");
        assertThat(report.compliant()).isFalse();
        assertThat(report.issues()).anyMatch(i -> "STEWARD_REQUIRED".equals(i.rule()));
    }

    @Test
    void conformance_failsWhenRetentionMissing() {
        GovernanceAssetDto entity = validRefundEntity();
        entity.setRetentionPolicy(null);

        catalogService.sync(new GovernanceSyncRequest("refund-service", List.of(entity)));

        ConformanceReport report = catalogService.conformance("refund-service");
        assertThat(report.compliant()).isFalse();
        assertThat(report.issues()).anyMatch(i -> "RETENTION_POLICY_REQUIRED".equals(i.rule()));
    }

    @Test
    void conformance_failsWhenDomainNotAllowed() {
        GovernanceAssetDto entity = validRefundEntity();
        entity.setDomain("unknown-domain");

        catalogService.sync(new GovernanceSyncRequest("refund-service", List.of(entity)));

        ConformanceReport report = catalogService.conformance("refund-service");
        assertThat(report.compliant()).isFalse();
        assertThat(report.issues()).anyMatch(i -> "DOMAIN_NOT_ALLOWED".equals(i.rule()));
    }

    @Test
    void conformance_failsWhenTableNameBreaksConvention() {
        GovernanceAssetDto entity = validRefundEntity();
        entity.setTableName("refund_application");

        catalogService.sync(new GovernanceSyncRequest("refund-service", List.of(entity)));

        ConformanceReport report = catalogService.conformance("refund-service");
        assertThat(report.compliant()).isFalse();
        assertThat(report.issues()).anyMatch(i -> "TABLE_NAME_CONVENTION".equals(i.rule()));
    }

    private static GovernanceAssetDto validRefundEntity() {
        GovernanceAssetDto entity = new GovernanceAssetDto();
        entity.setAssetType("ENTITY");
        entity.setEntityName("com.example.refund.entity.RefundApplicationEntity");
        entity.setFieldName("");
        entity.setTableName("RFND_APPL");
        entity.setOwner("tax-ops");
        entity.setSteward("Refund Data Management");
        entity.setDomain("refunds");
        entity.setClassification("PII");
        entity.setRetentionPolicy("REFUND_RETENTION");
        return entity;
    }
}
