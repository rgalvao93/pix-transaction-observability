package com.rodrigogalvao.partnermock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "VerifyBalanceRequest", description = "Consulta de saldo/disponibilidade da conta")
public class VerifyBalanceRequest {

    @Schema(description = "Identificador da conta no parceiro", example = "acc-789")
    @NotBlank(message = "accountId is required")
    private String accountId;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}