package com.example.camundatutorial.governance;

import java.lang.reflect.Field;
import java.util.StringJoiner;

/**
 * Builds {@code toString()} output that respects {@link Sensitive} + configured thresholds.
 * Use from entity/DTO {@code toString()} so {@code log.info("{}", obj)} does not leak PII.
 */
public final class SensitiveToStringBuilder {

    private SensitiveToStringBuilder() {
    }

    public static String toString(Object value, SensitivityClassifier classifier, SensitiveDataMasker masker) {
        if (value == null) {
            return "null";
        }
        Class<?> type = value.getClass();
        StringJoiner joiner = new StringJoiner(", ", type.getSimpleName() + "{", "}");
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isSynthetic() || java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object fieldValue = field.get(value);
                    if (classifier.isProtected(field)) {
                        joiner.add(field.getName() + "=" + masker.mask(fieldValue));
                    } else {
                        joiner.add(field.getName() + "=" + fieldValue);
                    }
                } catch (IllegalAccessException e) {
                    joiner.add(field.getName() + "=<inaccessible>");
                }
            }
            current = current.getSuperclass();
        }
        return joiner.toString();
    }
}
