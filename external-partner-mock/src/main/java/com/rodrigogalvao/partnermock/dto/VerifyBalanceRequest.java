package com.rodrigogalvao.partnermock.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyBalanceRequest {

    @NotBlank(message = "accountId is required")
    private String accountId;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}
