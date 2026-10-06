package com.example.camundatutorial.governance;

/**
 * Classification of sensitive data for governance, redaction, and schema documentation.
 * <p>
 * When mirrored into DB column remarks, the tag format is {@code [TYPE]} (e.g. {@code [PII]}).
 */
public enum SensitiveType {

    /** Personally identifiable information (e.g. name, contact details). */
    PII,

    /** Confidential or restricted business data that is not necessarily PII. */
    SENSITIVE,

    /** Internal operational data not intended for external exposure. */
    INTERNAL;

    /** Stable remark token consumed by data-governance observability tools. */
    public String commentTag() {
        return "[" + name() + "]";
    }

    static String[] names() {
        SensitiveType[] values = values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].name();
        }
        return names;
    }
}
