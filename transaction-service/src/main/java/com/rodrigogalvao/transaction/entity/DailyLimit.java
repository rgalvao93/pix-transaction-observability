package com.rodrigogalvao.transaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_limits", schema = "transaction_service")
@IdClass(DailyLimitId.class)
public class DailyLimit {

    @Id
    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Id
    @Column(name = "limit_date", nullable = false)
    private LocalDate limitDate;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    public DailyLimit() {
    }

    public DailyLimit(String accountId, LocalDate limitDate, BigDecimal totalAmount) {
        this.accountId = accountId;
        this.limitDate = limitDate;
        this.totalAmount = totalAmount;
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
