package com.rodrigogalvao.transaction.service;

import com.rodrigogalvao.transaction.dto.TransactionRequest;
import com.rodrigogalvao.transaction.dto.TransactionResponse;
import com.rodrigogalvao.transaction.model.TransactionStatus;
import com.rodrigogalvao.transaction.model.TransactionType;
import com.rodrigogalvao.transaction.partner.ExternalPartnerClient;
import com.rodrigogalvao.transaction.partner.ExternalPartnerException;
import com.rodrigogalvao.transaction.repository.DailyLimitTracker;
import com.rodrigogalvao.transaction.repository.TransactionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final ExternalPartnerClient partnerClient;
    private final TransactionRepository transactionRepository;
    private final DailyLimitTracker dailyLimitTracker;
    private final MeterRegistry meterRegistry;
    private final Tracer tracer;

    public TransactionService(ExternalPartnerClient partnerClient,
                               TransactionRepository transactionRepository,
                               DailyLimitTracker dailyLimitTracker,
                               MeterRegistry meterRegistry,
                               Tracer tracer) {
        this.partnerClient = partnerClient;
        this.transactionRepository = transactionRepository;
        this.dailyLimitTracker = dailyLimitTracker;
        this.meterRegistry = meterRegistry;
        this.tracer = tracer;
    }

    public TransactionResponse process(TransactionRequest request) {
        Optional<TransactionResponse> existing = transactionRepository.findByTransactionId(request.getTransactionId());
        if (existing.isPresent()) {
            log.info("Transaction {} already processed, returning cached result (status={})",
                    request.getTransactionId(), existing.get().getStatus());
            return existing.get();
        }

        Span span = tracer.nextSpan().name("transaction-process")
                .tag("transaction.id", request.getTransactionId())
                .tag("transaction.type", request.getType().name())
                .start();
        Timer.Sample sample = Timer.start(meterRegistry);
        long startedAt = System.currentTimeMillis();
        TransactionResponse response;
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            response = request.getType() == TransactionType.CASH_IN
                    ? processCashIn(request)
                    : processCashOut(request);
        } catch (ExternalPartnerException ex) {
            span.error(ex);
            log.error("Transaction {} failed due to partner communication error: {}",
                    request.getTransactionId(), ex.getMessage());
            response = new TransactionResponse(request.getTransactionId(), TransactionStatus.ERROR.name(),
                    "falha de comunicação com o parceiro externo");
        } catch (RuntimeException ex) {
            span.error(ex);
            span.tag("transaction.status", TransactionStatus.ERROR.name());
            span.end();
            throw ex;
        }
        span.tag("transaction.status", response.getStatus());
        span.end();

        transactionRepository.save(request.getTransactionId(), request.getType(), request.getAccountId(),
                request.getAmount(), response);
        recordMetrics(sample, request, response);
        log.info("Transaction {} processed as {} in {}ms", request.getTransactionId(), response.getStatus(),
                System.currentTimeMillis() - startedAt);
        return response;
    }

    private void recordMetrics(Timer.Sample sample, TransactionRequest request, TransactionResponse response) {
        String type = request.getType().name();
        String status = response.getStatus();
        meterRegistry.counter("transaction_total", "type", type, "status", status).increment();
        sample.stop(Timer.builder("transaction_processing_duration_seconds")
                .tag("type", type)
                .tag("status", status)
                .publishPercentileHistogram()
                .register(meterRegistry));
    }

    private TransactionResponse processCashIn(TransactionRequest request) {
        String transactionId = request.getTransactionId();
        String accountId = request.getAccountId();

        ExternalPartnerClient.VerifyBalanceResult balance = partnerClient.verifyBalance(accountId);
        if (!balance.available()) {
            log.warn("Transaction {} failed: account {} not available", transactionId, accountId);
            return new TransactionResponse(transactionId, TransactionStatus.FAILED.name(), "conta não disponível");
        }

        ExternalPartnerClient.TransferResult transfer =
                partnerClient.transfer(accountId, request.getAmount(), TransactionType.CASH_IN);
        if (!transfer.success()) {
            log.warn("Transaction {} failed: partner declined transfer", transactionId);
            return new TransactionResponse(transactionId, TransactionStatus.FAILED.name(), "falha na transferência");
        }

        return new TransactionResponse(transactionId, TransactionStatus.PROCESSED.name(), null);
    }

    private TransactionResponse processCashOut(TransactionRequest request) {
        String transactionId = request.getTransactionId();
        String accountId = request.getAccountId();
        BigDecimal amount = request.getAmount();

        ExternalPartnerClient.VerifyBalanceResult balance = partnerClient.verifyBalance(accountId);
        if (!balance.available() || balance.balance().compareTo(amount) < 0) {
            log.warn("Transaction {} failed: insufficient balance for account {}", transactionId, accountId);
            return new TransactionResponse(transactionId, TransactionStatus.FAILED.name(), "saldo insuficiente");
        }

        if (!dailyLimitTracker.tryReserve(accountId, amount)) {
            log.warn("Transaction {} failed: daily cash-out limit exceeded for account {}", transactionId, accountId);
            return new TransactionResponse(transactionId, TransactionStatus.FAILED.name(), "limite diário excedido");
        }

        ExternalPartnerClient.TransferResult transfer =
                partnerClient.transfer(accountId, amount, TransactionType.CASH_OUT);
        if (!transfer.success()) {
            log.warn("Transaction {} failed: partner declined transfer", transactionId);
            return new TransactionResponse(transactionId, TransactionStatus.FAILED.name(), "falha na transferência");
        }

        return new TransactionResponse(transactionId, TransactionStatus.PROCESSED.name(), null);
    }
}
