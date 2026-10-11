package com.example.taxpayer.controller;

import com.example.taxpayer.dto.TaxpayerProcessRequest;
import com.example.taxpayer.dto.TaxpayerProcessStepResponse;
import com.example.taxpayer.service.TaxpayerRegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/taxpayer-process")
public class TaxpayerProcessController {

    private final TaxpayerRegistrationService taxpayerRegistrationService;

    public TaxpayerProcessController(TaxpayerRegistrationService taxpayerRegistrationService) {
        this.taxpayerRegistrationService = taxpayerRegistrationService;
    }

    @PostMapping("/prepare-tax-types")
    public TaxpayerProcessStepResponse prepare(@RequestBody TaxpayerProcessRequest request) {
        try {
            return taxpayerRegistrationService.prepareTaxTypes(request);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PostMapping("/validate")
    public TaxpayerProcessStepResponse validate(@RequestBody TaxpayerProcessRequest request) {
        return taxpayerRegistrationService.validate(request);
    }

    @PostMapping("/register-tax-type")
    public TaxpayerProcessStepResponse register(@RequestBody TaxpayerProcessRequest request) {
        return taxpayerRegistrationService.registerTaxType(request);
    }

    @PostMapping("/finalize")
    public TaxpayerProcessStepResponse finalizeRegistration(@RequestBody TaxpayerProcessRequest request) {
        return taxpayerRegistrationService.finalizeRegistration(request);
    }
}
