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
    void buildCommentStatements_onlyFromCommentAnnotation() {
        MockEnvironment env = new MockEnvironment();
        GovernanceCatalogProperties catalog = new GovernanceCatalogProperties();
        GovernanceDbCommentApplier applier = new GovernanceDbCommentApplier(null, env, catalog);

        List<String> sql = applier.buildCommentStatements(Set.of(SampleCommentedEntity.class.getPackageName()));

        assertThat(sql).contains("COMMENT ON TABLE sample_thing IS 'Sample table'");
        assertThat(sql).contains("COMMENT ON COLUMN sample_thing.code IS 'Business key'");
        // @Sensitive alone must not become an Oracle remark; clear instead
        assertThat(sql).contains("COMMENT ON COLUMN sample_thing.full_name IS ''");
        assertThat(sql).noneMatch(s -> s.contains("[PII]") || s.contains("owner=") || s.contains("domain="));
    }
}
