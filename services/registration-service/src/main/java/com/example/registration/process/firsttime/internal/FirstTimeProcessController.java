package com.example.registration.process.firsttime.internal;

import com.example.registration.process.firsttime.dto.FirstTimeProcessRequest;
import com.example.registration.process.firsttime.dto.FirstTimeProcessStepResponse;
import com.example.registration.process.firsttime.service.FirstTimeRegistrationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal APIs invoked by shared Camunda delegates for first-time taxpayer registration.
 */
@RestController
@RequestMapping("/internal/reg-first-time")
public class FirstTimeProcessController {

    private final FirstTimeRegistrationService firstTimeRegistrationService;

    public FirstTimeProcessController(FirstTimeRegistrationService firstTimeRegistrationService) {
        this.firstTimeRegistrationService = firstTimeRegistrationService;
    }

    @PostMapping("/validate-identity")
    public FirstTimeProcessStepResponse validateIdentity(@RequestBody FirstTimeProcessRequest request) {
        return firstTimeRegistrationService.validateIdentity(request);
    }

    @PostMapping("/prepare-tax-types")
    public FirstTimeProcessStepResponse prepareTaxTypes(@RequestBody FirstTimeProcessRequest request) {
        return firstTimeRegistrationService.prepareTaxTypes(request);
    }

    @PostMapping("/validate")
    public FirstTimeProcessStepResponse validate(@RequestBody FirstTimeProcessRequest request) {
        return firstTimeRegistrationService.validateApplication(request);
    }

    @PostMapping("/register-tax-type")
    public FirstTimeProcessStepResponse registerTaxType(@RequestBody FirstTimeProcessRequest request) {
        return firstTimeRegistrationService.registerTaxType(request);
    }

    @PostMapping("/finalize")
    public FirstTimeProcessStepResponse finalizeRegistration(@RequestBody FirstTimeProcessRequest request) {
        return firstTimeRegistrationService.finalizeRegistration(request);
    }

    @PostMapping("/reject")
    public FirstTimeProcessStepResponse reject(@RequestBody FirstTimeProcessRequest request) {
        return firstTimeRegistrationService.rejectRegistration(request);
    }
}
