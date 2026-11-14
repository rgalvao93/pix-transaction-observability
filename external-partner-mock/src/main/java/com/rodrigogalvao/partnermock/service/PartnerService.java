package com.rodrigogalvao.partnermock.service;

import com.rodrigogalvao.partnermock.model.TransactionType;
import com.rodrigogalvao.partnermock.entity.PartnerAccount;
import com.rodrigogalvao.partnermock.repository.PartnerAccountRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Random;

@Service
public class PartnerService {

    private static final Logger log = LoggerFactory.getLogger(PartnerService.class);

    private final PartnerAccountRepository accountRepository;
    private final long minLatencyMs;
    private final long maxLatencyMs;
    private final double failureRate;
    private final Random random;

    @Autowired
    public PartnerService(
            PartnerAccountRepository accountRepository,
            @Value("${partner.mock.latency.min-ms:100}") long minLatencyMs,
            @Value("${partner.mock.latency.max-ms:500}") long maxLatencyMs,
            @Value("${partner.mock.failure-rate:0.05}") double failureRate) {
        this(accountRepository, minLatencyMs, maxLatencyMs, failureRate, new Random());
    }

    PartnerService(PartnerAccountRepository accountRepository, long minLatencyMs, long maxLatencyMs,
                   double failureRate, Random random) {
        this.accountRepository = accountRepository;
        this.minLatencyMs = minLatencyMs;
        this.maxLatencyMs = maxLatencyMs;
        this.failureRate = failureRate;
        this.random = random;
    }

    public BalanceResult verifyBalance(String accountId) {
        simulateLatency();
        Optional<PartnerAccount> account = accountRepository.findById(accountId);
        if (account.isEmpty()) {
            log.warn("verify-balance: unknown account {}", accountId);
            return new BalanceResult(accountId, BigDecimal.ZERO, false);
        }
        return new BalanceResult(accountId, account.get().getBalance(), true);
    }

    @Transactional
    public TransferResult transfer(String accountId, BigDecimal amount, TransactionType type) {
        simulateLatency();
        if (random.nextDouble() < failureRate) {
            log.warn("transfer: simulated failure for account {}", accountId);
            return new TransferResult(false, null);
        }

        BigDecimal delta = type == TransactionType.CASH_IN ? amount : amount.negate();
        PartnerAccount account = accountRepository.findByAccountIdForUpdate(accountId)
                .orElseGet(() -> new PartnerAccount(accountId, BigDecimal.ZERO));
        account.setBalance(account.getBalance().add(delta));
        accountRepository.save(account);

        String reference = "psp-ref-" + Math.abs(random.nextLong());
        log.info("transfer: {} {} on account {} succeeded (ref={})", type, amount, accountId, reference);
        return new TransferResult(true, reference);
    }

    private void simulateLatency() {
        if (maxLatencyMs <= 0) {
            return;
        }
        long range = Math.max(0, maxLatencyMs - minLatencyMs);
        long delay = minLatencyMs + (range == 0 ? 0 : (long) (random.nextDouble() * range));
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public record BalanceResult(
            @Schema(description = "Identificador da conta", example = "acc-789") String accountId,
            @Schema(description = "Saldo atual da conta", example = "500.00") BigDecimal balance,
            @Schema(description = "Conta disponível no parceiro", example = "true") boolean available) {
    }

    public record TransferResult(
            @Schema(description = "Sucesso da transferência", example = "true") boolean success,
            @Schema(description = "Referência do parceiro, quando houve sucesso", example = "psp-ref-987") String partnerReference) {
    }
}
