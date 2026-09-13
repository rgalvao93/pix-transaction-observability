package com.rodrigogalvao.transaction.controller;

import com.rodrigogalvao.transaction.dto.ErrorResponse;
import com.rodrigogalvao.transaction.dto.TransactionRequest;
import com.rodrigogalvao.transaction.dto.TransactionResponse;
import com.rodrigogalvao.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/transactions")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Operation(
            summary = "Cria e processa uma transação Pix (cash-in ou cash-out)",
            description = "Valida o payload, aplica idempotência por transactionId e orquestra o parceiro externo. " +
                    "Resposta sempre HTTP 200 com o status de negócio (PROCESSED/FAILED/ERROR) no corpo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Transação processada — status de negócio no corpo",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Payload inválido (validação)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido")
    })
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody TransactionRequest request) {

        return ResponseEntity.ok(transactionService.process(request));
    }
}