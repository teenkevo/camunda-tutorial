package com.example.taxpayer.dto;

import java.util.List;
import lombok.Data;

@Data
public class TaxpayerProcessRequest {
    private String processInstanceId;
    private String taxpayerName;
    private String nationalId;
    private Object taxTypesInput;
    private Object taxTypes;
    private String taxType;
    private List<String> registeredTaxTypes;
}
