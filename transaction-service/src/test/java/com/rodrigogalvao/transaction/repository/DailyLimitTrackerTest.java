package com.rodrigogalvao.transaction.repository;

import com.rodrigogalvao.transaction.entity.DailyLimit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyLimitTrackerTest {

    @Mock
    private JpaDailyLimitRepository repository;

    private DailyLimitTracker tracker(String dailyLimit) {
        return new DailyLimitTracker(new BigDecimal(dailyLimit), repository);
    }

    @Test
    void firstReservationAboveLimitIsRejectedWithoutSaving() {
        when(repository.findByAccountIdAndLimitDateForUpdate("acc-1", LocalDate.now()))
                .thenReturn(Optional.empty());

        boolean reserved = tracker("5000.00").tryReserve("acc-1", new BigDecimal("5001.00"));

        assertThat(reserved).isFalse();
        verify(repository).lockInitialReservation();
        verify(repository, never()).save(any(DailyLimit.class));
    }

    @Test
    void firstReservationWithinLimitIsSaved() {
        when(repository.findByAccountIdAndLimitDateForUpdate("acc-1", LocalDate.now()))
                .thenReturn(Optional.empty());

        boolean reserved = tracker("5000.00").tryReserve("acc-1", new BigDecimal("100.00"));

        assertThat(reserved).isTrue();
        ArgumentCaptor<DailyLimit> captor = ArgumentCaptor.forClass(DailyLimit.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void existingReservationWithinLimitIsAccumulatedAndSaved() {
        DailyLimit existing = new DailyLimit("acc-1", LocalDate.now(), new BigDecimal("1000.00"));
        when(repository.findByAccountIdAndLimitDateForUpdate("acc-1", LocalDate.now()))
                .thenReturn(Optional.of(existing));

        boolean reserved = tracker("5000.00").tryReserve("acc-1", new BigDecimal("500.00"));

        assertThat(reserved).isTrue();
        verify(repository).save(existing);
        assertThat(existing.getTotalAmount()).isEqualByComparingTo("1500.00");
    }

    @Test
    void existingReservationAboveLimitIsRejectedWithoutSaving() {
        DailyLimit existing = new DailyLimit("acc-1", LocalDate.now(), new BigDecimal("4900.00"));
        when(repository.findByAccountIdAndLimitDateForUpdate("acc-1", LocalDate.now()))
                .thenReturn(Optional.of(existing));

        boolean reserved = tracker("5000.00").tryReserve("acc-1", new BigDecimal("200.00"));

        assertThat(reserved).isFalse();
        verify(repository, never()).save(any(DailyLimit.class));
        assertThat(existing.getTotalAmount()).isEqualByComparingTo("4900.00");
    }

    @Test
    void getDailyLimitReturnsConfiguredValue() {
        assertThat(tracker("5000.00").getDailyLimit()).isEqualByComparingTo("5000.00");
    }
}
