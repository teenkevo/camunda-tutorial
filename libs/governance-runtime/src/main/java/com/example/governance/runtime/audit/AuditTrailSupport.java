package com.example.governance.runtime.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

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

    /**
     * {@code BASELINE} when the row has no history, {@code UPDATED} when the newest snapshot
     * does not match the current version or status, or null when history is already current.
     */
    public static String missingSnapshotAction(AuditLookup lookup, Object entity) {
        if (!(entity instanceof Audited audited) || audited.getAuditId() == null || audited.getAuditId().isBlank()) {
            return null;
        }
        List<AuditEntryResponse> history = lookup.find(audited.getAuditId());
        if (history.isEmpty()) {
            return "BASELINE";
        }
        Object storedSnapshot = history.get(history.size() - 1).getSnapshot();
        if (!(storedSnapshot instanceof Map<?, ?> stored)) {
            return null;
        }
        Map<String, Object> current;
        try {
            current = MAPPER.readValue(snapshot(entity), new TypeReference<>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot compare audit snapshot", exception);
        }
        for (String field : List.of("versionNo", "status", "lifecycleState")) {
            if (!stored.containsKey(field) || !current.containsKey(field)) {
                continue;
            }
            if (!String.valueOf(stored.get(field)).equals(String.valueOf(current.get(field)))) {
                return "UPDATED";
            }
        }
        return null;
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
        // Inserts flush early, so beforeCommit still runs. Updates flush inside the database
        // commit, after beforeCommit, so write them on the current connection immediately.
        if (flushInProgress()) {
            insert(sink, List.of(event));
            return;
        }
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
                    writePending(sink);
                }

                @Override
                public void afterCompletion(int status) {
                    List<AuditEvent> leftover = takePending();
                    if (status == STATUS_COMMITTED && leftover != null && !leftover.isEmpty()) {
                        writeAfterCommit(sink, leftover);
                    }
                }
            });
        }
        pending.add(event);
    }

    private static void writePending(AuditSink sink) {
        List<AuditEvent> events = takePending();
        if (events != null && !events.isEmpty()) {
            sink.append(events);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<AuditEvent> takePending() {
        if (!TransactionSynchronizationManager.hasResource(RESOURCE)) {
            return List.of();
        }
        List<AuditEvent> events = (List<AuditEvent>) TransactionSynchronizationManager.unbindResource(RESOURCE);
        return events == null ? List.of() : List.copyOf(events);
    }

    private static void writeAfterCommit(AuditSink sink, List<AuditEvent> events) {
        if (context == null) {
            return;
        }
        PlatformTransactionManager manager = context.getBeanProvider(PlatformTransactionManager.class).getIfAvailable();
        if (manager == null) {
            sink.append(events);
            return;
        }
        TransactionTemplate template = new TransactionTemplate(manager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.executeWithoutResult(status -> sink.append(events));
    }

    private static boolean flushInProgress() {
        EntityManager entityManager = entityManager();
        if (entityManager == null) {
            return false;
        }
        SessionImplementor session = entityManager.unwrap(SessionImplementor.class);
        return session.getPersistenceContext().isFlushing();
    }

    private static void insert(AuditSink sink, List<AuditEvent> events) {
        EntityManager entityManager = entityManager();
        if (entityManager == null) {
            sink.append(events);
            return;
        }
        String table = sink.auditTable();
        if (table == null || !table.matches("[A-Z_]+")) {
            throw new IllegalStateException("Unsafe audit table name: " + table);
        }
        String sql = "INSERT INTO " + table
                + " (AUDIT_ID, TABLE_NAME, ROW_ID, ACTION, SNAPSHOT, OCCURRED_AT) VALUES (?, ?, ?, ?, ?, ?)";
        entityManager.unwrap(Session.class).doWork(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (AuditEvent event : events) {
                    statement.setString(1, event.auditId());
                    statement.setString(2, event.tableName());
                    statement.setString(3, event.rowId());
                    statement.setString(4, event.action());
                    statement.setString(5, event.snapshot());
                    statement.setTimestamp(6, Timestamp.from(event.occurredAt()));
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        });
    }

    private static EntityManager entityManager() {
        if (context == null) {
            return null;
        }
        return context.getBeanProvider(EntityManager.class).getIfUnique();
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
