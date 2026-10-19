package com.example.governance.service.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.governance.runtime.audit.AuditRecord;
import com.example.governance.runtime.audit.AuditTrailSupport;
import com.example.governance.service.entity.GovernanceAssetEntity;
import com.example.governance.service.repository.GovernanceAssetRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@Import({AuditTrailSupport.class, GovernanceAuditStore.class})
class AuditTrailPersistenceTest {

    @Autowired
    private GovernanceAssetRepository assets;

    @Autowired
    private GovernanceAuditRepository audits;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void updateCommittedWithTheRowIsStoredOnTheSameAuditId() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        String auditId = transaction.execute(status -> {
            GovernanceAssetEntity row = new GovernanceAssetEntity();
            row.setServiceName("document-service");
            row.setAssetType("ENTITY");
            row.setEntityName("com.example.document.persistence.DocumentEntity");
            row.setFieldName("__ENTITY__");
            row.setTableName("DOCUMENT_RECORD");
            row.setLastSeenAt(Instant.parse("2026-10-17T14:00:00Z"));
            assets.save(row);

            row.setDescription("Document approved");
            assets.save(row);
            return row.getAuditId();
        });

        assertThat(audits.findByAuditIdOrderByOccurredAtAscIdAsc(auditId))
                .extracting(AuditRecord::getAction)
                .containsExactly("CREATED", "UPDATED");
        assertThat(audits.findByAuditIdOrderByOccurredAtAscIdAsc(auditId).get(1).getSnapshot())
                .contains("Document approved");
    }
}
