package com.example.governance.runtime;

import com.example.governance.annotations.Sensitive;
import com.example.governance.annotations.SensitiveType;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies to OMIT only during serialization so inbound JSON (POST bodies) can still
 * bind {@link Sensitive} fields. {@code hasIgnoreMarker} would break deserialization.
 */
public class SensitiveBeanSerializerModifier extends BeanSerializerModifier {

    private final SensitivityClassifier classifier;
    private final SensitivityProtectionProperties properties;

    public SensitiveBeanSerializerModifier(
            SensitivityClassifier classifier,
            SensitivityProtectionProperties properties
    ) {
        this.classifier = classifier;
        this.properties = properties;
    }

    @Override
    public List<BeanPropertyWriter> changeProperties(
            SerializationConfig config,
            BeanDescription beanDesc,
            List<BeanPropertyWriter> beanProperties
    ) {
        if (!properties.isEnabled()
                || properties.getResponseMode() != SensitivityProtectionMode.OMIT) {
            return beanProperties;
        }
        List<BeanPropertyWriter> kept = new ArrayList<>(beanProperties.size());
        for (BeanPropertyWriter writer : beanProperties) {
            if (writer.getMember() != null && classifier.isProtected(writer.getMember())) {
                continue;
            }
            kept.add(writer);
        }
        return kept;
    }
}
