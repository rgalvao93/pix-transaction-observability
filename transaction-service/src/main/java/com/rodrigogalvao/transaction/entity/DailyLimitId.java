package com.rodrigogalvao.transaction.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class DailyLimitId implements Serializable {

    private String accountId;
    private LocalDate limitDate;

    public DailyLimitId() {
    }

    public DailyLimitId(String accountId, LocalDate limitDate) {
        this.accountId = accountId;
        this.limitDate = limitDate;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public LocalDate getLimitDate() {
        return limitDate;
    }

    public void setLimitDate(LocalDate limitDate) {
        this.limitDate = limitDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailyLimitId that)) return false;
        return Objects.equals(accountId, that.accountId) && Objects.equals(limitDate, that.limitDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, limitDate);
    }
}
