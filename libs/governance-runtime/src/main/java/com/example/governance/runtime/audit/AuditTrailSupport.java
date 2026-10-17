package com.example.governance.runtime.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class AuditTrailSupport implements ApplicationContextAware {

    private static final Object RESOURCE = new Object();
    private static final ThreadLocal<String> ACTION = new ThreadLocal<>();
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        context = applicationContext;
    }

    public static void withAction(String action, Runnable work) {
        ACTION.set(action);
        try {
            work.run();
        } finally {
            ACTION.remove();
        }
    }

    public static void record(Object entity, String defaultAction) {
        if (!(entity instanceof Audited audited)) {
            return;
        }
        AuditSink sink = sink();
        if (sink == null || audited.getAuditId() == null || audited.getAuditId().isBlank()) {
            return;
        }
        String action = ACTION.get() == null ? defaultAction : ACTION.get();
        AuditEvent event = new AuditEvent(
                audited.getAuditId(),
                tableName(entity),
                rowId(entity),
                action,
                snapshot(entity),
                Instant.now()
        );
        enqueue(sink, event);
    }

    static String snapshot(Object entity) {
        Map<String, Object> values = new LinkedHashMap<>();
        Class<?> type = Hibernate.getClass(entity);
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    values.put(field.getName(), readable(field.get(entity)));
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot read " + type.getSimpleName() + "." + field.getName(), exception);
                }
            }
            type = type.getSuperclass();
        }
        try {
            return MAPPER.writeValueAsString(values);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot snapshot " + Hibernate.getClass(entity).getSimpleName(), exception);
        }
    }

    private static Object readable(Object value) {
        if (value instanceof Collection<?> collection) {
            if (!Hibernate.isInitialized(collection)) {
                Hibernate.initialize(collection);
            }
            return new ArrayList<>(collection);
        }
        if (value != null && value.getClass().getName().startsWith("org.hibernate")) {
            return null;
        }
        return value;
    }

    private static void enqueue(AuditSink sink, AuditEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sink.append(List.of(event));
            return;
        }
        @SuppressWarnings("unchecked")
        List<AuditEvent> pending = (List<AuditEvent>) TransactionSynchronizationManager.getResource(RESOURCE);
        if (pending == null) {
            pending = new ArrayList<>();
            TransactionSynchronizationManager.bindResource(RESOURCE, pending);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    @SuppressWarnings("unchecked")
                    List<AuditEvent> events =
                            (List<AuditEvent>) TransactionSynchronizationManager.getResource(RESOURCE);
                    if (events != null && !events.isEmpty()) {
                        sink.append(List.copyOf(events));
                        events.clear();
                    }
                }

                @Override
                public void afterCompletion(int status) {
                    if (TransactionSynchronizationManager.hasResource(RESOURCE)) {
                        TransactionSynchronizationManager.unbindResource(RESOURCE);
                    }
                }
            });
        }
        pending.add(event);
    }

    private static AuditSink sink() {
        if (context == null) {
            return null;
        }
        ObjectProvider<AuditSink> sinks = context.getBeanProvider(AuditSink.class);
        return sinks.getIfAvailable();
    }

    private static String tableName(Object entity) {
        Table table = Hibernate.getClass(entity).getAnnotation(Table.class);
        if (table != null && !table.name().isBlank()) {
            return table.name();
        }
        return Hibernate.getClass(entity).getSimpleName();
    }

    private static String rowId(Object entity) {
        Class<?> type = Hibernate.getClass(entity);
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getAnnotation(Id.class) == null) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    Object id = field.get(entity);
                    return id == null ? null : String.valueOf(id);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot read id of " + type.getSimpleName(), exception);
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }
}
