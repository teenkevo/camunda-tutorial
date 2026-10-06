package com.example.camundatutorial.governance;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Spring-backed accessor so domain objects can build sensitivity-aware {@code toString()}
 * without taking dependencies in every constructor.
 */
@Component
public class SensitivitySupport {

    private static SensitivityClassifier classifier;
    private static SensitiveDataMasker masker;
    private static SensitivityProtectionProperties properties;

    private final SensitivityClassifier classifierBean;
    private final SensitiveDataMasker maskerBean;
    private final SensitivityProtectionProperties propertiesBean;

    public SensitivitySupport(
            SensitivityClassifier classifierBean,
            SensitiveDataMasker maskerBean,
            SensitivityProtectionProperties propertiesBean
    ) {
        this.classifierBean = classifierBean;
        this.maskerBean = maskerBean;
        this.propertiesBean = propertiesBean;
    }

    @PostConstruct
    void register() {
        classifier = classifierBean;
        masker = maskerBean;
        properties = propertiesBean;
    }

    public static String sensitiveToString(Object value) {
        if (classifier == null || masker == null) {
            return String.valueOf(value);
        }
        if (properties != null && !properties.isEnabled()) {
            return String.valueOf(value);
        }
        return SensitiveToStringBuilder.toString(value, classifier, masker);
    }
}
