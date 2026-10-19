package com.example.registration.process.firsttime.service;

import com.example.registration.shared.domain.IdentifierType;
import java.util.List;

public record ResolvedRegistration(
        IdentifierType identifierType,
        String identifierValue,
        String dateOfBirthOrIncorp,
        String taxpayerName,
        String email,
        String telephone,
        List<String> taxTypes
) {
}
