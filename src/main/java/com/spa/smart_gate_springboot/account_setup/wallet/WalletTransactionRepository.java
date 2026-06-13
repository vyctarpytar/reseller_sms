package com.spa.smart_gate_springboot.account_setup.wallet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID>,
        JpaSpecificationExecutor<WalletTransaction> {

    boolean existsByExternalRef(String externalRef);

    Optional<WalletTransaction> findByExternalRef(String externalRef);

    Page<WalletTransaction> findByWalletCodeOrderByCreatedAtDesc(String walletCode, Pageable pageable);

    // Hierarchy-aware statement search is built dynamically via WalletTransactionSpecifications.filter(...)
    // and findAll(Specification, Pageable) from JpaSpecificationExecutor — null filters add no predicate
    // (platform-wide for TOP). We avoid the "(:param IS NULL OR col = :param)" JPQL pattern because a bare
    // bind param in ":param IS NULL" is untyped and breaks on PostgreSQL (42P18) when the filter is null.
    // Newest-first ordering is supplied by the caller via the Pageable's Sort.

    /**
     * Signed sum of {@code amount} across ledger rows matching owner/value/tx type — used for the TOP
     * derived summary (cash collected from unit sales, units sold). Returns 0 when there are no rows.
     */
    @Query("select coalesce(sum(t.amount), 0) from WalletTransaction t "
            + "where t.ownerType = :ownerType and t.valueType = :valueType and t.txType = :txType")
    BigDecimal sumAmount(@Param("ownerType") WalletOwnerType ownerType,
                         @Param("valueType") WalletValueType valueType,
                         @Param("txType") WalletTxType txType);
}
