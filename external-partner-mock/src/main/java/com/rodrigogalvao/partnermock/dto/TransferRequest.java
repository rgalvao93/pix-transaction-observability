package com.rodrigogalvao.partnermock.dto;

import com.rodrigogalvao.partnermock.model.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class TransferRequest {

    @NotBlank(message = "accountId is required")
    private String accountId;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than zero")
    private BigDecimal amount;

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
