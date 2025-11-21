package com.spa.smart_gate_springboot.account_setup.account;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByAccResellerId(UUID resellerId);

    List<Account> findByAccCreatedBy(UUID usrId);


    @Query(value = """
            select * from  js_core.jsc_accounts a
            where cast( a.acc_reseller_id as UUID) = cast( :resellerId as UUID)
              and not exists (
                select 1 from  msg.shortcode_setup s
                where s.sh_acc_id = a.acc_id
              )
            """, nativeQuery = true)
    List<Account> getAccountsWithoutSenderIdBcp(@Param("resellerId") UUID resellerId);

    @Query(value = """
            select * from  js_core.jsc_accounts a
            where cast( a.acc_reseller_id as UUID) = cast( :resellerId as UUID)
            """, nativeQuery = true)
    List<Account> getAccountsWithoutSenderId(@Param("resellerId") UUID resellerId);

    List<Account> findByAccToDisableOnDateAfterAndAccDeliveryUrlNotNull(Date date);


    @Query(value = """
            select * from  js_core.jsc_accounts a
            where             1=1
               and case when cast( :accAccId as UUID) is not null then a.acc_id   = cast( :accAccId as UUID) else 1=1 end
              and case when :accName is not null then acc_name ilike :accName else 1=1 end
              and case when :accStatus is not null then acc_status = :accStatus else 1=1 end
                AND ( case when   cast ( :accCreatedDate as DATE)   is not null then cast(acc_created_date as date) = cast(  :accCreatedDate as Date) else 1=1 end )
                AND ( case when   cast ( :accDateFrom as DATE)   is not null then cast(acc_created_date as date) between  cast(  :accDateFrom as Date) and cast(  :accDateTo as Date) else 1=1 end )
              and case when :accOfficeMobile is not null then acc_office_mobile ilike :accOfficeMobile else 1=1 end
            """, nativeQuery = true)
    List<Account> filterAccounts(@Param("accName") String accName, @Param("accStatus") String accStatus, @Param("accAccId") UUID accAccId, @Param("accCreatedDate") Date accCreatedDate, @Param("accDateFrom") Date accDateFrom, @Param("accDateTo") Date accDateTo, @Param("accOfficeMobile") String accOfficeMobile

    );


    @Query(value = """
            select sum(acc_msg_bal) from js_core.jsc_accounts a where acc_reseller_id = :resellerId
            """, nativeQuery = true)
    BigDecimal getAccountBalancesForReseller(UUID resellerId);

    @Modifying
    @Transactional
    @Query(value = """
            update js_core.jsc_accounts set acc_msg_bal = acc_msg_bal - :dedAmt where acc_id = :accId
            """, nativeQuery = true)
    int updateAccountMsgBal(@Param("accId") UUID accId, @Param("dedAmt") BigDecimal dedAmt);


    @Modifying
    @Transactional
    @Query(value = """
            update js_core.jsc_accounts set acc_msg_bal = acc_msg_bal + :msgCostId where acc_id = :msgAccId
            """, nativeQuery = true)
    void refundCostCharged(@Param("msgAccId") UUID msgAccId,@Param("msgCostId") BigDecimal msgCostId);

    List<Account> findAllByAccResellerId(UUID rsId);


    @Query(nativeQuery = true,value = """
            select * from js_core.jsc_accounts where not exists(select 1 from js_core.api_key where acc_id = api_acc_id)
            """)
    List<Account> fetchAccountsWithoutApiKeys();
}
