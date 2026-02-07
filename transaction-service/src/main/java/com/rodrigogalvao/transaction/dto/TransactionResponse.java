package com.rodrigogalvao.transaction.dto;

public class TransactionResponse {

    private String transactionId;
    private String status;
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
