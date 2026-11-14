package com.rodrigogalvao.transaction.repository;

import com.rodrigogalvao.transaction.entity.DailyLimit;
import com.rodrigogalvao.transaction.entity.DailyLimitId;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface JpaDailyLimitRepository extends JpaRepository<DailyLimit, DailyLimitId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select dailyLimit from DailyLimit dailyLimit " +
            "where dailyLimit.accountId = :accountId and dailyLimit.limitDate = :limitDate")
    Optional<DailyLimit> findByAccountIdAndLimitDateForUpdate(@Param("accountId") String accountId,
                                                              @Param("limitDate") LocalDate limitDate);

    @Query(value = "SELECT lock_id FROM transaction_service.daily_limit_initialization_lock " +
            "WHERE lock_id = 1 FOR UPDATE",
            nativeQuery = true)
    Integer lockInitialReservation();
}
