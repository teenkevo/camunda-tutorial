package com.example.taxpayer.dto;

import java.util.List;
import lombok.Data;

@Data
public class TaxpayerProcessStepResponse {
    private List<String> taxTypes;
    private List<String> registeredTaxTypes;
    private Boolean valid;
    private String validationMessage;
    private String lastRegistrationRef;
    private String tin;
    private String registrationStatus;
}
