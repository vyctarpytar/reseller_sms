package com.spa.smart_gate_springboot.ndovuPay.withdraw;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.UUID;

@Repository
public interface WithDrawRequestRepository extends JpaRepository<WithDrawRequest, UUID> {
    @Query(nativeQuery = true, value = """
            select * from msg.ndovu_pay_withdraw
            where  1=1 and (case when  cast(:withDrawResellerId as UUID) is not null then  with_draw_reseller_id = :withDrawResellerId else 1=1 end )
              and (case when :withDrawCreatedByName is not null then with_draw_created_by_email ilike :withDrawCreatedByName else 1=1 end )
              and (case when :withDrawSubmobileNo is not null then with_draw_phone_number ilike :withDrawSubmobileNo else 1=1 end )
              and (case when :withDrawStatus is not null then with_draw_status ilike :withDrawStatus else 1=1 end )
                                      and  ( case when   cast ( :msgDateFrom as DATE)   is not null then
                                                      cast(with_draw_created_date as date) between cast(  :msgDateFrom as Date) and  cast(  :msgDateTo as Date)
                                                         else  cast(with_draw_created_date as date) = cast(  :msgDate as Date) end )
            """)
    Page<WithDrawRequest> getFilteredWithDrawRequest(@Param("withDrawResellerId") UUID withDrawResellerId, @Param("withDrawCreatedByName") String withDrawCreatedByName,
                                                     @Param("msgDate") Date msgDate, @Param("msgDateFrom") Date msgDateFrom, @Param("msgDateTo") Date msgDateTo,
                                                     @Param("withDrawStatus") String withDrawStatus,
                                                     @Param("withDrawSubmobileNo") String withDrawSubmobileNo, Pageable pageable);
}
