package com.example.governance.annotations;

/**
 * Classification of sensitive data for governance, redaction, and catalog registration.
 * <p>
 * When mirrored into tooling, the tag format is {@code [TYPE]} (e.g. {@code [PII]}).
 */
public enum SensitiveType {

    /** Personally identifiable information (e.g. name, contact details). */
    PII,

    /** Confidential or restricted business data that is not necessarily PII. */
    SENSITIVE,

    /** Internal operational data not intended for external exposure. */
    INTERNAL;

    /** Stable token for catalogs and optional DB remark bridges. */
    public String commentTag() {
        return "[" + name() + "]";
    }

    public static String[] names() {
        SensitiveType[] values = values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].name();
        }
        return names;
    }
}
