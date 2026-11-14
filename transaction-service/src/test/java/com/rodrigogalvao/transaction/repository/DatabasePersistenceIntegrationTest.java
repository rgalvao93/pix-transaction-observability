package com.rodrigogalvao.transaction.repository;

import com.rodrigogalvao.transaction.dto.TransactionResponse;
import com.rodrigogalvao.transaction.model.TransactionStatus;
import com.rodrigogalvao.transaction.model.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DatabasePersistenceIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private JpaTransactionRepository jpaTransactionRepository;

    @Autowired
    private DailyLimitTracker dailyLimitTracker;

    @Test
    void persistsTransactionDetailsAndResponseForIdempotency() {
        String transactionId = "txn-" + UUID.randomUUID();
        TransactionResponse response =
                new TransactionResponse(transactionId, TransactionStatus.FAILED.name(), "saldo insuficiente");

        transactionRepository.save(transactionId, TransactionType.CASH_OUT, "acc-db",
                new BigDecimal("125.50"), response);

        assertThat(transactionRepository.findByTransactionId(transactionId))
                .hasValueSatisfying(saved -> {
                    assertThat(saved.getTransactionId()).isEqualTo(transactionId);
                    assertThat(saved.getStatus()).isEqualTo(TransactionStatus.FAILED.name());
                    assertThat(saved.getReason()).isEqualTo("saldo insuficiente");
                });
        assertThat(jpaTransactionRepository.findById(transactionId))
                .hasValueSatisfying(saved -> {
                    assertThat(saved.getType()).isEqualTo(TransactionType.CASH_OUT);
                    assertThat(saved.getAccountId()).isEqualTo("acc-db");
                    assertThat(saved.getAmount()).isEqualByComparingTo("125.50");
                });
    }

    @Test
    void reservesDailyLimitPersistentlyAndRejectsAmountsOverTheLimit() {
        String accountId = "acc-limit-" + UUID.randomUUID();

        assertThat(dailyLimitTracker.tryReserve(accountId, new BigDecimal("3000.00"))).isTrue();
        assertThat(dailyLimitTracker.tryReserve(accountId, new BigDecimal("2000.00"))).isTrue();
        assertThat(dailyLimitTracker.tryReserve(accountId, new BigDecimal("0.01"))).isFalse();
    }

    @Test
    void serializesConcurrentFirstReservationsForTheSameAccount() throws Exception {
        String accountId = "acc-concurrent-" + UUID.randomUUID();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> {
                start.await();
                return dailyLimitTracker.tryReserve(accountId, new BigDecimal("3000.00"));
            });
            Future<Boolean> second = executor.submit(() -> {
                start.await();
                return dailyLimitTracker.tryReserve(accountId, new BigDecimal("3000.00"));
            });
            start.countDown();

            assertThat(first.get()).isNotEqualTo(second.get());
        } finally {
            executor.shutdownNow();
        }
    }
}
