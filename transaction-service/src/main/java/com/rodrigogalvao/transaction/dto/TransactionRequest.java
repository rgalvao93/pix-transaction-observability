package com.rodrigogalvao.transaction.dto;

import com.rodrigogalvao.transaction.model.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(name = "TransactionRequest", description = "Payload de criação de uma transação Pix")
public class TransactionRequest {

    @Schema(description = "Identificador único do cliente. Reenvio do mesmo transactionId retorna a resposta já processada, sem chamar o parceiro novamente (idempotência).",
            example = "txn-123456")
    @NotBlank(message = "transactionId is required")
    private String transactionId;

    @Schema(description = "Tipo da operação: cash-in (crédito) ou cash-out (débito)", example = "CASH_IN")
    @NotNull(message = "type is required and must be CASH_IN or CASH_OUT")
    private TransactionType type;

    @Schema(description = "Valor da transação, maior que zero", example = "150.00")
    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than zero")
    private BigDecimal amount;

    @Schema(description = "Identificador da conta no parceiro externo", example = "acc-789")
    @NotBlank(message = "accountId is required")
    private String accountId;

    // getters e setters
    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}