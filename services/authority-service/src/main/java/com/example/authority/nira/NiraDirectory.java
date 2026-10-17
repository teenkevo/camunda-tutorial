package com.example.authority.nira;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NiraDirectory {

    private static final Map<String, NiraPersonResponse> KNOWN = Map.of(
            "CM90001234", new NiraPersonResponse(
                    "CM90001234", "NIN", "Amina", "Nakato", "1990-05-12", "F", "Ugandan",
                    "amina.nakato@example.ug", "0752001001"
            ),
            "AIN00012345", new NiraPersonResponse(
                    "AIN00012345", "AIN", "John", "Okello", "1988-11-02", "M", "Kenyan",
                    "john.okello@example.ug", "0752001002"
            ),
            "CM90008888", new NiraPersonResponse(
                    "CM90008888", "NIN", "Peter", "Mukasa", "1985-07-01", "M", "Ugandan",
                    null, null
            )
    );

    public Optional<NiraPersonResponse> find(NiraLookupRequest request) {
        String number = normalize(request.nationalIdentificationNumber());
        String dateOfBirth = request.dateOfBirth() == null ? "" : request.dateOfBirth().trim();
        if (number.length() < 5 || dateOfBirth.isBlank()) {
            return Optional.empty();
        }
        NiraPersonResponse known = KNOWN.get(number);
        if (known != null) {
            return known.dateOfBirth().equals(dateOfBirth) ? Optional.of(known) : Optional.empty();
        }
        String identifierType = request.identifierType() == null || request.identifierType().isBlank()
                ? "NIN"
                : request.identifierType().trim().toUpperCase(Locale.ROOT);
        boolean alien = "AIN".equals(identifierType);
        int hash = Math.abs(number.hashCode());
        return Optional.of(new NiraPersonResponse(
                number,
                identifierType,
                "Demo",
                alien ? "Alien " + number.substring(number.length() - 4) : "Citizen " + number.substring(number.length() - 4),
                dateOfBirth,
                hash % 2 == 0 ? "F" : "M",
                alien ? "Foreign" : "Ugandan",
                number.toLowerCase(Locale.ROOT) + "@nira.example.ug",
                "075" + (hash % 10_000_000)
        ));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
