package com.rodrigogalvao.partnermock.controller;

import com.rodrigogalvao.partnermock.dto.TransferRequest;
import com.rodrigogalvao.partnermock.dto.VerifyBalanceRequest;
import com.rodrigogalvao.partnermock.service.PartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/partner")
@SecurityRequirement(name = "bearerAuth")
public class PartnerController {

    private final PartnerService partnerService;

    public PartnerController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    @Operation(
            summary = "Verifica saldo e disponibilidade de uma conta",
            description = "Contas demo conhecidas: acc-123 (1000.00) e acc-789 (500.00). " +
                    "Qualquer outra accountId é tratada como desconhecida (available: false).")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Saldo e disponibilidade da conta",
                    content = @Content(schema = @Schema(implementation = PartnerService.BalanceResult.class))),
            @ApiResponse(responseCode = "400",
                    description = "Payload inválido (validação)",
                    content = @Content(schema = @Schema(implementation = com.rodrigogalvao.partnermock.dto.ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido")
    })
    @PostMapping("/verify-balance")
    public ResponseEntity<PartnerService.BalanceResult> verifyBalance(@Valid @RequestBody VerifyBalanceRequest request) {
        return ResponseEntity.ok(partnerService.verifyBalance(request.getAccountId()));
    }

    @Operation(
            summary = "Executa transferência aplicando o delta ao saldo",
            description = "CASH_IN soma, CASH_OUT subtrai. Uma falha simulada (taxa de falha configurável) " +
                    "retorna success: false com HTTP 200, não um erro HTTP.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Resultado da transferência (success: true/false)",
                    content = @Content(schema = @Schema(implementation = PartnerService.TransferResult.class))),
            @ApiResponse(responseCode = "400",
                    description = "Payload inválido (validação)",
                    content = @Content(schema = @Schema(implementation = com.rodrigogalvao.partnermock.dto.ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido")
    })
    @PostMapping("/transfer")
    public ResponseEntity<PartnerService.TransferResult> transfer(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(partnerService.transfer(request.getAccountId(), request.getAmount(), request.getType()));
    }
}