package com.spa.smart_gate_springboot.account_setup.wallet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    boolean existsByExternalRef(String externalRef);

    Optional<WalletTransaction> findByExternalRef(String externalRef);

    Page<WalletTransaction> findByWalletCodeOrderByCreatedAtDesc(String walletCode, Pageable pageable);

    /**
     * Hierarchy-aware statement search. Any null filter is ignored (platform-wide for TOP). Filters:
     * reseller context, triggering account, and value type (KSH/UNIT). Newest first.
     */
    @Query("select t from WalletTransaction t where "
            + "(:resellerId is null or t.resellerId = :resellerId) and "
            + "(:accountId is null or t.accountId = :accountId) and "
            + "(:valueType is null or t.valueType = :valueType) "
            + "order by t.createdAt desc")
    Page<WalletTransaction> search(@Param("resellerId") UUID resellerId,
                                   @Param("accountId") UUID accountId,
                                   @Param("valueType") WalletValueType valueType,
                                   Pageable pageable);

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
