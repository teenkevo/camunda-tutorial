package com.example.authority.ursb;

import jakarta.validation.constraints.NotBlank;

public record UrsbLookupRequest(
        @NotBlank String registrationNumber,
        @NotBlank String registrationDate
) {
}
