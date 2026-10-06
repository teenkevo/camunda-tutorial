package com.example.camundatutorial.governance;

import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;

/**
 * Jackson safety net: omits or masks properties marked {@link Sensitive}
 * when their type is within the configured protection threshold.
 * <p>
 * Chosen over a type-specific serializer so protection is annotation-driven
 * and works for any property Java type.
 */
public class SensitiveAnnotationIntrospector extends NopAnnotationIntrospector {

    private final SensitivityClassifier classifier;
    private final SensitivityProtectionProperties properties;
    private final SensitiveDataMasker masker;

    public SensitiveAnnotationIntrospector(
            SensitivityClassifier classifier,
            SensitivityProtectionProperties properties,
            SensitiveDataMasker masker
    ) {
        this.classifier = classifier;
        this.properties = properties;
        this.masker = masker;
    }

    @Override
    public boolean hasIgnoreMarker(AnnotatedMember member) {
        if (!properties.isEnabled()) {
            return false;
        }
        if (properties.getResponseMode() != SensitivityProtectionMode.OMIT) {
            return false;
        }
        return classifier.isProtected(member);
    }

    @Override
    public Object findSerializer(Annotated annotated) {
        if (!properties.isEnabled()) {
            return null;
        }
        if (properties.getResponseMode() != SensitivityProtectionMode.MASK) {
            return null;
        }
        if (!classifier.isProtected(annotated)) {
            return null;
        }
        return new SensitiveValueSerializer(masker);
    }
}
