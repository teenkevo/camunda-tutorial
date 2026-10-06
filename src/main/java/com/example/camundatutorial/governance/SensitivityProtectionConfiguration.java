package com.example.camundatutorial.governance;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SensitivityProtectionProperties.class)
public class SensitivityProtectionConfiguration {
}
