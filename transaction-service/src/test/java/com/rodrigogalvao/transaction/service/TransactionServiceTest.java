package com.rodrigogalvao.transaction.service;

import com.rodrigogalvao.transaction.dto.TransactionRequest;
import com.rodrigogalvao.transaction.dto.TransactionResponse;
import com.rodrigogalvao.transaction.model.TransactionStatus;
import com.rodrigogalvao.transaction.model.TransactionType;
import com.rodrigogalvao.transaction.partner.ExternalPartnerClient;
import com.rodrigogalvao.transaction.partner.ExternalPartnerException;
import com.rodrigogalvao.transaction.repository.DailyLimitTracker;
import com.rodrigogalvao.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private ExternalPartnerClient partnerClient;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private DailyLimitTracker dailyLimitTracker;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(partnerClient, transactionRepository, dailyLimitTracker);
    }

    private TransactionRequest request(TransactionType type, BigDecimal amount) {
        TransactionRequest request = new TransactionRequest();
        request.setTransactionId("txn-1");
        request.setType(type);
        request.setAmount(amount);
        request.setAccountId("acc-1");
        return request;
    }

    @Test
    void cashInSucceedsWhenPartnerAcceptsTransfer() {
        when(transactionRepository.findByTransactionId("txn-1")).thenReturn(Optional.empty());
        when(partnerClient.verifyBalance("acc-1"))
                .thenReturn(new ExternalPartnerClient.VerifyBalanceResult("acc-1", BigDecimal.ZERO, true));
        when(partnerClient.transfer(eq("acc-1"), eq(BigDecimal.valueOf(100)), eq(TransactionType.CASH_IN)))
                .thenReturn(new ExternalPartnerClient.TransferResult(true, "ref-1"));

        TransactionResponse response = transactionService.process(request(TransactionType.CASH_IN, BigDecimal.valueOf(100)));

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.PROCESSED.name());
        verify(transactionRepository).save(eq("txn-1"), any());
    }

    @Test
    void cashInFailsWhenAccountNotAvailable() {
        when(transactionRepository.findByTransactionId("txn-1")).thenReturn(Optional.empty());
        when(partnerClient.verifyBalance("acc-1"))
                .thenReturn(new ExternalPartnerClient.VerifyBalanceResult("acc-1", BigDecimal.ZERO, false));

        TransactionResponse response = transactionService.process(request(TransactionType.CASH_IN, BigDecimal.valueOf(100)));

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.FAILED.name());
        assertThat(response.getReason()).isEqualTo("conta não disponível");
        verify(partnerClient, never()).transfer(anyString(), any(BigDecimal.class), any(TransactionType.class));
    }

    @Test
    void cashOutSucceedsWhenBalanceAndLimitAreOk() {
        when(transactionRepository.findByTransactionId("txn-1")).thenReturn(Optional.empty());
        when(partnerClient.verifyBalance("acc-1"))
                .thenReturn(new ExternalPartnerClient.VerifyBalanceResult("acc-1", BigDecimal.valueOf(500), true));
        when(dailyLimitTracker.tryReserve("acc-1", BigDecimal.valueOf(100))).thenReturn(true);
        when(partnerClient.transfer(eq("acc-1"), eq(BigDecimal.valueOf(100)), eq(TransactionType.CASH_OUT)))
                .thenReturn(new ExternalPartnerClient.TransferResult(true, "ref-2"));

        TransactionResponse response = transactionService.process(request(TransactionType.CASH_OUT, BigDecimal.valueOf(100)));

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.PROCESSED.name());
    }

    @Test
    void cashOutFailsWithInsufficientBalance() {
        when(transactionRepository.findByTransactionId("txn-1")).thenReturn(Optional.empty());
        when(partnerClient.verifyBalance("acc-1"))
                .thenReturn(new ExternalPartnerClient.VerifyBalanceResult("acc-1", BigDecimal.valueOf(50), true));

        TransactionResponse response = transactionService.process(request(TransactionType.CASH_OUT, BigDecimal.valueOf(100)));

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.FAILED.name());
        assertThat(response.getReason()).isEqualTo("saldo insuficiente");
        verify(dailyLimitTracker, never()).tryReserve(anyString(), any(BigDecimal.class));
    }

    @Test
    void cashOutFailsWhenDailyLimitExceeded() {
        when(transactionRepository.findByTransactionId("txn-1")).thenReturn(Optional.empty());
        when(partnerClient.verifyBalance("acc-1"))
                .thenReturn(new ExternalPartnerClient.VerifyBalanceResult("acc-1", BigDecimal.valueOf(5000), true));
        when(dailyLimitTracker.tryReserve("acc-1", BigDecimal.valueOf(100))).thenReturn(false);

        TransactionResponse response = transactionService.process(request(TransactionType.CASH_OUT, BigDecimal.valueOf(100)));

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.FAILED.name());
        assertThat(response.getReason()).isEqualTo("limite diário excedido");
        verify(partnerClient, never()).transfer(anyString(), any(BigDecimal.class), any(TransactionType.class));
    }

    @Test
    void returnsErrorWhenPartnerCommunicationFails() {
        when(transactionRepository.findByTransactionId("txn-1")).thenReturn(Optional.empty());
        when(partnerClient.verifyBalance("acc-1")).thenThrow(new ExternalPartnerException("timeout", null));

        TransactionResponse response = transactionService.process(request(TransactionType.CASH_IN, BigDecimal.valueOf(100)));

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.ERROR.name());
    }

    @Test
    void returnsCachedResponseForDuplicateTransactionId() {
        TransactionResponse cached = new TransactionResponse("txn-1", TransactionStatus.PROCESSED.name(), null);
        when(transactionRepository.findByTransactionId("txn-1")).thenReturn(Optional.of(cached));

        TransactionResponse response = transactionService.process(request(TransactionType.CASH_IN, BigDecimal.valueOf(100)));

        assertThat(response).isSameAs(cached);
        verifyNoInteractions(partnerClient);
    }
}
