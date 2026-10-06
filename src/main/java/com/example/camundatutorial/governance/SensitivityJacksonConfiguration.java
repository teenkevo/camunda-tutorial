package com.example.camundatutorial.governance;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SensitivityJacksonConfiguration {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer sensitivityJacksonCustomizer(
            SensitivityClassifier classifier,
            SensitivityProtectionProperties properties,
            SensitiveDataMasker masker
    ) {
        SensitiveAnnotationIntrospector introspector =
                new SensitiveAnnotationIntrospector(classifier, properties, masker);
        return builder -> builder.modulesToInstall(new SimpleModule("sensitivityProtection") {
            @Override
            public void setupModule(SetupContext context) {
                context.appendAnnotationIntrospector(introspector);
            }
        });
    }
}
