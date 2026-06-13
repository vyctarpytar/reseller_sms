package com.spa.smart_gate_springboot.account_setup.senderId;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShortCodeRepository extends JpaRepository<ShortCode, UUID> {

    Optional<ShortCode> findByShCodeAndShResellerId(String shortCode, UUID resellerId);

    @Query(value = """
            SELECT DISTINCT m.sh_code FROM msg.shortcode m
            where sh_code is not null
                 and case when cast( :usrResellerId as UUID) is not null then sh_reseller_id = cast(:usrResellerId as UUID) else 1=1 end
            """, nativeQuery = true)
    List<String> findDistinctSenderNames(@Param("usrResellerId") UUID usrResellerId);


    @Query(value = """
            SELECT * FROM msg.shortcode m
            where  sh_code is not null
                            and case when :shCode is not null then sh_code ilike :shCode else 1=1 end
               and case when :shStatus is not null then sh_status = :shStatus else 1=1 end
                 and case when cast( :usrResellerId as UUID) is not null then   sh_reseller_id =   cast(:usrResellerId as UUID)    else 1=1 end
            """, nativeQuery = true)
    Page<ShortCode> findAllShortCodes(@Param("usrResellerId") UUID usrResellerId, @Param("shCode") String shCode, @Param("shStatus") String shStatus, Pageable pageable);


    /**
     * Sender-ID breakdown for the overview cards. Returns a single row of
     * {@code [promotional, transactional, mapped, pending, total]}. "Mapped" = sender IDs already
     * mapped to an account ({@code ShStatus.ACTIVE}); "pending" = created but not yet mapped
     * ({@code ShStatus.PENDING_MAPPING}). Sender type is a free-text string, so anything starting
     * with PROMOT counts as promotional.
     */
    @Query("""
            select
              coalesce(sum(case when upper(s.shSenderType) like 'PROMOT%' then 1 else 0 end), 0),
              coalesce(sum(case when upper(s.shSenderType) = 'TRANSACTIONAL' then 1 else 0 end), 0),
              coalesce(sum(case when s.shStatus = com.spa.smart_gate_springboot.account_setup.senderId.ShStatus.ACTIVE then 1 else 0 end), 0),
              coalesce(sum(case when s.shStatus = com.spa.smart_gate_springboot.account_setup.senderId.ShStatus.PENDING_MAPPING then 1 else 0 end), 0),
              count(s)
            from shortcode s
            """)
    List<Object[]> senderIdStats();

    @Query("""
            select
              coalesce(sum(case when upper(s.shSenderType) like 'PROMOT%' then 1 else 0 end), 0),
              coalesce(sum(case when upper(s.shSenderType) = 'TRANSACTIONAL' then 1 else 0 end), 0),
              coalesce(sum(case when s.shStatus = com.spa.smart_gate_springboot.account_setup.senderId.ShStatus.ACTIVE then 1 else 0 end), 0),
              coalesce(sum(case when s.shStatus = com.spa.smart_gate_springboot.account_setup.senderId.ShStatus.PENDING_MAPPING then 1 else 0 end), 0),
              count(s)
            from shortcode s
            where s.shResellerId = :resellerId
            """)
    List<Object[]> senderIdStatsForReseller(@Param("resellerId") UUID resellerId);

}
