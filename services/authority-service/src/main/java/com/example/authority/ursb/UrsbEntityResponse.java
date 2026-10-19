package com.example.authority.ursb;

public record UrsbEntityResponse(
        String registrationNumber,
        String entityName,
        String entityType,
        String registrationDate,
        String registrationStatus,
        String email,
        String telephone
) {
}
