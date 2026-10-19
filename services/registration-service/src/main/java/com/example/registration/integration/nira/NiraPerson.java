package com.example.registration.integration.nira;

public record NiraPerson(
        String nationalIdentificationNumber,
        String identifierType,
        String givenNames,
        String surname,
        String dateOfBirth,
        String gender,
        String nationality,
        String email,
        String telephone
) {
}
