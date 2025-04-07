package com.spa.smart_gate_springboot.account_setup.credit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CreditRepository extends JpaRepository<Credit, UUID> {
    List<Credit> findBySmsResellerIdOrderBySmsCreatedDateDesc(UUID resellerId);

    List<Credit> findBySmsAccIdOrderBySmsCreatedDateDesc(UUID usrAccId);

    @Query(value = """
            select * from js_core.jsc_accounts_sms_payment c
                     where 1=1
                       and case when cast(:accId as UUID) is not null then sms_acc_id = :accId else 1=1 end
                       and (case when cast( :resellerId as UUID) is not null then
                         sms_created_by = :resellerId or
                         sms_reseller_id = :resellerId or
                         exists(select 1 from js_core.jsc_accounts where acc_reseller_id = :resellerId and acc_id =sms_acc_id ) else 1=1 end )
                        AND (case when :crStatus IS NOT  NULL then  cr_status = :crStatus else 1=1 end )
                        AND (case when :smsAccountName IS NOT  NULL then  sms_account_name ilike :smsAccountName else 1=1 end )
                         and (case when cast(:msgSalesUserId as UUID) is not null then
                             exists(select 1 from js_core.jsc_accounts where acc_created_by = :msgSalesUserId and sms_acc_id = acc_id) else 1=1 end )
                          and (case when cast(:resellerId as uuid) is not null then
                            exists(select  1 from js_core.jsc_accounts where sms_acc_id = acc_id and acc_reseller_id = :resellerId) else 1=1 end )
        
            """, nativeQuery = true)
    Page<Credit> getAllAccuntsCreditsByResellerId(@Param("resellerId") UUID resellerId,
                                                  @Param("msgSalesUserId") UUID msgSalesUserId,
                                                  @Param("accId") UUID accId,
                                                  @Param("crStatus") String crStatus,
                                                  @Param("smsAccountName") String smsAccountName,
                                                  Pageable pageable);

    @Query(nativeQuery = true,
    value = """
select * from js_core.jsc_accounts_sms_payment where exists(select 1 from js_core.reseller where sms_reseller_id = rs_id and sms_acc_id is null and rs_created_by = :rsCreatedBy)""")
    Page<Credit> getCreditLoadedToResellers(@Param("rsCreatedBy") UUID rsCreatedBy,
                                                  Pageable pageable);
}

