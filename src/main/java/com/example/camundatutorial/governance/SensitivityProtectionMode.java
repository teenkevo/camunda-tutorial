package com.example.camundatutorial.governance;

/**
 * How protected sensitive values are represented in API responses and entity→DTO mapping.
 */
public enum SensitivityProtectionMode {

    /** Replace the value with the configured mask (e.g. {@code ********}). */
    MASK,

    /** Omit the property / map it as {@code null}. */
    OMIT
}
