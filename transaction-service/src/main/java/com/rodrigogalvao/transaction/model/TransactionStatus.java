package com.rodrigogalvao.transaction.model;

public enum TransactionStatus {

    PROCESSED, // transação processada com sucesso
    FAILED,    // falha de negócio (ex: saldo insuficiente)
    ERROR      // erro técnico / instabilidade
}
