package com.example.registration.shared.domain;

/**
 * Whether the registered taxpayer is a person or an entity.
 */
public enum TaxpayerType {
    INDIVIDUAL,
    NON_INDIVIDUAL;

    public static TaxpayerType from(IdentifierType identifierType) {
        return switch (identifierType) {
            case NIN, AIN -> INDIVIDUAL;
            case ERN -> NON_INDIVIDUAL;
            case SYS -> throw new IllegalArgumentException("A system identifier has no taxpayer type");
        };
    }
}
