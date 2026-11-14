package com.rodrigogalvao.transaction.repository;

import com.rodrigogalvao.transaction.dto.TransactionResponse;
import com.rodrigogalvao.transaction.entity.Transaction;
import com.rodrigogalvao.transaction.model.TransactionStatus;
import com.rodrigogalvao.transaction.model.TransactionType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class TransactionRepository {

    private final JpaTransactionRepository jpaRepository;

    public TransactionRepository(JpaTransactionRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public Optional<TransactionResponse> findByTransactionId(String transactionId) {
        return jpaRepository.findByTransactionId(transactionId)
                .map(this::toResponse);
    }

    @Transactional
    public void save(String transactionId, TransactionType type, String accountId,
                     BigDecimal amount, TransactionResponse response) {
        jpaRepository.save(new Transaction(transactionId, type, accountId, amount,
                TransactionStatus.valueOf(response.getStatus()), response.getReason(), LocalDateTime.now()));
    }

    private TransactionResponse toResponse(Transaction entity) {
        return new TransactionResponse(
                entity.getTransactionId(),
                entity.getStatus().name(),
                entity.getReason());
    }
}
