package com.example.camundatutorial.governance;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

import jakarta.annotation.Nonnull;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copies {@link Sensitive} classification into Hibernate column comments as {@code [PII]},
 * {@code [SENSITIVE]}, or {@code [INTERNAL]} so DB-level governance tools can discover them
 * via column remarks (e.g. H2 {@code INFORMATION_SCHEMA.COLUMNS.REMARKS}).
 */
public class SensitiveColumnCommentIntegrator implements Integrator {

    private static final Logger log = LoggerFactory.getLogger(SensitiveColumnCommentIntegrator.class);

    private static final Pattern LEADING_SENSITIVITY_TAG = Pattern.compile(
            "^\\[(" + String.join("|", SensitiveType.names()) + ")]\\s*",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public void integrate(
            Metadata metadata,
            @Nonnull BootstrapContext bootstrapContext,
            @Nonnull SessionFactoryImplementor sessionFactory
    ) {
        for (PersistentClass persistentClass : metadata.getEntityBindings()) {
            Class<?> mappedClass = persistentClass.getMappedClass();
            if (mappedClass == null) {
                continue;
            }
            applyToProperty(mappedClass, persistentClass.getIdentifierProperty());
            for (Property property : persistentClass.getProperties()) {
                applyToProperty(mappedClass, property);
            }
        }
    }

    private static void applyToProperty(Class<?> mappedClass, Property property) {
        if (property == null) {
            return;
        }
        Sensitive sensitive = findSensitive(mappedClass, property.getName());
        if (sensitive == null) {
            return;
        }
        String tag = sensitive.value().commentTag();
        for (Column column : property.getColumns()) {
            String merged = mergeComment(column.getComment(), tag);
            column.setComment(merged);
            log.debug(
                    "Injected sensitivity comment on {}.{}: {}",
                    mappedClass.getSimpleName(),
                    property.getName(),
                    merged
            );
        }
    }

    static String mergeComment(String existingComment, String tag) {
        String base = existingComment == null ? "" : existingComment.strip();
        base = LEADING_SENSITIVITY_TAG.matcher(base).replaceFirst("").strip();
        if (base.isEmpty()) {
            return tag;
        }
        return tag + " " + base;
    }

    private static Sensitive findSensitive(Class<?> type, String propertyName) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(propertyName);
                return field.getAnnotation(Sensitive.class);
            }
            catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    @Override
    public void disintegrate(
            @Nonnull SessionFactoryImplementor sessionFactory,
            @Nonnull SessionFactoryServiceRegistry serviceRegistry
    ) {
        // no-op
    }
}
