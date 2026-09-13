package com.rodrigogalvao.partnermock.dto;

import com.rodrigogalvao.partnermock.model.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(name = "TransferRequest", description = "Pedido de transferência ao parceiro externo")
public class TransferRequest {

    @Schema(description = "Identificador da conta no parceiro", example = "acc-789")
    @NotBlank(message = "accountId is required")
    private String accountId;

    @Schema(description = "Valor a transferir, maior que zero", example = "150.00")
    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than zero")
    private BigDecimal amount;

    @Schema(description = "Tipo da operação: cash-in (crédito) ou cash-out (débito)", example = "CASH_IN")
    @NotNull(message = "type is required and must be CASH_IN or CASH_OUT")
    private TransactionType type;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }
}