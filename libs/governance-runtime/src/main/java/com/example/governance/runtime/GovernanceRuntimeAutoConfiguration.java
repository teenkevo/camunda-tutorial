package com.example.governance.runtime;

import com.example.governance.runtime.catalog.GovernanceCatalogProperties;
import com.example.governance.runtime.catalog.GovernanceRegistryProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@EnableScheduling
@EnableConfigurationProperties({
        SensitivityProtectionProperties.class,
        GovernanceCatalogProperties.class,
        GovernanceRegistryProperties.class
})
@ComponentScan(basePackages = "com.example.governance.runtime")
public class GovernanceRuntimeAutoConfiguration {
}
