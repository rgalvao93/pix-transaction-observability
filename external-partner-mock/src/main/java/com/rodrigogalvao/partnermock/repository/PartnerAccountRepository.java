package com.rodrigogalvao.partnermock.repository;

import com.rodrigogalvao.partnermock.entity.PartnerAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartnerAccountRepository extends JpaRepository<PartnerAccount, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from PartnerAccount account where account.accountId = :accountId")
    Optional<PartnerAccount> findByAccountIdForUpdate(@Param("accountId") String accountId);
}
