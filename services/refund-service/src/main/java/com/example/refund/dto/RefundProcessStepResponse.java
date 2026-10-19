package com.example.refund.dto;

import java.util.List;
import lombok.Data;

@Data
public class RefundProcessStepResponse {
    private String applicationStatus;
    private String createdAt;
    private List<String> taxTypes;
    private String drn;
    private String station;
    private String riskLevel;
    private Boolean approved;
    private String reviewComment;
    private String notificationMessage;
    private String decision;
}
