package com.spa.smart_gate_springboot.account_setup.reseller;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ResellerRepo extends JpaRepository<Reseller, UUID> {
    List<Reseller> findByRsCreatedBy(UUID createdBy);

    /** Reseller census for the platform (TOP) overview cards: active vs inactive. */
    @Query("select count(r) from Reseller r where r.isActive = :active")
    long countByActiveFlag(@Param("active") boolean active);

    /** Total units still sitting in reseller pools (not yet allocated to accounts). */
    @Query("select coalesce(sum(r.rsAllocatableUnit), 0) from Reseller r")
    BigDecimal sumAllocatableUnits();

    /**
     * Active resellers whose allocatable units are at or below their reorder level
     * ({@code rs_reorder_level}, or {@code defaultThreshold} when unset or not a whole number) and that have not been
     * alerted since {@code alertBefore}. Mirrors {@code AccountRepository.findAccountsForLowBalanceAlert}:
     * only resellers with an account that sent since {@code sentSince} (EAT midnight from
     * {@code AppTime}) are returned, so dormant resellers are never alerted.
     */
    @Query(nativeQuery = true, value = """
            select * from js_core.reseller r
            where coalesce(r.rs_allocatable_unit, 0) <= coalesce(case when r.rs_reorder_level ~ '^[0-9]+$'
                                                                then cast(r.rs_reorder_level as numeric) end, :defaultThreshold)
              and coalesce(case when r.rs_reorder_level ~ '^[0-9]+$'
                                then cast(r.rs_reorder_level as numeric) end, :defaultThreshold) > 0
              and r.is_active
              and r.rs_deleted_date is null
              and (r.rs_last_low_bal_alert is null or r.rs_last_low_bal_alert <= :alertBefore)
              and exists (
                    select 1 from js_core.jsc_accounts a
                    join msg.message_queue_arc m on m.msg_acc_id = a.acc_id
                    where a.acc_reseller_id = r.rs_id
                      and m.msg_created_date >= :sentSince
              )
            """)
    List<Reseller> findResellersForLowBalanceAlert(@Param("defaultThreshold") BigDecimal defaultThreshold,
                                                   @Param("alertBefore") LocalDateTime alertBefore,
                                                   @Param("sentSince") LocalDateTime sentSince);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            update js_core.reseller set rs_last_low_bal_alert = :alertTime where rs_id = :rsId
            """)
    void updateLowBalanceAlertTime(@Param("rsId") UUID rsId, @Param("alertTime") LocalDateTime alertTime);

    /**
     * Sets (or, with null, clears back to the platform default) the reseller's low-balance reorder
     * level. Single-column UPDATE so a self-service change never writes back stale unit balances.
     */
    @Modifying(clearAutomatically = true) // so the re-read in the same request sees the new level
    @Transactional
    @Query(nativeQuery = true, value = """
            update js_core.reseller set rs_reorder_level = cast(:level as varchar) where rs_id = :rsId
            """)
    void updateReorderLevel(@Param("rsId") UUID rsId, @Param("level") String level);
}
