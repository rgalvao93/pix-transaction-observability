package com.rodrigogalvao.transaction.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks cumulative CASH_OUT amount per account per day, in memory.
 * Resets naturally at midnight since the map key includes the current date.
 */
@Component
public class DailyLimitTracker {

    private final BigDecimal dailyLimit;
    private final Map<String, BigDecimal> totalsByAccountAndDate = new ConcurrentHashMap<>();

    public DailyLimitTracker(@Value("${transaction.cash-out.daily-limit:5000.00}") BigDecimal dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    /**
     * Atomically checks whether adding {@code amount} to today's total for {@code accountId}
     * stays within the daily limit, and reserves it if so.
     */
    public boolean tryReserve(String accountId, BigDecimal amount) {
        String key = key(accountId);
        boolean[] allowed = {true};
        totalsByAccountAndDate.compute(key, (k, currentTotal) -> {
            BigDecimal base = currentTotal == null ? BigDecimal.ZERO : currentTotal;
            BigDecimal updated = base.add(amount);
            if (updated.compareTo(dailyLimit) > 0) {
                allowed[0] = false;
                return base;
            }
            return updated;
        });
        return allowed[0];
    }

    public BigDecimal getDailyLimit() {
        return dailyLimit;
    }

    private String key(String accountId) {
        return accountId + ":" + LocalDate.now();
    }
}
