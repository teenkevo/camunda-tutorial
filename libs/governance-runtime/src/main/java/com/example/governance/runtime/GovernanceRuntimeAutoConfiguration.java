package com.example.governance.runtime;

import com.example.governance.runtime.catalog.GovernanceCatalogProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@EnableConfigurationProperties({
        SensitivityProtectionProperties.class,
        GovernanceCatalogProperties.class
})
@ComponentScan(basePackages = "com.example.governance.runtime")
public class GovernanceRuntimeAutoConfiguration {
}
