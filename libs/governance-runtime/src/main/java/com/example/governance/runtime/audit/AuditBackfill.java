package com.example.governance.runtime.audit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Gives existing rows an audit id and a BASELINE history snapshot.
 * Later startups leave rows that already have an id untouched.
 */
@Component
public class AuditBackfill {

    private final ObjectProvider<AuditSink> sinks;
    private final ObjectProvider<AuditLookup> lookups;
    private final EntityManager entityManager;
    private final TransactionTemplate transactions;

    public AuditBackfill(
            ObjectProvider<AuditSink> sinks,
            ObjectProvider<AuditLookup> lookups,
            EntityManager entityManager,
            PlatformTransactionManager transactionManager
    ) {
        this.sinks = sinks;
        this.lookups = lookups;
        this.entityManager = entityManager;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void backfill() {
        if (sinks.getIfAvailable() == null) {
            return;
        }
        transactions.executeWithoutResult(status -> {
            AuditTrailSupport.withAction("BASELINE", () -> {
                for (EntityType<?> type : entityManager.getMetamodel().getEntities()) {
                    Class<?> javaType = type.getJavaType();
                    if (!Audited.class.isAssignableFrom(javaType)) {
                        continue;
                    }
                    List<?> rows = entityManager
                            .createQuery("select e from " + type.getName() + " e where e.auditId is null", javaType)
                            .getResultList();
                    for (Object row : rows) {
                        ((Audited) row).setAuditId(UUID.randomUUID().toString());
                    }
                }
                entityManager.flush();
            });
            recordCurrentSnapshots();
        });
    }

    private void recordCurrentSnapshots() {
        AuditLookup lookup = lookups.getIfAvailable();
        if (lookup == null) {
            return;
        }
        for (EntityType<?> type : entityManager.getMetamodel().getEntities()) {
            Class<?> javaType = type.getJavaType();
            if (!Audited.class.isAssignableFrom(javaType)) {
                continue;
            }
            List<?> rows = entityManager
                    .createQuery("select e from " + type.getName() + " e where e.auditId is not null", javaType)
                    .getResultList();
            for (Object row : rows) {
                String action = AuditTrailSupport.missingSnapshotAction(lookup, row);
                if (action != null) {
                    AuditTrailSupport.record(row, action);
                }
            }
        }
    }
}
