package com.example.registration.integration.identity;

import com.example.registration.integration.nira.NiraClient;
import com.example.registration.integration.nira.NiraPerson;
import com.example.registration.integration.ursb.UrsbClient;
import com.example.registration.integration.ursb.UrsbEntity;
import com.example.registration.shared.domain.IdentifierType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Chooses NIRA or URSB from the identifier type and maps the authority payload into URA prefill fields.
 */
@Component
public class IdentityLookup {

    private final NiraClient niraClient;
    private final UrsbClient ursbClient;

    public IdentityLookup(NiraClient niraClient, UrsbClient ursbClient) {
        this.niraClient = niraClient;
        this.ursbClient = ursbClient;
    }

    public IdentityPrefill lookup(IdentifierType identifierType, String identifierValue, String dateOfBirthOrIncorp) {
        if (identifierType == IdentifierType.SYS) {
            return unmatched(null, "System identifiers are issued by URA and are not retrieved from NIRA or URSB");
        }
        if (identifierValue == null || identifierValue.isBlank() || dateOfBirthOrIncorp == null || dateOfBirthOrIncorp.isBlank()) {
            return unmatched(authority(identifierType), "Identifier and date of birth or registration are required");
        }
        return switch (identifierType) {
            case NIN, AIN -> fromNira(identifierType, identifierValue.trim(), dateOfBirthOrIncorp.trim());
            case ERN -> fromUrsb(identifierValue.trim(), dateOfBirthOrIncorp.trim());
            case SYS -> unmatched(null, "System identifiers are issued by URA and are not retrieved from NIRA or URSB");
        };
    }

    private IdentityPrefill fromNira(IdentifierType identifierType, String identifierValue, String dateOfBirth) {
        Optional<NiraPerson> person = niraClient.lookup(identifierValue, dateOfBirth, identifierType.name());
        if (person.isEmpty()) {
            return unmatched("NIRA", "No NIRA person matches this identifier and date of birth");
        }
        NiraPerson found = person.get();
        String displayName = (found.givenNames() + " " + found.surname()).trim();
        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("authority", "NIRA");
        identity.put("nationalIdentificationNumber", found.nationalIdentificationNumber());
        identity.put("identifierType", found.identifierType());
        identity.put("givenNames", found.givenNames());
        identity.put("surname", found.surname());
        identity.put("displayName", displayName);
        identity.put("dateOfBirth", found.dateOfBirth());
        identity.put("gender", found.gender());
        identity.put("nationality", found.nationality());
        identity.put("email", found.email());
        identity.put("telephone", found.telephone());
        return IdentityPrefill.builder()
                .authority("NIRA")
                .matched(true)
                .message("NIRA identity matched")
                .displayName(displayName)
                .email(found.email())
                .telephone(found.telephone())
                .identity(identity)
                .build();
    }

    private IdentityPrefill fromUrsb(String registrationNumber, String registrationDate) {
        Optional<UrsbEntity> entity = ursbClient.lookup(registrationNumber, registrationDate);
        if (entity.isEmpty()) {
            return unmatched("URSB", "No URSB entity matches this registration number and registration date");
        }
        UrsbEntity found = entity.get();
        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("authority", "URSB");
        identity.put("registrationNumber", found.registrationNumber());
        identity.put("entityName", found.entityName());
        identity.put("displayName", found.entityName());
        identity.put("entityType", found.entityType());
        identity.put("registrationDate", found.registrationDate());
        identity.put("registrationStatus", found.registrationStatus());
        identity.put("email", found.email());
        identity.put("telephone", found.telephone());
        return IdentityPrefill.builder()
                .authority("URSB")
                .matched(true)
                .message("URSB entity registration matched")
                .displayName(found.entityName())
                .email(found.email())
                .telephone(found.telephone())
                .identity(identity)
                .build();
    }

    private static IdentityPrefill unmatched(String authority, String message) {
        return IdentityPrefill.builder()
                .authority(authority)
                .matched(false)
                .message(message)
                .build();
    }

    private static String authority(IdentifierType identifierType) {
        return switch (identifierType) {
            case NIN, AIN -> "NIRA";
            case ERN -> "URSB";
            case SYS -> null;
        };
    }
}