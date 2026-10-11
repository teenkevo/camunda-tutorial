package com.example.governance.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.governance.annotations.Sensitive;
import com.example.governance.annotations.SensitiveType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SensitivityProtectionTest {

    private SensitivityProtectionProperties properties;
    private SensitivityClassifier classifier;
    private SensitiveDataMasker masker;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new SensitivityProtectionProperties();
        properties.setEnabled(true);
        properties.setResponseMode(SensitivityProtectionMode.MASK);
        properties.setMaskValue("********");
        properties.setProtectedTypes(EnumSet.of(SensitiveType.PII));

        classifier = new SensitivityClassifier(properties);
        masker = new SensitiveDataMasker(properties);
        objectMapper = createObjectMapper();
        new SensitivitySupport(classifier, masker, properties).register();
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        SensitiveAnnotationIntrospector introspector =
                new SensitiveAnnotationIntrospector(classifier, properties, masker);
        SensitiveBeanSerializerModifier omitModifier =
                new SensitiveBeanSerializerModifier(classifier, properties);
        mapper.registerModule(new SimpleModule("sensitivityProtection") {
            @Override
            public void setupModule(SetupContext context) {
                context.appendAnnotationIntrospector(introspector);
                context.addBeanSerializerModifier(omitModifier);
            }
        });
        return mapper;
    }

    @Test
    void maskMode_redactsManyTypes() throws Exception {
        SampleDto dto = new SampleDto();
        dto.name = "Ada";
        dto.amount = new BigDecimal("10.00");
        dto.born = LocalDate.of(1990, 1, 1);
        dto.token = UUID.randomUUID();
        dto.safe = "ok";

        String json = objectMapper.writeValueAsString(dto);
        assertThat(json).doesNotContain("Ada");
        assertThat(json).contains("\"name\":\"********\"");
        assertThat(json).contains("\"safe\":\"ok\"");
    }

    @Test
    void omitMode_stillDeserializesInboundPii() throws Exception {
        properties.setResponseMode(SensitivityProtectionMode.OMIT);
        objectMapper = createObjectMapper();

        SampleDto inbound = objectMapper.readValue("{\"name\":\"Ada\",\"safe\":\"ok\"}", SampleDto.class);
        assertThat(inbound.name).isEqualTo("Ada");

        inbound.safe = "visible";
        String outbound = objectMapper.writeValueAsString(inbound);
        assertThat(outbound).doesNotContain("name");
        assertThat(outbound).doesNotContain("Ada");
        assertThat(outbound).contains("\"safe\":\"visible\"");
    }

    @Test
    void nestedCollectionsAreProtected() throws Exception {
        Wrapper wrapper = new Wrapper();
        SampleDto child = new SampleDto();
        child.name = "Secret";
        child.safe = "x";
        wrapper.items = List.of(child);

        String json = objectMapper.writeValueAsString(wrapper);
        assertThat(json).doesNotContain("Secret");
        assertThat(json).contains("********");
    }

    @Test
    void toStringMasksPii() {
        SampleDto dto = new SampleDto();
        dto.name = "Ada";
        dto.safe = "ok";
        String text = SensitivitySupport.sensitiveToString(dto);
        assertThat(text).doesNotContain("Ada");
        assertThat(text).contains("name=********");
    }

    static class SampleDto {
        @Sensitive(SensitiveType.PII)
        public String name;
        @Sensitive(SensitiveType.PII)
        public BigDecimal amount;
        @Sensitive(SensitiveType.PII)
        public LocalDate born;
        @Sensitive(SensitiveType.PII)
        public UUID token;
        public String safe;
    }

    static class Wrapper {
        public List<SampleDto> items;
    }
}
