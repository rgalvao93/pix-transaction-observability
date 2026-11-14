package com.rodrigogalvao.partnermock.service;

import com.rodrigogalvao.partnermock.model.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PartnerAccountPersistenceIntegrationTest {

    @Autowired
    private PartnerService partnerService;

    @Test
    void persistsBalanceChangesAcrossRepositoryReads() {
        String accountId = "acc-" + UUID.randomUUID();

        assertThat(partnerService.verifyBalance(accountId).available()).isFalse();

        PartnerService.TransferResult result =
                partnerService.transfer(accountId, new BigDecimal("25.00"), TransactionType.CASH_IN);

        assertThat(result.success()).isTrue();
        assertThat(partnerService.verifyBalance(accountId).balance()).isEqualByComparingTo("25.00");
    }
}
