package com.rodrigogalvao.partnermock.controller;

import com.rodrigogalvao.partnermock.dto.TransferRequest;
import com.rodrigogalvao.partnermock.dto.VerifyBalanceRequest;
import com.rodrigogalvao.partnermock.service.PartnerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/partner")
public class PartnerController {

    private final PartnerService partnerService;

    public PartnerController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    @PostMapping("/verify-balance")
    public ResponseEntity<PartnerService.BalanceResult> verifyBalance(@Valid @RequestBody VerifyBalanceRequest request) {
        return ResponseEntity.ok(partnerService.verifyBalance(request.getAccountId()));
    }

    @PostMapping("/transfer")
    public ResponseEntity<PartnerService.TransferResult> transfer(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(partnerService.transfer(request.getAccountId(), request.getAmount(), request.getType()));
    }
}
