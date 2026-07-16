package com.rodrigogalvao.partnermock.service;

import com.rodrigogalvao.partnermock.model.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PartnerService {

    private static final Logger log = LoggerFactory.getLogger(PartnerService.class);

    private final Map<String, BigDecimal> balances = new ConcurrentHashMap<>();
    private final long minLatencyMs;
    private final long maxLatencyMs;
    private final double failureRate;
    private final Random random;

    @Autowired
    public PartnerService(
            @Value("${partner.mock.latency.min-ms:100}") long minLatencyMs,
            @Value("${partner.mock.latency.max-ms:500}") long maxLatencyMs,
            @Value("${partner.mock.failure-rate:0.05}") double failureRate) {
        this(minLatencyMs, maxLatencyMs, failureRate, new Random());
    }

    PartnerService(long minLatencyMs, long maxLatencyMs, double failureRate, Random random) {
        this.minLatencyMs = minLatencyMs;
        this.maxLatencyMs = maxLatencyMs;
        this.failureRate = failureRate;
        this.random = random;
        seedDemoAccounts();
    }

    private void seedDemoAccounts() {
        balances.put("acc-123", new BigDecimal("1000.00"));
        balances.put("acc-789", new BigDecimal("500.00"));
    }

    public BalanceResult verifyBalance(String accountId) {
        simulateLatency();
        BigDecimal balance = balances.get(accountId);
        if (balance == null) {
            log.warn("verify-balance: unknown account {}", accountId);
            return new BalanceResult(accountId, BigDecimal.ZERO, false);
        }
        return new BalanceResult(accountId, balance, true);
    }

    public TransferResult transfer(String accountId, BigDecimal amount, TransactionType type) {
        simulateLatency();
        if (random.nextDouble() < failureRate) {
            log.warn("transfer: simulated failure for account {}", accountId);
            return new TransferResult(false, null);
        }

        BigDecimal delta = type == TransactionType.CASH_IN ? amount : amount.negate();
        balances.merge(accountId, delta, BigDecimal::add);

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

    public record BalanceResult(String accountId, BigDecimal balance, boolean available) {
    }

    public record TransferResult(boolean success, String partnerReference) {
    }
}
