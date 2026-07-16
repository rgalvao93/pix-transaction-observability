package com.rodrigogalvao.transaction.repository;

import com.rodrigogalvao.transaction.dto.TransactionResponse;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class TransactionRepository {

    private final Map<String, TransactionResponse> processed = new ConcurrentHashMap<>();

    public Optional<TransactionResponse> findByTransactionId(String transactionId) {
        return Optional.ofNullable(processed.get(transactionId));
    }

    public void save(String transactionId, TransactionResponse response) {
        processed.put(transactionId, response);
    }
}
