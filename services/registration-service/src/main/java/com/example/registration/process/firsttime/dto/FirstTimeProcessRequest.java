package com.example.registration.process.firsttime.dto;

import java.util.List;
import lombok.Data;

/**
 * Payload exchanged between Camunda delegates and internal process APIs.
 */
@Data
public class FirstTimeProcessRequest {

    private String drn;
    private String processInstanceId;
    private String identifierType;
    private String identifierValue;
    private String taxpayerName;
    private String dateOfBirthOrIncorp;
    private String email;
    private String telephone;
    private Object taxTypes;
    private Object taxTypesInput;
    private String taxType;
    private List<String> registeredTaxTypes;
    private Boolean approved;
    private String reviewComment;
}
