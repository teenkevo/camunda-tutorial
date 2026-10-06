package com.example.camundatutorial.governance;

import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.springframework.stereotype.Component;

/**
 * Discovers {@link Sensitive} on fields and JavaBean getters and decides whether
 * the configured protection threshold applies.
 */
@Component
public class SensitivityClassifier {

    private final SensitivityProtectionProperties properties;

    public SensitivityClassifier(SensitivityProtectionProperties properties) {
        this.properties = properties;
    }

    public boolean isProtected(Annotated annotated) {
        Sensitive sensitive = findSensitive(annotated);
        return sensitive != null && properties.protects(sensitive.value());
    }

    public boolean isProtected(Field field) {
        Sensitive sensitive = findSensitive(field);
        return sensitive != null && properties.protects(sensitive.value());
    }

    public boolean isProtected(Class<?> type, String propertyName) {
        Sensitive sensitive = findSensitive(type, propertyName);
        return sensitive != null && properties.protects(sensitive.value());
    }

    public Sensitive findSensitive(Annotated annotated) {
        if (annotated == null) {
            return null;
        }
        Sensitive direct = annotated.getAnnotation(Sensitive.class);
        if (direct != null) {
            return direct;
        }
        if (annotated instanceof AnnotatedField field) {
            return field.getAnnotation(Sensitive.class);
        }
        if (annotated instanceof AnnotatedMethod method) {
            return findSensitive(method.getDeclaringClass(), propertyNameFromAccessor(method.getName()));
        }
        return null;
    }

    public Sensitive findSensitive(Field field) {
        if (field == null) {
            return null;
        }
        return field.getAnnotation(Sensitive.class);
    }

    public Sensitive findSensitive(Class<?> type, String propertyName) {
        if (type == null || propertyName == null || propertyName.isBlank()) {
            return null;
        }
        Field field = findField(type, propertyName);
        if (field != null) {
            Sensitive onField = field.getAnnotation(Sensitive.class);
            if (onField != null) {
                return onField;
            }
        }
        Method getter = findGetter(type, propertyName);
        if (getter != null) {
            return getter.getAnnotation(Sensitive.class);
        }
        return null;
    }

    static String propertyNameFromAccessor(String methodName) {
        if (methodName == null) {
            return null;
        }
        String base;
        if (methodName.startsWith("get") && methodName.length() > 3) {
            base = methodName.substring(3);
        } else if (methodName.startsWith("is") && methodName.length() > 2) {
            base = methodName.substring(2);
        } else {
            return methodName;
        }
        return Character.toLowerCase(base.charAt(0)) + base.substring(1);
    }

    static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    static Method findGetter(Class<?> type, String propertyName) {
        String capitalized = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredMethod("get" + capitalized);
            } catch (NoSuchMethodException ignored) {
                try {
                    return current.getDeclaredMethod("is" + capitalized);
                } catch (NoSuchMethodException ignoredAgain) {
                    current = current.getSuperclass();
                }
            }
        }
        return null;
    }
}
