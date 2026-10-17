package com.example.registration.integration.ursb;

public record UrsbEntity(
        String registrationNumber,
        String entityName,
        String entityType,
        String registrationDate,
        String registrationStatus,
        String email,
        String telephone
) {
}
