package com.rodrigogalvao.transaction.dto;

import com.rodrigogalvao.transaction.model.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class TransactionRequest {

    @NotBlank(message = "transactionId is required")
    private String transactionId;

    @NotNull(message = "type is required and must be CASH_IN or CASH_OUT")
    private TransactionType type;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than zero")
    private BigDecimal amount;

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
