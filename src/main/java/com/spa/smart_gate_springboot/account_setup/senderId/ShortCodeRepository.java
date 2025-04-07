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

    Optional<ShortCode> findByShCode(String shortCode);

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

}
