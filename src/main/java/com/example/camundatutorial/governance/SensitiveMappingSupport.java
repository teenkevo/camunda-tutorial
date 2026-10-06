package com.example.camundatutorial.governance;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Applies sensitivity policy when copying Entity → DTO by matching property names.
 * Never copies an original protected value into the DTO.
 */
@Component
public class SensitiveMappingSupport {

    private final SensitivityClassifier classifier;
    private final SensitivityProtectionProperties properties;
    private final SensitiveDataMasker masker;

    public SensitiveMappingSupport(
            SensitivityClassifier classifier,
            SensitivityProtectionProperties properties,
            SensitiveDataMasker masker
    ) {
        this.classifier = classifier;
        this.properties = properties;
        this.masker = masker;
    }

    /**
     * Copies same-named fields from {@code source} to {@code target}.
     * Protected sensitive fields (on source or target) are nulled (OMIT) or masked (MASK for Strings).
     */
    public void copyApplyingPolicy(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        Class<?> sourceType = source.getClass();
        Class<?> targetType = target.getClass();
        for (Field targetField : allFields(targetType)) {
            Field sourceField = SensitivityClassifier.findField(sourceType, targetField.getName());
            if (sourceField == null) {
                continue;
            }
            try {
                sourceField.setAccessible(true);
                targetField.setAccessible(true);
                Object raw = sourceField.get(source);
                Object outbound = outboundValue(sourceType, targetType, targetField.getName(), raw, targetField.getType());
                if (canAssign(targetField.getType(), outbound)) {
                    targetField.set(target, outbound);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(
                        "Failed to map " + sourceField.getName() + " from " + sourceType.getSimpleName(),
                        e
                );
            }
        }
    }

    public Object outboundValue(
            Class<?> sourceType,
            Class<?> targetType,
            String propertyName,
            Object rawValue,
            Class<?> targetFieldType
    ) {
        boolean protectedField = classifier.isProtected(sourceType, propertyName)
                || classifier.isProtected(targetType, propertyName);
        if (!properties.isEnabled() || !protectedField) {
            return rawValue;
        }
        if (rawValue == null) {
            return null;
        }
        if (properties.getResponseMode() == SensitivityProtectionMode.OMIT) {
            return null;
        }
        if (targetFieldType == String.class) {
            return masker.mask(rawValue);
        }
        return null;
    }

    private static boolean canAssign(Class<?> targetType, Object value) {
        if (value == null) {
            return !targetType.isPrimitive();
        }
        return targetType.isAssignableFrom(value.getClass());
    }

    private static List<Field> allFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }
}
