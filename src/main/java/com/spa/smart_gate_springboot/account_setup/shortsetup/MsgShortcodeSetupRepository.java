package com.spa.smart_gate_springboot.account_setup.shortsetup;

import com.spa.smart_gate_springboot.account_setup.senderId.MsnProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MsgShortcodeSetupRepository extends JpaRepository<MsgShortcodeSetup, UUID> {
    Optional<MsgShortcodeSetup> findByShId(UUID id);

    List<MsgShortcodeSetup> findByShAccId(UUID id);

    /**
     * The sender ID this account is mapped to on one network. Preferred over the reseller-level
     * registry so an account can carry its own Airtel sender ID. Priority ordering as in
     * {@code ShortCodeRepository}.
     */
    Optional<MsgShortcodeSetup> findFirstByShAccIdAndShMsnProviderOrderByShPriorityAsc(UUID shAccId, MsnProvider msnProvider);

    /**
     * Keep the account mappings on the same network as the registry entry they were copied from —
     * the send path checks the mapping first, so leaving it stale would hide the correction.
     */
    @Modifying
    @Transactional
    @Query("update shortcode_setup s set s.shMsnProvider = :provider where s.shCode = :shCode and s.shResellerId = :resellerId")
    int updateMsnProviderByShCode(@Param("shCode") String shCode, @Param("resellerId") UUID resellerId, @Param("provider") MsnProvider provider);

    /** Backfill counterpart of {@code ShortCodeRepository.backfillMsnProvider()}. */
    @Modifying
    @Transactional
    @Query("update shortcode_setup s set s.shMsnProvider = com.spa.smart_gate_springboot.account_setup.senderId.MsnProvider.SAFARICOM where s.shMsnProvider is null")
    int backfillMsnProvider();



    /**
     * Distinct on (name, network) rather than on the name alone: a sender ID is registered once per
     * network, so collapsing on the name hides that e.g. MERIDIANBET exists on both Safaricom and
     * Airtel. Returns {@code [sh_code, sh_msn_provider]} pairs.
     */
    @Query(value = """
            SELECT DISTINCT m.sh_code, m.sh_msn_provider FROM msg.shortcode_setup m
            where sh_code is not null
              and case when cast(:usrAccId as UUID) is not null then sh_acc_id = cast(:usrAccId as UUID) else 1=1 end
                 and case when cast( :usrResellerId as UUID) is not null then sh_reseller_id = cast(:usrResellerId as UUID) else 1=1 end
            order by 1, 2
            """, nativeQuery = true)
    List<Object[]> findDistinctSenderNames(@Param("usrResellerId") UUID usrResellerId, @Param("usrAccId") UUID usrAccId);


    @Query(value = """
            SELECT * FROM msg.shortcode_setup m
            where  sh_code is not null
              and case when cast(:usrAccId as UUID) is not null then  sh_acc_id = cast(:usrAccId as UUID) else 1=1 end
               and case when :shCode is not null then sh_code ilike :shCode else 1=1 end
               and case when :shStatus is not null then sh_status = :shStatus else 1=1 end
                 and case when cast( :usrResellerId as UUID) is not null then   sh_reseller_id =   cast(:usrResellerId as UUID)    else 1=1 end
            """, nativeQuery = true)
    Page<MsgShortcodeSetup> findAllShortCodes(@Param("usrResellerId") UUID usrResellerId, @Param("usrAccId") UUID usrAccId, @Param("shCode") String shCode, @Param("shStatus") String shStatus, Pageable pageable);

    List<MsgShortcodeSetup> findByShStatusIsNull();
    List<MsgShortcodeSetup> findByShResellerIdIsNull();

    /**
     * Every network variant of one sender ID mapped to an account. Returns a list, not an Optional:
     * the same name can be mapped on both Safaricom and Airtel, and an Optional query would throw
     * {@code IncorrectResultSizeDataAccessException} the moment that happens.
     */
    List<MsgShortcodeSetup> findByShCodeAndShAccId(String shCode, UUID shAccId);
}
