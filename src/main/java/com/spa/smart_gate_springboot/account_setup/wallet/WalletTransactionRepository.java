package com.spa.smart_gate_springboot.account_setup.wallet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    boolean existsByExternalRef(String externalRef);

    Optional<WalletTransaction> findByExternalRef(String externalRef);

    Page<WalletTransaction> findByWalletCodeOrderByCreatedAtDesc(String walletCode, Pageable pageable);
}
