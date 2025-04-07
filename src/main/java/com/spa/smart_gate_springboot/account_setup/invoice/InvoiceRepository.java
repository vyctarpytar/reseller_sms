package com.spa.smart_gate_springboot.account_setup.invoice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByInvoCode(String billRefNumber);

    @Query(value = """
            select * from msg.credit_invoice
            where 1=1
            and (case when   cast(:invoAccId as UUID) is not null then  invo_acc_id =  cast(:invoAccId as UUID)  else 1=1 end )
            and (case when   cast(:invoResellerId as UUID) is not null then  invo_reseller_id =  cast(:invoResellerId as UUID)  else 1=1 end )
            AND (case when :invoStatus IS not  NULL then  invo_status = :invoStatus else 1=1 end )
            AND ( case when   cast ( :invoDate as DATE)   is not null then cast(invo_created_date as date) = cast(  :invoDate as Date) else 1=1 end )
            AND (case when :invoPayerMobileNo IS not  NULL then  invo_payer_mobile_number ilike :invoPayerMobileNo else 1=1 end )
            AND (case when :invoCode IS not  NULL then  invo_code ilike :invoCode else 1=1 end )
            """,
            countQuery = """
             select count(*) from msg.credit_invoice
                    where 1=1
                    and (case when   cast(:invoAccId as UUID) is not null then  invo_acc_id =  cast(:invoAccId as UUID)  else 1=1 end )
                    and (case when   cast(:invoResellerId as UUID) is not null then  invo_reseller_id =  cast(:invoResellerId as UUID)  else 1=1 end )
                    AND (case when :invoStatus IS not  NULL then  invo_status = :invoStatus else 1=1 end )
                    AND ( case when   cast ( :invoDate as DATE)   is not null then cast(invo_created_date as date) = cast(  :invoDate as Date) else 1=1 end )
                    AND (case when :invoPayerMobileNo IS not  NULL then  invo_payer_mobile_number ilike :invoPayerMobileNo else 1=1 end )
                    AND (case when :invoCode IS not  NULL then  invo_code ilike :invoCode else 1=1 end )
                                """,
            nativeQuery = true)
    Page<Invoice> findInvoiceFiltered(
            @Param("invoCode") String invoCode,
            @Param("invoAccId") UUID invoAccId,
            @Param("invoResellerId") UUID invoResellerId,
            @Param("invoPayerMobileNo") String invoPayerMobileNo,
            @Param("invoStatus") String invoStatus,
            @Param("invoDate") Date invoDate,
            Pageable pageable);


@Query (nativeQuery = true,
value = """
select sum(invo_amount) as invo_amount, invo_month_name, invo_month_id
from msg.credit_invoice
where invo_status not in ('PENDING_PAYMENT', 'FAILED_TO_POP_SDK')
  and invo_reseller_id = :resellerId
  and extract(year from invo_created_date) = extract(year from current_timestamp)
group by invo_month_name, invo_month_id
order by invo_month_id asc
""")
List<Object[]> getResellerInvoicesPerYearSummary(@Param("resellerId") UUID resellerId);
}
