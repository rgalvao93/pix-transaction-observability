package com.rodrigogalvao.transaction.partner;

import com.rodrigogalvao.transaction.model.TransactionType;

import java.math.BigDecimal;

public interface ExternalPartnerClient {

    VerifyBalanceResult verifyBalance(String accountId);

    TransferResult transfer(String accountId, BigDecimal amount, TransactionType type);

    record VerifyBalanceResult(String accountId, BigDecimal balance, boolean available) {
    }

    record TransferResult(boolean success, String partnerReference) {
    }
}
