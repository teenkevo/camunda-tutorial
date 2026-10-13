package com.example.governance.service;

import com.example.governance.service.config.GovernanceNamingProperties;
import com.example.governance.service.config.OpenMetadataProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({GovernanceNamingProperties.class, OpenMetadataProperties.class})
public class GovernanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GovernanceServiceApplication.class, args);
    }
}
