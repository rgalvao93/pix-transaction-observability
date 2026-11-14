package com.rodrigogalvao.partnermock.service;

import com.rodrigogalvao.partnermock.entity.PartnerAccount;
import com.rodrigogalvao.partnermock.model.TransactionType;
import com.rodrigogalvao.partnermock.repository.PartnerAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Random;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerServiceTest {

    @Mock
    private PartnerAccountRepository accountRepository;

    private PartnerService service(double failureRate) {
        return new PartnerService(accountRepository, 0, 0, failureRate, new Random());
    }

    private PartnerAccount account(String accountId, String balance) {
        return new PartnerAccount(accountId, new BigDecimal(balance));
    }

    private void stubAccountRead(PartnerAccount account) {
        when(accountRepository.findById(account.getAccountId())).thenReturn(Optional.of(account));
    }

    private void stubAccountTransfer(PartnerAccount account) {
        when(accountRepository.findByAccountIdForUpdate(account.getAccountId())).thenReturn(Optional.of(account));
    }

    @Test
    void verifyBalanceReturnsBalanceForKnownAccount() {
        PartnerAccount account = account("acc-123", "1000.00");
        stubAccountRead(account);
        PartnerService.BalanceResult result = service(0.0).verifyBalance("acc-123");

        assertThat(result.available()).isTrue();
        assertThat(result.balance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void verifyBalanceReturnsUnavailableForUnknownAccount() {
        when(accountRepository.findById("acc-unknown")).thenReturn(Optional.empty());
        PartnerService.BalanceResult result = service(0.0).verifyBalance("acc-unknown");

        assertThat(result.available()).isFalse();
        assertThat(result.balance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void cashInIncreasesBalance() {
        PartnerAccount account = account("acc-123", "1000.00");
        stubAccountTransfer(account);
        PartnerService partnerService = service(0.0);

        PartnerService.TransferResult result = partnerService.transfer("acc-123", BigDecimal.valueOf(200), TransactionType.CASH_IN);

        assertThat(result.success()).isTrue();
        assertThat(result.partnerReference()).isNotBlank();
        assertThat(account.getBalance()).isEqualByComparingTo("1200.00");
    }

    @Test
    void cashOutDecreasesBalance() {
        PartnerAccount account = account("acc-123", "1000.00");
        stubAccountTransfer(account);
        PartnerService partnerService = service(0.0);

        PartnerService.TransferResult result = partnerService.transfer("acc-123", BigDecimal.valueOf(300), TransactionType.CASH_OUT);

        assertThat(result.success()).isTrue();
        assertThat(account.getBalance()).isEqualByComparingTo("700.00");
    }

    @Test
    void transferAlwaysFailsWhenFailureRateIsOne() {
        PartnerService.TransferResult result = service(1.0).transfer("acc-123", BigDecimal.TEN, TransactionType.CASH_IN);

        assertThat(result.success()).isFalse();
        assertThat(result.partnerReference()).isNull();
    }
}
