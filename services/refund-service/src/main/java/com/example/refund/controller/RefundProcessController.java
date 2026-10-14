package com.example.refund.controller;

import com.example.refund.dto.CreateRefundProcessRequest;
import com.example.refund.dto.NotifyRefundRequest;
import com.example.refund.dto.RefundProcessStepResponse;
import com.example.refund.service.RefundApplicationService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal APIs invoked by shared Camunda delegates.
 */
@RestController
@RequestMapping("/internal/refund-process")
public class RefundProcessController {

    private final RefundApplicationService refundApplicationService;

    public RefundProcessController(RefundApplicationService refundApplicationService) {
        this.refundApplicationService = refundApplicationService;
    }

    @PostMapping("/create")
    public RefundProcessStepResponse create(@RequestBody CreateRefundProcessRequest request) {
        return refundApplicationService.createFromProcess(request);
    }

    @PostMapping("/{applicationId}/assign-drn-station")
    public RefundProcessStepResponse assign(@PathVariable("applicationId") String applicationId) {
        return refundApplicationService.assignDrnAndStation(applicationId);
    }

    @PostMapping("/{applicationId}/check-risk")
    public RefundProcessStepResponse checkRisk(@PathVariable("applicationId") String applicationId) {
        return refundApplicationService.checkRisk(applicationId);
    }

    @PostMapping("/{applicationId}/auto-approve")
    public RefundProcessStepResponse autoApprove(@PathVariable("applicationId") String applicationId) {
        return refundApplicationService.autoApprove(applicationId);
    }

    @PostMapping("/{applicationId}/notify")
    public RefundProcessStepResponse notifyTaxpayer(
            @PathVariable("applicationId") String applicationId,
            @RequestBody NotifyRefundRequest request
    ) {
        return refundApplicationService.notifyTaxpayer(applicationId, request);
    }
}
