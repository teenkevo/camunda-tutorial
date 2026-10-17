package com.example.governance.runtime.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditTrailListenerTest {

    @Test
    void assignIdKeepsAnExistingIdAndSnapshotIncludesIt() {
        SampleRow row = new SampleRow();
        row.setDisplayName("Amina Nakato");
        String existing = UUID.randomUUID().toString();
        row.setAuditId(existing);

        AuditTrailListener listener = new AuditTrailListener();
        listener.assignId(row);

        assertThat(row.getAuditId()).isEqualTo(existing);
        assertThat(AuditTrailSupport.snapshot(row)).contains("Amina Nakato").contains(existing);
    }

    @Test
    void assignIdFillsAMissingId() {
        SampleRow row = new SampleRow();
        new AuditTrailListener().assignId(row);
        assertThat(row.getAuditId()).isNotBlank();
    }

    static class SampleRow implements Audited {
        private String auditId;
        private String displayName;

        @Override
        public String getAuditId() {
            return auditId;
        }

        @Override
        public void setAuditId(String auditId) {
            this.auditId = auditId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }
}
