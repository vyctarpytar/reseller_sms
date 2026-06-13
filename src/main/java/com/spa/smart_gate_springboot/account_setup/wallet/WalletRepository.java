package com.spa.smart_gate_springboot.account_setup.wallet;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByWalletCode(String walletCode);

    /** Sum of available cash (balance − locked) held across every reseller wallet. */
    @Query("select coalesce(sum(w.balance - w.lockedBalance), 0) from Wallet w "
            + "where w.ownerType = com.spa.smart_gate_springboot.account_setup.wallet.WalletOwnerType.RESELLER")
    BigDecimal sumResellerAvailableBalance();

    boolean existsByWalletCode(String walletCode);

    Optional<Wallet> findByOwnerId(UUID ownerId);

    /** All wallets for the given owner ids (used to enrich a reseller list with cash balances). */
    List<Wallet> findByOwnerIdIn(Collection<UUID> ownerIds);

    /** Acquires a row-level exclusive lock — must be called inside a @Transactional method. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.walletCode = :walletCode")
    Optional<Wallet> findByWalletCodeForUpdate(@Param("walletCode") String walletCode);
}
