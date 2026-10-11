package com.example.refund.dto;

import lombok.Data;

@Data
public class RefundProcessStepResponse {
    private String applicationId;
    private String applicationStatus;
    private String createdAt;
    private String drn;
    private String station;
    private String riskLevel;
    private Boolean approved;
    private String reviewComment;
    private String notificationMessage;
    private String decision;
}
