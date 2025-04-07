package com.spa.smart_gate_springboot.account_setup.shortsetup;

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
public interface MsgShortcodeSetupRepository extends JpaRepository<MsgShortcodeSetup, UUID> {
    Optional<MsgShortcodeSetup> findByShId(UUID id);

    List<MsgShortcodeSetup> findByShAccId(UUID id);



    @Query(value = """
            SELECT DISTINCT m.sh_code FROM msg.shortcode_setup m
            where sh_code is not null
              and case when cast(:usrAccId as UUID) is not null then sh_acc_id = cast(:usrAccId as UUID) else 1=1 end
                 and case when cast( :usrResellerId as UUID) is not null then sh_reseller_id = cast(:usrResellerId as UUID) else 1=1 end
            """, nativeQuery = true)
    List<String> findDistinctSenderNames(@Param("usrResellerId") UUID usrResellerId, @Param("usrAccId") UUID usrAccId);


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

    Optional<MsgShortcodeSetup> findByShCodeAndShAccId(String shCode, UUID shAccId);
}
