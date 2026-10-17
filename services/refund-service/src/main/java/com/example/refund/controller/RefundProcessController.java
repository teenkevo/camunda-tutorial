package com.example.refund.controller;

import com.example.refund.dto.NotifyRefundRequest;
import com.example.refund.dto.RefundProcessStepResponse;
import com.example.refund.service.RefundApplicationService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal APIs invoked by shared Camunda delegates after the document and DRN exist.
 */
@RestController
@RequestMapping("/internal/refund-process")
public class RefundProcessController {

    private final RefundApplicationService refundApplicationService;

    public RefundProcessController(RefundApplicationService refundApplicationService) {
        this.refundApplicationService = refundApplicationService;
    }

    @PostMapping("/{drn}/assign-station")
    public RefundProcessStepResponse assignStation(@PathVariable("drn") String drn) {
        return refundApplicationService.assignStation(drn);
    }

    @PostMapping("/{drn}/check-risk")
    public RefundProcessStepResponse checkRisk(@PathVariable("drn") String drn) {
        return refundApplicationService.checkRisk(drn);
    }

    @PostMapping("/{drn}/auto-approve")
    public RefundProcessStepResponse autoApprove(@PathVariable("drn") String drn) {
        return refundApplicationService.autoApprove(drn);
    }

    @PostMapping("/{drn}/notify")
    public RefundProcessStepResponse notifyTaxpayer(
            @PathVariable("drn") String drn,
            @RequestBody NotifyRefundRequest request
    ) {
        return refundApplicationService.notifyTaxpayer(drn, request);
    }
}
