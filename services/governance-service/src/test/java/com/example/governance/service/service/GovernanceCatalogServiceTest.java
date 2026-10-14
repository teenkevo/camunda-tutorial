package com.example.governance.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.example.governance.service.config.GovernanceNamingProperties;
import com.example.governance.service.config.OpenMetadataProperties;
import com.example.governance.service.dto.ConformanceReport;
import com.example.governance.service.dto.GovernanceAssetDto;
import com.example.governance.service.dto.GovernanceMetadataDto;
import com.example.governance.service.dto.GovernanceSyncRequest;
import com.example.governance.service.dto.GovernanceTableMetadataDto;
import com.example.governance.service.dto.GovernanceValidationDto;
import com.example.governance.service.entity.GovernanceValidationEntity;
import com.example.governance.service.openmetadata.OpenMetadataClient;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@Import(GovernanceCatalogService.class)
@EnableConfigurationProperties({GovernanceNamingProperties.class, OpenMetadataProperties.class})
@TestPropertySource(properties = {
        "app.governance.openmetadata.enabled=true",
        "app.governance.openmetadata.require-registry-attributes=true",
        "app.governance.openmetadata.seed=false"
})
class GovernanceCatalogServiceTest {

    @Autowired
    private GovernanceCatalogService catalogService;

    @MockitoBean
    private OpenMetadataClient openMetadataClient;

    @MockitoBean
    private GovernanceRefreshNotifier refreshNotifier;

    @BeforeEach
    void stubRegistry() {
        when(openMetadataClient.isEnabled()).thenReturn(true);
        when(openMetadataClient.fetchAsset(anyString())).thenAnswer(invocation -> {
            String assetId = invocation.getArgument(0);
            return Optional.of(new GovernanceMetadataDto(
                    assetId,
                    1L,
                    "tax-ops",
                    "Refund Data Management",
                    "refunds",
                    "PII",
                    "REFUND_RETENTION",
                    "Refund application business record"
            ));
        });
    }

    @Test
    void syncAndConformance_requiresGovernedAssetAndRegistry() {
        GovernanceAssetDto entity = validRefundEntity();
        entity.setDescription("Refund application business record");

        GovernanceAssetDto field = new GovernanceAssetDto();
        field.setAssetType("FIELD");
        field.setEntityName("com.example.refund.entity.RefundApplicationEntity");
        field.setFieldName("firstName");
        field.setTableName("RFND_APPL");
        field.setColumnName("first_name");
        field.setClassification("PII");
        field.setAssetId("refunds.application");
        field.setDomain("refunds");

        catalogService.sync(new GovernanceSyncRequest("refund-service", List.of(entity, field)));

        ConformanceReport report = catalogService.conformance("refund-service");
        assertThat(report.compliant()).isTrue();

        List<GovernanceTableMetadataDto> tables = catalogService.listTables("refund-service");
        assertThat(tables).hasSize(1);
        assertThat(tables.get(0).getAssetId()).isEqualTo("refunds.application");
        assertThat(tables.get(0).getSteward()).isEqualTo("Refund Data Management");
        assertThat(tables.get(0).getColumns()).hasSize(1);
    }

    @Test
    void conformance_failsWhenGovernedAssetMissing() {
        GovernanceAssetDto entity = validRefundEntity();
        entity.setAssetId(null);
        entity.setDomain("refunds");

        catalogService.sync(new GovernanceSyncRequest("refund-service", List.of(entity)));

        ConformanceReport report = catalogService.conformance("refund-service");
        assertThat(report.compliant()).isFalse();
        assertThat(report.issues()).anyMatch(i -> "GOVERNED_ASSET_REQUIRED".equals(i.rule()));
    }

    @Test
    void conformance_failsWhenRegistryAssetMissing() {
        when(openMetadataClient.fetchAsset("refunds.application")).thenReturn(Optional.empty());

        catalogService.sync(new GovernanceSyncRequest("refund-service", List.of(validRefundEntity())));

        ConformanceReport report = catalogService.conformance("refund-service");
        assertThat(report.compliant()).isFalse();
        assertThat(report.issues()).anyMatch(i -> "REGISTRY_ASSET_MISSING".equals(i.rule()));
    }

    @Test
    void conformance_failsWhenStewardMissingInRegistry() {
        when(openMetadataClient.fetchAsset("refunds.application")).thenReturn(Optional.of(
                new GovernanceMetadataDto(
                        "refunds.application", 1L, "tax-ops", null, "refunds", "PII", "REFUND_RETENTION", null
                )
        ));

        catalogService.sync(new GovernanceSyncRequest("refund-service", List.of(validRefundEntity())));

        ConformanceReport report = catalogService.conformance("refund-service");
        assertThat(report.compliant()).isFalse();
        assertThat(report.issues()).anyMatch(i -> "STEWARD_REQUIRED".equals(i.rule()));
    }

    @Test
    void conformance_failsWhenDomainNotAllowed() {
        GovernanceAssetDto entity = validRefundEntity();
        entity.setAssetId("unknown-domain.thing");
        entity.setDomain(null);

        when(openMetadataClient.fetchAsset("unknown-domain.thing")).thenReturn(Optional.of(
                new GovernanceMetadataDto(
                        "unknown-domain.thing", 1L, "x", "y", "unknown-domain", "PII", "R", null
                )
        ));

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

    @Test
    void sync_upsertsDiscoveredRequestValidations() {
        GovernanceValidationDto validation = new GovernanceValidationDto();
        validation.setValidationId("refund-service.RefundApplicationRequest.firstName.NotBlank");
        validation.setDtoType("com.example.refund.dto.RefundApplicationRequest");
        validation.setFieldName("firstName");
        validation.setConstraintName("NotBlank");
        validation.setConstraintType("jakarta.validation.constraints.NotBlank");
        validation.setMessage("firstName is required");
        validation.setControllerType("com.example.refund.controller.RefundApplicationController");
        validation.setControllerMethod("start");
        validation.setHttpMethod("POST");
        validation.setRequestPath("/api/refund-applications");

        catalogService.sync(new GovernanceSyncRequest(
                "refund-service",
                List.of(validRefundEntity()),
                List.of(validation)
        ));

        List<GovernanceValidationEntity> listed = catalogService.listValidations("refund-service");
        assertThat(listed).hasSize(1);
        assertThat(listed.get(0).getValidationId())
                .isEqualTo("refund-service.RefundApplicationRequest.firstName.NotBlank");
        assertThat(listed.get(0).getConstraintName()).isEqualTo("NotBlank");

        validation.setMessage("updated message");
        catalogService.sync(new GovernanceSyncRequest("refund-service", List.of(), List.of(validation)));

        assertThat(catalogService.listValidations("refund-service")).hasSize(1);
        assertThat(catalogService.findValidation(
                "refund-service.RefundApplicationRequest.firstName.NotBlank"))
                .isPresent()
                .get()
                .extracting(GovernanceValidationEntity::getMessage)
                .isEqualTo("updated message");
    }

    private static GovernanceAssetDto validRefundEntity() {
        GovernanceAssetDto entity = new GovernanceAssetDto();
        entity.setAssetType("ENTITY");
        entity.setEntityName("com.example.refund.entity.RefundApplicationEntity");
        entity.setFieldName("");
        entity.setTableName("RFND_APPL");
        entity.setAssetId("refunds.application");
        entity.setDomain("refunds");
        return entity;
    }
}
