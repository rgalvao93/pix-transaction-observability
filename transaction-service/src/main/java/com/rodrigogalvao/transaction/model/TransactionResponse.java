package com.rodrigogalvao.transaction.model;

public class TransactionResponse {

    private String transactionId;
    private String status;
    private String reason;

    public TransactionResponse(String transactionId, String status, String reason) {
        this.transactionId = transactionId;
        this.status = status;
        this.reason = reason;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }
}
