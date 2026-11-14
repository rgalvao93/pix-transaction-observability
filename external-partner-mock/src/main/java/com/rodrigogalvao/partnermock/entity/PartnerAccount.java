package com.rodrigogalvao.partnermock.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "partner_accounts", schema = "partner_mock")
public class PartnerAccount {

    @Id
    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance;

    public PartnerAccount() {
    }

    public PartnerAccount(String accountId, BigDecimal balance) {
        this.accountId = accountId;
        this.balance = balance;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
