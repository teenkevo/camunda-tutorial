package com.example.camundatutorial.taxpayer;

import java.util.List;

public class TaxpayerRegistrationRequest {

    private String taxpayerName;
    private String nationalId;
    private List<String> taxTypes;

    public String getTaxpayerName() {
        return taxpayerName;
    }

    public void setTaxpayerName(String taxpayerName) {
        this.taxpayerName = taxpayerName;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public List<String> getTaxTypes() {
        return taxTypes;
    }

    public void setTaxTypes(List<String> taxTypes) {
        this.taxTypes = taxTypes;
    }
}
