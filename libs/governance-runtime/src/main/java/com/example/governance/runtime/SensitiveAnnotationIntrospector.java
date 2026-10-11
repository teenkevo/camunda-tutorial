package com.example.governance.runtime;

import com.example.governance.annotations.Sensitive;
import com.example.governance.annotations.SensitiveType;

import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;

/**
 * Jackson serialization safety net for {@link Sensitive} fields in MASK mode.
 * OMIT is handled by {@link SensitiveBeanSerializerModifier} so request deserialization
 * is never affected.
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
