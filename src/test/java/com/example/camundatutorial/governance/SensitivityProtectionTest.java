package com.example.camundatutorial.governance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.camundatutorial.refund.RefundApplicationEntity;
import com.example.camundatutorial.refund.RefundApplicationMapper;
import com.example.camundatutorial.refund.RefundApplicationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class SensitivityProtectionTest {

    private SensitivityProtectionProperties properties;
    private SensitivityClassifier classifier;
    private SensitiveDataMasker masker;
    private SensitiveMappingSupport mappingSupport;
    private RefundApplicationMapper mapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new SensitivityProtectionProperties();
        properties.setEnabled(true);
        properties.setResponseMode(SensitivityProtectionMode.MASK);
        properties.setMaskValue("********");
        properties.setProtectedTypes(EnumSet.of(SensitiveType.PII));
        properties.setBlockEntityResponses(true);

        classifier = new SensitivityClassifier(properties);
        masker = new SensitiveDataMasker(properties);
        mappingSupport = new SensitiveMappingSupport(classifier, properties, masker);
        mapper = new RefundApplicationMapper(mappingSupport);
        objectMapper = createObjectMapper();

        // Enable SensitivitySupport.toString for logging tests
        new SensitivitySupport(classifier, masker, properties).register();
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        SensitiveAnnotationIntrospector introspector =
                new SensitiveAnnotationIntrospector(classifier, properties, masker);
        mapper.registerModule(new SimpleModule("sensitivityProtection") {
            @Override
            public void setupModule(SetupContext context) {
                context.appendAnnotationIntrospector(introspector);
            }
        });
        return mapper;
    }

    @Test
    void masker_preservesNull() {
        assertThat(masker.mask(null)).isNull();
        assertThat(masker.mask("John")).isEqualTo("********");
        assertThat(masker.mask(1012345678L)).isEqualTo("********");
    }

    @Test
    void entityToDto_masksPiiByDefault() {
        RefundApplicationEntity entity = sampleEntity();
        RefundApplicationResponse dto = mapper.toResponse(entity);

        assertThat(dto.getApplicationId()).isEqualTo("REF-1");
        assertThat(dto.getStatus()).isEqualTo("CREATED");
        assertThat(dto.getFirstName()).isEqualTo("********");
        assertThat(dto.getLastName()).isEqualTo("********");
        assertThat(dto.getReviewComment()).isEqualTo("********");
        assertThat(dto.getNotificationMessage()).isEqualTo("********");
    }

    @Test
    void entityToDto_omitsPiiWhenConfigured() {
        properties.setResponseMode(SensitivityProtectionMode.OMIT);
        RefundApplicationResponse dto = mapper.toResponse(sampleEntity());

        assertThat(dto.getApplicationId()).isEqualTo("REF-1");
        assertThat(dto.getFirstName()).isNull();
        assertThat(dto.getLastName()).isNull();
        assertThat(dto.getReviewComment()).isNull();
    }

    @Test
    void jackson_masksSensitiveDtoFields_ofManyTypes() throws Exception {
        MixedDto dto = new MixedDto();
        dto.name = "John Doe";
        dto.tin = "1012345678";
        dto.amount = new BigDecimal("12.50");
        dto.born = LocalDate.of(1990, 1, 2);
        dto.token = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        dto.safe = "ok";

        String json = objectMapper.writeValueAsString(dto);
        assertThat(json).doesNotContain("John Doe");
        assertThat(json).doesNotContain("1012345678");
        assertThat(json).doesNotContain("12.50");
        assertThat(json).doesNotContain("1990-01-02");
        assertThat(json).doesNotContain("123e4567-e89b-12d3-a456-426614174000");
        assertThat(json).contains("\"name\":\"********\"");
        assertThat(json).contains("\"tin\":\"********\"");
        assertThat(json).contains("\"amount\":\"********\"");
        assertThat(json).contains("\"born\":\"********\"");
        assertThat(json).contains("\"token\":\"********\"");
        assertThat(json).contains("\"safe\":\"ok\"");
    }

    @Test
    void jackson_omitsSensitiveFieldsWhenConfigured() throws Exception {
        properties.setResponseMode(SensitivityProtectionMode.OMIT);
        objectMapper = createObjectMapper();

        MixedDto dto = new MixedDto();
        dto.name = "John Doe";
        dto.safe = "ok";

        String json = objectMapper.writeValueAsString(dto);
        assertThat(json).doesNotContain("name");
        assertThat(json).doesNotContain("John Doe");
        assertThat(json).contains("\"safe\":\"ok\"");
    }

    @Test
    void jackson_preservesNullSensitiveFields() throws Exception {
        MixedDto dto = new MixedDto();
        dto.name = null;
        dto.safe = "ok";

        String json = objectMapper.writeValueAsString(dto);
        assertThat(json).contains("\"name\":null");
        assertThat(json).doesNotContain("********");
    }

    @Test
    void jackson_masksNestedAndCollections() throws Exception {
        Wrapper wrapper = new Wrapper();
        MixedDto one = new MixedDto();
        one.name = "Alice";
        one.safe = "a";
        MixedDto two = new MixedDto();
        two.name = "Bob";
        two.safe = "b";
        wrapper.taxpayer = one;
        wrapper.taxpayers = List.of(two);

        String json = objectMapper.writeValueAsString(wrapper);
        assertThat(json).doesNotContain("Alice");
        assertThat(json).doesNotContain("Bob");
        assertThat(json).contains("\"name\":\"********\"");
        assertThat(json).contains("\"safe\":\"a\"");
        assertThat(json).contains("\"safe\":\"b\"");
    }

    @Test
    void jackson_respectsSensitiveOnGetter() throws Exception {
        GetterAnnotatedDto dto = new GetterAnnotatedDto();
        dto.setNationalId("CM12345678");
        dto.setLabel("visible");

        String json = objectMapper.writeValueAsString(dto);
        assertThat(json).doesNotContain("CM12345678");
        assertThat(json).contains("\"nationalId\":\"********\"");
        assertThat(json).contains("\"label\":\"visible\"");
    }

    @Test
    void nonPiiSensitiveType_notProtectedByDefault() throws Exception {
        ThresholdDto dto = new ThresholdDto();
        dto.secret = "top-secret";
        dto.name = "Jane";

        String json = objectMapper.writeValueAsString(dto);
        assertThat(json).contains("\"secret\":\"top-secret\"");
        assertThat(json).contains("\"name\":\"********\"");
    }

    @Test
    void wideningThreshold_protectsSensitiveType() throws Exception {
        properties.setProtectedTypes(EnumSet.of(SensitiveType.PII, SensitiveType.SENSITIVE));
        objectMapper = createObjectMapper();

        ThresholdDto dto = new ThresholdDto();
        dto.secret = "top-secret";
        dto.name = "Jane";

        String json = objectMapper.writeValueAsString(dto);
        assertThat(json).contains("\"secret\":\"********\"");
        assertThat(json).contains("\"name\":\"********\"");
    }

    @Test
    void pipeline_entityToDtoToJson_neverLeaksOriginalPii() throws Exception {
        RefundApplicationEntity entity = sampleEntity();
        RefundApplicationResponse dto = mapper.toResponse(entity);
        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).doesNotContain("Ada");
        assertThat(json).doesNotContain("Lovelace");
        assertThat(json).doesNotContain("please call me");
        assertThat(json).doesNotContain("Dear Ada");
        assertThat(json).contains("REF-1");
        assertThat(json).contains("CREATED");
    }

    @Test
    void accidentalEntitySerialization_stillMaskedByJackson() throws Exception {
        // Safety net if someone bypasses the DTO mapper but Jackson still runs
        String json = objectMapper.writeValueAsString(sampleEntity());
        assertThat(json).doesNotContain("Ada");
        assertThat(json).doesNotContain("Lovelace");
        assertThat(json).contains("\"firstName\":\"********\"");
    }

    @Test
    void entityResponseGuard_rejectsJpaEntities() {
        EntityApiResponseGuard guard = new EntityApiResponseGuard(properties);
        assertThat(EntityApiResponseGuard.containsJpaEntity(sampleEntity())).isTrue();
        assertThat(EntityApiResponseGuard.containsJpaEntity(List.of(sampleEntity()))).isTrue();
        assertThat(EntityApiResponseGuard.containsJpaEntity(mapper.toResponse(sampleEntity()))).isFalse();

        assertThatThrownBy(() -> guard.beforeBodyWrite(
                sampleEntity(), null, null, null, null, null
        )).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("DTO");
    }

    @Test
    void logging_toString_masksPii() {
        RefundApplicationEntity entity = sampleEntity();
        String text = entity.toString();
        assertThat(text).doesNotContain("Ada");
        assertThat(text).doesNotContain("Lovelace");
        assertThat(text).contains("firstName=********");
        assertThat(text).contains("applicationId=REF-1");
    }

    @Test
    void nullSensitiveValue_staysNullInMapping() {
        RefundApplicationEntity entity = sampleEntity();
        entity.setReviewComment(null);
        RefundApplicationResponse dto = mapper.toResponse(entity);
        assertThat(dto.getReviewComment()).isNull();
    }

    private static RefundApplicationEntity sampleEntity() {
        RefundApplicationEntity entity = new RefundApplicationEntity();
        entity.setApplicationId("REF-1");
        entity.setProcessInstanceId("proc-1");
        entity.setFirstName("Ada");
        entity.setLastName("Lovelace");
        entity.setStatus("CREATED");
        entity.setReviewComment("please call me");
        entity.setNotificationMessage("Dear Ada Lovelace");
        entity.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return entity;
    }

    static class MixedDto {
        @Sensitive(SensitiveType.PII)
        public String name;
        @Sensitive(SensitiveType.PII)
        public String tin;
        @Sensitive(SensitiveType.PII)
        public BigDecimal amount;
        @Sensitive(SensitiveType.PII)
        public LocalDate born;
        @Sensitive(SensitiveType.PII)
        public UUID token;
        public String safe;
    }

    static class Wrapper {
        public MixedDto taxpayer;
        public List<MixedDto> taxpayers;
    }

    static class ThresholdDto {
        @Sensitive(SensitiveType.SENSITIVE)
        public String secret;
        @Sensitive(SensitiveType.PII)
        public String name;
    }

    static class GetterAnnotatedDto {
        private String nationalId;
        private String label;

        @Sensitive(SensitiveType.PII)
        public String getNationalId() {
            return nationalId;
        }

        public void setNationalId(String nationalId) {
            this.nationalId = nationalId;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }
}
