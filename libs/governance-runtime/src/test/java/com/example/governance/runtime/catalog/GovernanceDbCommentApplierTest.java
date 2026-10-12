package com.example.governance.runtime.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class GovernanceDbCommentApplierTest {

    @Test
    void toSnakeCase_matchesSpringBootDefault() {
        assertThat(GovernanceDbCommentApplier.toSnakeCase("firstName")).isEqualTo("first_name");
        assertThat(GovernanceDbCommentApplier.toSnakeCase("RefundApplicationEntity"))
                .isEqualTo("refund_application_entity");
    }

    @Test
    void buildCommentStatements_fromAnnotations() {
        MockEnvironment env = new MockEnvironment();
        GovernanceCatalogProperties catalog = new GovernanceCatalogProperties();
        GovernanceDbCommentApplier applier = new GovernanceDbCommentApplier(null, env, catalog);

        List<String> sql = applier.buildCommentStatements(Set.of(SampleCommentedEntity.class.getPackageName()));

        assertThat(sql).anyMatch(s -> s.contains("COMMENT ON TABLE sample_thing IS '")
                && s.contains("Sample table")
                && s.contains("owner=tax-ops")
                && s.contains("domain=refunds"));
        assertThat(sql).contains("COMMENT ON COLUMN sample_thing.full_name IS '[PII]'");
    }
}
