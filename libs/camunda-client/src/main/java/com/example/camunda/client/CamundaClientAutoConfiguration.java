package com.example.camunda.client;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(RestTemplateBuilder.class)
@EnableConfigurationProperties(CamundaClientProperties.class)
public class CamundaClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CamundaProcessClient camundaProcessClient(
            RestTemplateBuilder restTemplateBuilder,
            CamundaClientProperties properties
    ) {
        return new CamundaProcessClient(restTemplateBuilder, properties);
    }
}
