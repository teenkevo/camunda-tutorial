package com.example.registration.process.firsttime.dto;

import java.util.List;
import lombok.Data;

@Data
public class FirstTimeProcessStepResponse {

    private Boolean valid;
    private Boolean identityMatched;
    private String identityAuthority;
    private String validationMessage;
    private String taxpayerName;
    private String email;
    private String telephone;
    private List<String> taxTypes;
    private List<String> registeredTaxTypes;
    private String lastRegistrationRef;
    private String registrationStatus;
    private Long taxpayerId;
}
