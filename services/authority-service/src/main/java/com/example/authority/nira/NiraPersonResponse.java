package com.example.authority.nira;

public record NiraPersonResponse(
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
