package com.rodrigogalvao.partnermock.service;

import com.rodrigogalvao.partnermock.model.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class PartnerServiceTest {

    private PartnerService service(double failureRate) {
        return new PartnerService(0, 0, failureRate, new Random());
    }

    @Test
    void verifyBalanceReturnsBalanceForKnownAccount() {
        PartnerService.BalanceResult result = service(0.0).verifyBalance("acc-123");

        assertThat(result.available()).isTrue();
        assertThat(result.balance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void verifyBalanceReturnsUnavailableForUnknownAccount() {
        PartnerService.BalanceResult result = service(0.0).verifyBalance("acc-unknown");

        assertThat(result.available()).isFalse();
        assertThat(result.balance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void cashInIncreasesBalance() {
        PartnerService partnerService = service(0.0);

        PartnerService.TransferResult result = partnerService.transfer("acc-123", BigDecimal.valueOf(200), TransactionType.CASH_IN);

        assertThat(result.success()).isTrue();
        assertThat(result.partnerReference()).isNotBlank();
        assertThat(partnerService.verifyBalance("acc-123").balance()).isEqualByComparingTo("1200.00");
    }

    @Test
    void cashOutDecreasesBalance() {
        PartnerService partnerService = service(0.0);

        PartnerService.TransferResult result = partnerService.transfer("acc-123", BigDecimal.valueOf(300), TransactionType.CASH_OUT);

        assertThat(result.success()).isTrue();
        assertThat(partnerService.verifyBalance("acc-123").balance()).isEqualByComparingTo("700.00");
    }

    @Test
    void transferAlwaysFailsWhenFailureRateIsOne() {
        PartnerService.TransferResult result = service(1.0).transfer("acc-123", BigDecimal.TEN, TransactionType.CASH_IN);

        assertThat(result.success()).isFalse();
        assertThat(result.partnerReference()).isNull();
    }
}
