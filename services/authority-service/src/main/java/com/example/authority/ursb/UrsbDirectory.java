package com.example.authority.ursb;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class UrsbDirectory {

    private static final Map<String, UrsbEntityResponse> KNOWN = Map.of(
            "80020001234567", new UrsbEntityResponse(
                    "80020001234567", "Nile Harvest Limited", "PRIVATE_COMPANY", "2018-03-20",
                    "REGISTERED", "info@nileharvest.example.ug", "0753002001"
            ),
            "80020007654321", new UrsbEntityResponse(
                    "80020007654321", "Kampala Traders Cooperative", "COOPERATIVE", "2012-09-01",
                    "REGISTERED", "info@kampalatraders.example.ug", "0753002002"
            ),
            "80020000000001", new UrsbEntityResponse(
                    "80020000000001", "Silent Books Limited", "PRIVATE_COMPANY", "2010-01-01",
                    "REGISTERED", null, null
            )
    );

    public Optional<UrsbEntityResponse> find(UrsbLookupRequest request) {
        String number = normalize(request.registrationNumber());
        String registrationDate = request.registrationDate() == null ? "" : request.registrationDate().trim();
        if (number.length() < 5 || registrationDate.isBlank()) {
            return Optional.empty();
        }
        UrsbEntityResponse known = KNOWN.get(number);
        if (known != null) {
            return known.registrationDate().equals(registrationDate) ? Optional.of(known) : Optional.empty();
        }
        return Optional.of(new UrsbEntityResponse(
                number,
                "Entity " + number.substring(number.length() - 4),
                "PRIVATE_COMPANY",
                registrationDate,
                "REGISTERED",
                number.toLowerCase(Locale.ROOT) + "@ursb.example.ug",
                "075" + (Math.abs(number.hashCode()) % 10_000_000)
        ));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
