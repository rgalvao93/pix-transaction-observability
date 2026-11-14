package com.rodrigogalvao.transaction.repository;

import com.rodrigogalvao.transaction.entity.DailyLimit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Component
public class DailyLimitTracker {

    private final BigDecimal dailyLimit;
    private final JpaDailyLimitRepository repository;

    public DailyLimitTracker(@Value("${transaction.cash-out.daily-limit:5000.00}") BigDecimal dailyLimit,
                             JpaDailyLimitRepository repository) {
        this.dailyLimit = dailyLimit;
        this.repository = repository;
    }

    @Transactional
    public boolean tryReserve(String accountId, BigDecimal amount) {
        LocalDate today = LocalDate.now();
        Optional<DailyLimit> current = repository.findByAccountIdAndLimitDateForUpdate(accountId, today);
        if (current.isEmpty()) {
            repository.lockInitialReservation();
            current = repository.findByAccountIdAndLimitDateForUpdate(accountId, today);
        }

        if (current.isPresent()) {
            DailyLimit dailyLimitForAccount = current.get();
            BigDecimal updatedTotal = dailyLimitForAccount.getTotalAmount().add(amount);
            if (updatedTotal.compareTo(dailyLimit) > 0) {
                return false;
            }
            dailyLimitForAccount.setTotalAmount(updatedTotal);
            repository.save(dailyLimitForAccount);
            return true;
        }

        if (amount.compareTo(dailyLimit) > 0) {
            return false;
        }
        repository.save(new DailyLimit(accountId, today, amount));
        return true;
    }

    public BigDecimal getDailyLimit() {
        return dailyLimit;
    }

}
