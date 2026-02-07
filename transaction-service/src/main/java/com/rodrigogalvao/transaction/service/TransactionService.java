package com.rodrigogalvao.transaction.service;

import com.rodrigogalvao.transaction.model.TransactionRequest;
import com.rodrigogalvao.transaction.model.TransactionResponse;
import com.rodrigogalvao.transaction.model.TransactionStatus;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    public TransactionResponse process(TransactionRequest request) {

        return new TransactionResponse(
                request.getTransactionId(),
                TransactionStatus.PROCESSED.name(),
                null
        );
    }
}
