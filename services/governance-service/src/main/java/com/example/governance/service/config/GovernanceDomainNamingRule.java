package com.example.governance.service.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * One allowed data domain and the table-naming convention it must follow.
 */
@Data
public class GovernanceDomainNamingRule {

    /** Domain id matching {@code @DataDomain} / catalog {@code domain}. */
    private String id;

    /** Optional human-readable label. */
    private String label;

    /** Required physical table prefix, e.g. {@code REG_} or {@code RFND_}. */
    private String tablePrefix;

    /**
     * Regex the physical table name must match (after uppercasing).
     * Defaults to {@code ^} + prefix + {@code [A-Z0-9_]+$} when blank.
     */
    private String tablePattern;

    /** Optional: only these microservices may publish this domain. Empty = any service. */
    private List<String> services = new ArrayList<>();
}
