package com.example.authority.nira;

import jakarta.validation.constraints.NotBlank;

public record NiraLookupRequest(
        @NotBlank String nationalIdentificationNumber,
        @NotBlank String dateOfBirth,
        String identifierType
) {
}
