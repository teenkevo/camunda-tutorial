package com.example.registration.process.firsttime.service;

import com.example.registration.client.DocumentClient;
import com.example.registration.integration.identity.IdentityLookup;
import com.example.registration.integration.identity.IdentityPrefill;
import com.example.registration.integration.otp.OtpService;
import com.example.registration.process.firsttime.dto.FirstTimeApplicationResponse;
import com.example.registration.process.firsttime.dto.FirstTimeContactsRequest;
import com.example.registration.process.firsttime.dto.FirstTimeIdentityRequest;
import com.example.registration.process.firsttime.dto.FirstTimeIdentityResponse;
import com.example.registration.process.firsttime.dto.FirstTimeProcessRequest;
import com.example.registration.process.firsttime.dto.FirstTimeProcessStepResponse;
import com.example.registration.process.firsttime.dto.FirstTimeRegistrationRequest;
import com.example.registration.shared.domain.IdentifierType;
import com.example.registration.shared.domain.LifecycleState;
import com.example.registration.shared.entity.TaxpayerEntity;
import com.example.registration.shared.service.TaxpayerRegistryService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FirstTimeRegistrationService {

    static final List<String> DOCUMENT_TYPES = List.of(
            "INDIVIDUAL_REGISTRATION",
            "NON_INDIVIDUAL_REGISTRATION"
    );

    private static final Set<String> SUPPORTED_TAX_TYPES = Set.of(
            "INCOME_TAX", "VAT", "PAYE", "WHT", "RENTAL", "LED", "GAMING", "STAMP_DUTY", "CIT"
    );

    private final DocumentClient documentClient;
    private final TaxpayerRegistryService taxpayerRegistryService;
    private final IdentityLookup identityLookup;
    private final OtpService otpService;

    public FirstTimeRegistrationService(
            DocumentClient documentClient,
            TaxpayerRegistryService taxpayerRegistryService,
            IdentityLookup identityLookup,
            OtpService otpService
    ) {
        this.documentClient = documentClient;
        this.taxpayerRegistryService = taxpayerRegistryService;
        this.identityLookup = identityLookup;
        this.otpService = otpService;
    }

    public FirstTimeIdentityResponse lookupIdentity(FirstTimeIdentityRequest request) {
        IdentityPrefill prefill = requirePrefill(request);
        ensureNotAlreadyActive(request.getIdentifierType(), request);
        ContactChannels channels = channels(prefill, null, null);
        if (channels.complete()) {
            otpService.send(channels.email(), channels.telephone());
        }
        return identityResponse(request, prefill, channels, channels.complete());
    }

    public FirstTimeIdentityResponse supplyContacts(FirstTimeContactsRequest request) {
        IdentityPrefill prefill = requirePrefill(request);
        ensureNotAlreadyActive(request.getIdentifierType(), request);
        ContactChannels channels = channels(prefill, request.getEmail(), request.getTelephone());
        if (!channels.complete()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, missingContactsMessage(prefill, channels));
        }
        otpService.send(channels.email(), channels.telephone());
        return identityResponse(request, prefill, channels, true);
    }

    public ResolvedRegistration prepareStart(FirstTimeRegistrationRequest request) {
        IdentityPrefill prefill = requirePrefill(request);
        ensureNotAlreadyActive(request.getIdentifierType(), request);
        ContactChannels channels = channels(prefill, request.getEmail(), request.getTelephone());
        if (!channels.complete()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, missingContactsMessage(prefill, channels));
        }
        if (!otpService.verify(channels.email(), channels.telephone(), request.getOtpCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP verification failed");
        }
        List<String> taxTypes = normalizeTaxTypes(request.getTaxTypes());
        validateSupportedTaxTypes(taxTypes);
        return new ResolvedRegistration(
                request.getIdentifierType(),
                normalizeIdentifier(request.getIdentifierValue()),
                request.getDateOfBirthOrIncorp().trim(),
                prefill.getDisplayName(),
                channels.email(),
                channels.telephone(),
                taxTypes
        );
    }

    private IdentityPrefill requirePrefill(FirstTimeIdentityRequest request) {
        if (request.getIdentifierType() == IdentifierType.SYS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A first-time application uses NIN and date of birth, or ERN and date of incorporation"
            );
        }
        IdentityPrefill prefill = identityLookup.lookup(
                request.getIdentifierType(),
                normalizeIdentifier(request.getIdentifierValue()),
                request.getDateOfBirthOrIncorp().trim()
        );
        if (!prefill.isMatched()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, prefill.getMessage());
        }
        return prefill;
    }

    private void ensureNotAlreadyActive(IdentifierType identifierType, FirstTimeIdentityRequest request) {
        if (taxpayerRegistryService.isAlreadyActive(identifierType, normalizeIdentifier(request.getIdentifierValue()))) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Taxpayer already has an active registration for this identifier"
            );
        }
    }

    private ContactChannels channels(IdentityPrefill prefill, String suppliedEmail, String suppliedTelephone) {
        String email = textOrNull(prefill.getEmail());
        String telephone = textOrNull(prefill.getTelephone());
        boolean emailFromAuthority = email != null;
        boolean telephoneFromAuthority = telephone != null;
        if (!emailFromAuthority) {
            email = normalizeEmail(suppliedEmail);
        }
        if (!telephoneFromAuthority) {
            telephone = normalizeTelephone(suppliedTelephone);
        }
        return new ContactChannels(email, telephone, emailFromAuthority, telephoneFromAuthority);
    }

    private FirstTimeIdentityResponse identityResponse(
            FirstTimeIdentityRequest request,
            IdentityPrefill prefill,
            ContactChannels channels,
            boolean otpSent
    ) {
        FirstTimeIdentityResponse response = new FirstTimeIdentityResponse();
        response.setIdentifierType(request.getIdentifierType());
        response.setIdentifierValue(normalizeIdentifier(request.getIdentifierValue()));
        response.setDateOfBirthOrIncorp(request.getDateOfBirthOrIncorp().trim());
        response.setIdentityMatched(true);
        response.setIdentityAuthority(prefill.getAuthority());
        response.setTaxpayerName(prefill.getDisplayName());
        response.setEmail(channels.email());
        response.setTelephone(channels.telephone());
        response.setEmailFromAuthority(channels.emailFromAuthority());
        response.setTelephoneFromAuthority(channels.telephoneFromAuthority());
        response.setContactsRequired(!channels.complete());
        response.setOtpSent(otpSent);
        response.setMessage(otpSent
                ? "OTP sent to the email and telephone for this " + prefill.getAuthority() + " record"
                : missingContactsMessage(prefill, channels));
        return response;
    }

    private String missingContactsMessage(IdentityPrefill prefill, ContactChannels channels) {
        String authority = prefill.getAuthority() == null ? "The authority" : prefill.getAuthority();
        if (channels.email() == null && channels.telephone() == null) {
            return authority + " did not return an email or telephone. Add both before continuing.";
        }
        if (channels.email() == null) {
            return authority + " did not return an email. Add an email before continuing.";
        }
        return authority + " did not return a telephone. Add a telephone before continuing.";
    }

    private String normalizeIdentifier(String identifierValue) {
        return identifierValue.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String trimmed = email.trim();
        if (!trimmed.contains("@") || trimmed.startsWith("@") || trimmed.endsWith("@")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email address is not valid");
        }
        return trimmed;
    }

    private String normalizeTelephone(String telephone) {
        if (telephone == null || telephone.isBlank()) {
            return null;
        }
        return telephone.trim();
    }

    private String textOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record ContactChannels(
            String email,
            String telephone,
            boolean emailFromAuthority,
            boolean telephoneFromAuthority
    ) {
        boolean complete() {
            return email != null && telephone != null;
        }
    }

    public Map<String, Object> findByProcessInstanceId(String processInstanceId) {
        return documentClient.getByProcessInstanceId(processInstanceId);
    }

    public FirstTimeProcessStepResponse validateIdentity(FirstTimeProcessRequest request) {
        Map<String, Object> document = requireDocument(request);
        Map<String, Object> payload = payload(document);
        IdentifierType identifierType = IdentifierType.valueOf(text(payload, "identifierType"));
        IdentityPrefill identity = identityLookup.lookup(
                identifierType,
                text(payload, "identifierValue"),
                text(payload, "dateOfBirthOrIncorp")
        );

        Map<String, Object> patch = new HashMap<>();
        patch.put("identityMatched", identity.isMatched());
        patch.put("identityAuthority", identity.getAuthority());
        patch.put("validationMessage", identity.getMessage());
        if (identity.isMatched()) {
            patch.put("identity", identity.getIdentity());
            if (text(payload, "taxpayerName") == null) {
                patch.put("taxpayerName", identity.getDisplayName());
            }
            if (text(payload, "email") == null) {
                patch.put("email", identity.getEmail());
            }
            if (text(payload, "telephone") == null) {
                patch.put("telephone", identity.getTelephone());
            }
        }
        Map<String, Object> updated = merge(text(document, "drn"), patch, "IDENTITY_VALIDATED");
        Map<String, Object> updatedPayload = payload(updated);

        FirstTimeProcessStepResponse response = new FirstTimeProcessStepResponse();
        response.setIdentityMatched(identity.isMatched());
        response.setIdentityAuthority(identity.getAuthority());
        response.setValid(identity.isMatched());
        response.setValidationMessage(identity.getMessage());
        response.setTaxpayerName(text(updatedPayload, "taxpayerName"));
        response.setEmail(text(updatedPayload, "email"));
        response.setTelephone(text(updatedPayload, "telephone"));
        response.setTaxTypes(stringList(updatedPayload.get("taxTypes")));
        return response;
    }

    public FirstTimeProcessStepResponse prepareTaxTypes(FirstTimeProcessRequest request) {
        Map<String, Object> document = requireDocument(request);
        Map<String, Object> payload = payload(document);
        List<String> taxTypes = normalizeTaxTypes(request.getTaxTypesInput());
        if (taxTypes.isEmpty()) {
            taxTypes = normalizeTaxTypes(request.getTaxTypes());
        }
        if (taxTypes.isEmpty()) {
            taxTypes = stringList(payload.get("taxTypes"));
        }
        validateSupportedTaxTypes(taxTypes);

        Map<String, Object> patch = new HashMap<>();
        patch.put("taxTypes", taxTypes);
        patch.put("registeredTaxTypes", List.of());
        merge(text(document, "drn"), patch, "TAX_TYPES_PREPARED");

        FirstTimeProcessStepResponse response = new FirstTimeProcessStepResponse();
        response.setTaxTypes(taxTypes);
        response.setRegisteredTaxTypes(new ArrayList<>());
        response.setValid(true);
        return response;
    }

    public FirstTimeProcessStepResponse validateApplication(FirstTimeProcessRequest request) {
        Map<String, Object> document = requireDocument(request);
        Map<String, Object> payload = payload(document);
        List<String> taxTypes = stringList(payload.get("taxTypes"));
        IdentifierType identifierType = IdentifierType.valueOf(text(payload, "identifierType"));
        String identifier = text(payload, "identifierValue");
        boolean valid = Boolean.TRUE.equals(payload.get("identityMatched"))
                && text(payload, "taxpayerName") != null
                && identifier != null
                && identifier.length() >= 5
                && !taxTypes.isEmpty()
                && !taxpayerRegistryService.isAlreadyActive(identifierType, identifier);

        String message = valid
                ? "First-time registration application is valid"
                : "NIRA or URSB match, taxpayer name, identifier, and at least one tax type are required; taxpayer must not already be active";
        merge(text(document, "drn"), Map.of("validationMessage", message), "APPLICATION_VALIDATED");

        FirstTimeProcessStepResponse response = new FirstTimeProcessStepResponse();
        response.setValid(valid);
        response.setValidationMessage(message);
        response.setTaxTypes(taxTypes);
        response.setTaxpayerName(text(payload, "taxpayerName"));
        return response;
    }

    public FirstTimeProcessStepResponse registerTaxType(FirstTimeProcessRequest request) {
        Map<String, Object> document = requireDocument(request);
        Map<String, Object> payload = payload(document);
        String taxType = String.valueOf(request.getTaxType()).trim().toUpperCase(Locale.ROOT);
        List<String> registered = new ArrayList<>(stringList(payload.get("registeredTaxTypes")));
        if (!registered.contains(taxType)) {
            registered.add(taxType);
        }
        Map<String, Object> updated = merge(
                text(document, "drn"),
                Map.of("registeredTaxTypes", registered),
                "TAX_TYPE_RECORDED"
        );
        String identifier = text(payload(updated), "identifierValue");

        FirstTimeProcessStepResponse response = new FirstTimeProcessStepResponse();
        response.setRegisteredTaxTypes(registered);
        response.setLastRegistrationRef(identifier + "-" + taxType);
        return response;
    }

    public FirstTimeProcessStepResponse finalizeRegistration(FirstTimeProcessRequest request) {
        Map<String, Object> document = requireDocument(request);
        if ("APPROVED".equals(text(document, "status")) && document.get("taxpayerId") != null) {
            FirstTimeProcessStepResponse response = new FirstTimeProcessStepResponse();
            response.setRegistrationStatus(LifecycleState.ACTIVE.name());
            response.setTaxpayerId(longValue(document.get("taxpayerId")));
            response.setTaxTypes(stringList(payload(document).get("registeredTaxTypes")));
            return response;
        }

        Map<String, Object> payload = payload(document);
        List<String> taxTypes = stringList(payload.get("registeredTaxTypes"));
        if (taxTypes.isEmpty()) {
            taxTypes = stringList(payload.get("taxTypes"));
        }
        TaxpayerEntity taxpayer = taxpayerRegistryService.activateTaxpayer(
                IdentifierType.valueOf(text(payload, "identifierType")),
                text(payload, "identifierValue"),
                text(payload, "taxpayerName"),
                text(payload, "email"),
                text(payload, "telephone"),
                taxTypes
        );

        Map<String, Object> body = new HashMap<>();
        body.put("status", "APPROVED");
        body.put("taxpayerId", taxpayer.getId());
        body.put("eventType", "APPROVED");
        body.put("payload", Map.of("approved", true));
        documentClient.update(text(document, "drn"), body);

        FirstTimeProcessStepResponse response = new FirstTimeProcessStepResponse();
        response.setRegistrationStatus(LifecycleState.ACTIVE.name());
        response.setTaxpayerId(taxpayer.getId());
        response.setTaxTypes(taxTypes);
        return response;
    }

    public FirstTimeProcessStepResponse rejectRegistration(FirstTimeProcessRequest request) {
        Map<String, Object> document = requireDocument(request);
        String drn = text(document, "drn");
        if (!"REJECTED".equals(text(document, "status"))) {
            Map<String, Object> body = new HashMap<>();
            body.put("status", "REJECTED");
            body.put("eventType", "REJECTED");
            body.put("payload", Map.of(
                    "approved", false,
                    "reviewComment", request.getReviewComment() == null ? "" : request.getReviewComment()
            ));
            documentClient.update(drn, body);
        }
        FirstTimeProcessStepResponse response = new FirstTimeProcessStepResponse();
        response.setRegistrationStatus("REJECTED");
        response.setValidationMessage(request.getReviewComment());
        return response;
    }

    public FirstTimeApplicationResponse getByDrn(String drn) {
        return toResponse(documentClient.getByDrn(drn));
    }

    public List<FirstTimeApplicationResponse> findAll() {
        List<FirstTimeApplicationResponse> applications = new ArrayList<>();
        for (String documentType : DOCUMENT_TYPES) {
            List<Map<String, Object>> documents = documentClient.list(documentType);
            if (documents != null) {
                documents.stream().map(this::toResponse).forEach(applications::add);
            }
        }
        return applications;
    }

    private Map<String, Object> requireDocument(FirstTimeProcessRequest request) {
        if (request.getDrn() == null || request.getDrn().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "drn is required");
        }
        return documentClient.getByDrn(request.getDrn());
    }

    private Map<String, Object> merge(String drn, Map<String, Object> payloadPatch, String eventType) {
        Map<String, Object> body = new HashMap<>();
        body.put("eventType", eventType);
        body.put("payload", payloadPatch);
        return documentClient.update(drn, body);
    }

    private void validateSupportedTaxTypes(List<String> taxTypes) {
        if (taxTypes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one tax type is required");
        }
        List<String> unsupported = taxTypes.stream().filter(type -> !SUPPORTED_TAX_TYPES.contains(type)).toList();
        if (!unsupported.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported tax types: " + unsupported + ". Supported: " + SUPPORTED_TAX_TYPES
            );
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> normalizeTaxTypes(Object raw) {
        if (raw == null) {
            return List.of();
        }
        Collection<String> values;
        if (raw instanceof Collection<?> collection) {
            values = collection.stream().map(String::valueOf).collect(Collectors.toList());
        } else {
            values = Arrays.asList(String.valueOf(raw).split(","));
        }
        return values.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .map(value -> "INCOME".equals(value) ? "INCOME_TAX" : value)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new
                ));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> payload(Map<String, Object> document) {
        Object payload = document.get("payload");
        if (payload instanceof Map<?, ?> map) {
            return new HashMap<>((Map<String, Object>) map);
        }
        return new HashMap<>();
    }

    private FirstTimeApplicationResponse toResponse(Map<String, Object> document) {
        Map<String, Object> payload = payload(document);
        FirstTimeApplicationResponse response = new FirstTimeApplicationResponse();
        response.setDrn(text(document, "drn"));
        response.setProcessInstanceId(text(document, "processInstanceId"));
        String identifierType = text(payload, "identifierType");
        if (identifierType != null) {
            response.setIdentifierType(IdentifierType.valueOf(identifierType));
        }
        response.setIdentifierValue(text(payload, "identifierValue"));
        response.setTaxpayerName(text(payload, "taxpayerName"));
        response.setEmail(text(payload, "email"));
        response.setTelephone(text(payload, "telephone"));
        response.setTaxTypes(stringList(payload.get("taxTypes")));
        response.setRegisteredTaxTypes(stringList(payload.get("registeredTaxTypes")));
        response.setStatus(text(document, "status"));
        Object identityMatched = payload.get("identityMatched");
        if (identityMatched instanceof Boolean matched) {
            response.setIdentityMatched(matched);
        }
        response.setIdentityAuthority(text(payload, "identityAuthority"));
        response.setValidationMessage(text(payload, "validationMessage"));
        Object approved = payload.get("approved");
        if (approved instanceof Boolean value) {
            response.setApproved(value);
        }
        response.setCreatedAt(instant(document.get("createdAt")));
        response.setUpdatedAt(instant(document.get("updatedAt")));
        return response;
    }

    private List<String> stringList(Object raw) {
        return normalizeTaxTypes(raw);
    }

    private String text(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() || "null".equals(text) ? null : text;
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? null : Long.valueOf(String.valueOf(value));
    }

    private Instant instant(Object value) {
        if (value == null) {
            return null;
        }
        return Instant.parse(String.valueOf(value));
    }
}
