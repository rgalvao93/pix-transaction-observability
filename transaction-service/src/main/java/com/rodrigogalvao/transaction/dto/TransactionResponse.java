package com.rodrigogalvao.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TransactionResponse", description = "Resultado do processamento da transação")
public class TransactionResponse {

    @Schema(description = "Identificador único da transação", example = "txn-123456")
    private String transactionId;

    @Schema(description = "Status de negócio da transação: PROCESSED (sucesso), FAILED (falha de negócio) ou ERROR (falha técnica)",
            example = "PROCESSED",
            allowableValues = {"PROCESSED", "FAILED", "ERROR"})
    private String status;

    @Schema(description = "Motivo da falha, quando aplicável (saldo insuficiente, limite diário excedido, falha de comunicação com o parceiro externo, ...)",
            example = "null")
    private String reason;

    public TransactionResponse() {
    }

    public TransactionResponse(String transactionId, String status, String reason) {
        this.transactionId = transactionId;
        this.status = status;
        this.reason = reason;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}