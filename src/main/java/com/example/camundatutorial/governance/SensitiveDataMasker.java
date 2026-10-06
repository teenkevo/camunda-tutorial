package com.example.camundatutorial.governance;

import org.springframework.stereotype.Component;

/**
 * Central masking utility. Never converts {@code null} into a mask.
 */
@Component
public class SensitiveDataMasker {

    private final SensitivityProtectionProperties properties;

    public SensitiveDataMasker(SensitivityProtectionProperties properties) {
        this.properties = properties;
    }

    /**
     * @return configured mask string, or {@code null} when {@code value} is {@code null}
     */
    public String mask(Object value) {
        if (value == null) {
            return null;
        }
        return properties.getMaskValue();
    }
}
