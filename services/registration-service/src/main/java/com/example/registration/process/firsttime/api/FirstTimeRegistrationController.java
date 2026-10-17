package com.example.registration.process.firsttime.api;

import com.example.camunda.client.CamundaProcessClient;
import com.example.registration.process.firsttime.dto.FirstTimeApplicationResponse;
import com.example.registration.process.firsttime.dto.FirstTimeContactsRequest;
import com.example.registration.process.firsttime.dto.FirstTimeIdentityRequest;
import com.example.registration.process.firsttime.dto.FirstTimeIdentityResponse;
import com.example.registration.process.firsttime.dto.FirstTimeRegistrationRequest;
import com.example.registration.process.firsttime.dto.FirstTimeRegistrationStartResponse;
import com.example.registration.process.firsttime.dto.IndividualContactsRequest;
import com.example.registration.process.firsttime.dto.IndividualIdentityRequest;
import com.example.registration.process.firsttime.dto.IndividualIdentityResponse;
import com.example.registration.process.firsttime.dto.IndividualRegistrationRequest;
import com.example.registration.process.firsttime.dto.NonIndividualContactsRequest;
import com.example.registration.process.firsttime.dto.NonIndividualIdentityRequest;
import com.example.registration.process.firsttime.dto.NonIndividualIdentityResponse;
import com.example.registration.process.firsttime.dto.NonIndividualRegistrationRequest;
import com.example.registration.process.firsttime.service.FirstTimeRegistrationService;
import com.example.registration.process.firsttime.service.ResolvedRegistration;
import com.example.registration.shared.domain.IdentifierType;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/registrations/first-time")
public class FirstTimeRegistrationController {

    public static final String PROCESS_DEFINITION_KEY = "reg-first-time-taxpayer";

    private final CamundaProcessClient camundaProcessClient;
    private final FirstTimeRegistrationService firstTimeRegistrationService;

    public FirstTimeRegistrationController(
            CamundaProcessClient camundaProcessClient,
            FirstTimeRegistrationService firstTimeRegistrationService
    ) {
        this.camundaProcessClient = camundaProcessClient;
        this.firstTimeRegistrationService = firstTimeRegistrationService;
    }

    @PostMapping("/individual/identity")
    public IndividualIdentityResponse individualIdentity(@Valid @RequestBody IndividualIdentityRequest request) {
        return toIndividual(firstTimeRegistrationService.lookupIdentity(toIdentity(request)));
    }

    @PostMapping("/individual/contacts")
    public IndividualIdentityResponse individualContacts(@Valid @RequestBody IndividualContactsRequest request) {
        return toIndividual(firstTimeRegistrationService.supplyContacts(toContacts(request)));
    }

    @PostMapping("/individual")
    @ResponseStatus(HttpStatus.CREATED)
    public FirstTimeRegistrationStartResponse registerIndividual(
            @Valid @RequestBody IndividualRegistrationRequest request
    ) {
        return start(firstTimeRegistrationService.prepareStart(toRegistration(request)));
    }

    @PostMapping("/non-individual/identity")
    public NonIndividualIdentityResponse nonIndividualIdentity(
            @Valid @RequestBody NonIndividualIdentityRequest request
    ) {
        return toNonIndividual(firstTimeRegistrationService.lookupIdentity(toIdentity(request)));
    }

    @PostMapping("/non-individual/contacts")
    public NonIndividualIdentityResponse nonIndividualContacts(
            @Valid @RequestBody NonIndividualContactsRequest request
    ) {
        return toNonIndividual(firstTimeRegistrationService.supplyContacts(toContacts(request)));
    }

    @PostMapping("/non-individual")
    @ResponseStatus(HttpStatus.CREATED)
    public FirstTimeRegistrationStartResponse registerNonIndividual(
            @Valid @RequestBody NonIndividualRegistrationRequest request
    ) {
        return start(firstTimeRegistrationService.prepareStart(toRegistration(request)));
    }

    @GetMapping
    public List<FirstTimeApplicationResponse> list() {
        return firstTimeRegistrationService.findAll();
    }

    @GetMapping("/{drn}")
    public FirstTimeApplicationResponse get(@PathVariable("drn") String drn) {
        return firstTimeRegistrationService.getByDrn(drn);
    }

    private FirstTimeRegistrationStartResponse start(ResolvedRegistration resolved) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("identifierType", resolved.identifierType().name());
        variables.put("identifierValue", resolved.identifierValue());
        variables.put("dateOfBirthOrIncorp", resolved.dateOfBirthOrIncorp());
        variables.put("email", resolved.email());
        variables.put("telephone", resolved.telephone());
        variables.put("taxpayerName", resolved.taxpayerName());
        variables.put("taxTypes", resolved.taxTypes());
        variables.put("taxTypesInput", resolved.taxTypes().stream().collect(Collectors.joining(", ")));

        String processInstanceId = camundaProcessClient.startProcess(PROCESS_DEFINITION_KEY, variables);
        Map<String, Object> document = firstTimeRegistrationService.findByProcessInstanceId(processInstanceId);

        FirstTimeRegistrationStartResponse response = new FirstTimeRegistrationStartResponse();
        response.setDrn(String.valueOf(document.get("drn")));
        response.setProcessInstanceId(processInstanceId);
        response.setProcessDefinitionKey(PROCESS_DEFINITION_KEY);
        response.setIdentifierType(resolved.identifierType());
        response.setIdentifierValue(resolved.identifierValue());
        response.setTaxpayerName(resolved.taxpayerName());
        response.setTaxTypes(resolved.taxTypes());
        response.setMessage(
                "Registration document created. Complete Registration Officer review in Camunda Tasklist."
        );
        return response;
    }

    private static IdentifierType individualType(IndividualIdentityRequest request) {
        boolean nin = hasText(request.getNin());
        boolean ain = hasText(request.getAin());
        if (nin == ain) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Individual registration requires either nin or ain, and dateOfBirth"
            );
        }
        return nin ? IdentifierType.NIN : IdentifierType.AIN;
    }

    private static String individualValue(IndividualIdentityRequest request) {
        return individualType(request) == IdentifierType.NIN ? request.getNin().trim() : request.getAin().trim();
    }

    private static FirstTimeIdentityRequest toIdentity(IndividualIdentityRequest request) {
        FirstTimeIdentityRequest identity = new FirstTimeIdentityRequest();
        identity.setIdentifierType(individualType(request));
        identity.setIdentifierValue(individualValue(request));
        identity.setDateOfBirthOrIncorp(request.getDateOfBirth());
        return identity;
    }

    private static FirstTimeContactsRequest toContacts(IndividualContactsRequest request) {
        FirstTimeContactsRequest contacts = new FirstTimeContactsRequest();
        contacts.setIdentifierType(individualType(request));
        contacts.setIdentifierValue(individualValue(request));
        contacts.setDateOfBirthOrIncorp(request.getDateOfBirth());
        contacts.setEmail(request.getEmail());
        contacts.setTelephone(request.getTelephone());
        return contacts;
    }

    private static FirstTimeRegistrationRequest toRegistration(IndividualRegistrationRequest request) {
        FirstTimeRegistrationRequest registration = new FirstTimeRegistrationRequest();
        registration.setIdentifierType(individualType(request));
        registration.setIdentifierValue(individualValue(request));
        registration.setDateOfBirthOrIncorp(request.getDateOfBirth());
        registration.setEmail(request.getEmail());
        registration.setTelephone(request.getTelephone());
        registration.setOtpCode(request.getOtpCode());
        registration.setTaxTypes(request.getTaxTypes());
        return registration;
    }

    private static FirstTimeIdentityRequest toIdentity(NonIndividualIdentityRequest request) {
        FirstTimeIdentityRequest identity = new FirstTimeIdentityRequest();
        identity.setIdentifierType(IdentifierType.ERN);
        identity.setIdentifierValue(request.getErn());
        identity.setDateOfBirthOrIncorp(request.getDateOfIncorporation());
        return identity;
    }

    private static FirstTimeContactsRequest toContacts(NonIndividualContactsRequest request) {
        FirstTimeContactsRequest contacts = new FirstTimeContactsRequest();
        contacts.setIdentifierType(IdentifierType.ERN);
        contacts.setIdentifierValue(request.getErn());
        contacts.setDateOfBirthOrIncorp(request.getDateOfIncorporation());
        contacts.setEmail(request.getEmail());
        contacts.setTelephone(request.getTelephone());
        return contacts;
    }

    private static FirstTimeRegistrationRequest toRegistration(NonIndividualRegistrationRequest request) {
        FirstTimeRegistrationRequest registration = new FirstTimeRegistrationRequest();
        registration.setIdentifierType(IdentifierType.ERN);
        registration.setIdentifierValue(request.getErn());
        registration.setDateOfBirthOrIncorp(request.getDateOfIncorporation());
        registration.setEmail(request.getEmail());
        registration.setTelephone(request.getTelephone());
        registration.setOtpCode(request.getOtpCode());
        registration.setTaxTypes(request.getTaxTypes());
        return registration;
    }

    private static IndividualIdentityResponse toIndividual(FirstTimeIdentityResponse source) {
        IndividualIdentityResponse response = new IndividualIdentityResponse();
        if (source.getIdentifierType() == IdentifierType.AIN) {
            response.setAin(source.getIdentifierValue());
        } else {
            response.setNin(source.getIdentifierValue());
        }
        response.setDateOfBirth(source.getDateOfBirthOrIncorp());
        response.setIdentityMatched(source.getIdentityMatched());
        response.setIdentityAuthority(source.getIdentityAuthority());
        response.setTaxpayerName(source.getTaxpayerName());
        response.setEmail(source.getEmail());
        response.setTelephone(source.getTelephone());
        response.setEmailFromAuthority(source.isEmailFromAuthority());
        response.setTelephoneFromAuthority(source.isTelephoneFromAuthority());
        response.setContactsRequired(source.isContactsRequired());
        response.setOtpSent(source.isOtpSent());
        response.setMessage(source.getMessage());
        return response;
    }

    private static NonIndividualIdentityResponse toNonIndividual(FirstTimeIdentityResponse source) {
        NonIndividualIdentityResponse response = new NonIndividualIdentityResponse();
        response.setErn(source.getIdentifierValue());
        response.setDateOfIncorporation(source.getDateOfBirthOrIncorp());
        response.setIdentityMatched(source.getIdentityMatched());
        response.setIdentityAuthority(source.getIdentityAuthority());
        response.setTaxpayerName(source.getTaxpayerName());
        response.setEmail(source.getEmail());
        response.setTelephone(source.getTelephone());
        response.setEmailFromAuthority(source.isEmailFromAuthority());
        response.setTelephoneFromAuthority(source.isTelephoneFromAuthority());
        response.setContactsRequired(source.isContactsRequired());
        response.setOtpSent(source.isOtpSent());
        response.setMessage(source.getMessage());
        return response;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
