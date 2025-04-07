package com.spa.smart_gate_springboot.dashboad.reports;

import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<MsgMessageQueueArc, UUID> {
    @Query(value = """
            SELECT  msg_sender_id_name,cast( count(*) as int ) as msg_count
                        FROM msg.message_queue_arc
                         WHERE 1=1 and (case when  cast(:msgAccId as UUID) is not null then  msg_acc_id = :msgAccId else 1=1 end )
                            and  ( case when   cast ( :msgCreatedFromDate as DATE)   is not null then
                              cast(msg_created_date as date) between cast(  :msgCreatedFromDate as Date) and  cast(  :msgCreatedToDate as Date)
                                 else  cast(msg_created_date as date) = cast(  :msgCreatedDate as Date) end )
                        AND (case when :msgStatus IS NOT  NULL then  msg_status = :msgStatus else 1=1 end )
                         and (case when cast(:msgSalesUserId as UUID) is not null then
                             exists(select 1 from js_core.jsc_accounts where acc_created_by = :msgSalesUserId and msg_acc_id = acc_id) else 1=1 end )
                          and (case when cast(:msgResellerId as uuid) is not null then
                            exists(select  1 from js_core.jsc_accounts where msg_acc_id = acc_id and acc_reseller_id = :msgResellerId) else 1=1 end )
                           AND (case when :msgSenderName IS not  NULL then   coalesce(msg_sender_id_name,'-1') = :msgSenderName else 1=1 end )
                        GROUP BY msg_sender_id_name
            """, nativeQuery = true)
    List<Object[]> getSmsSummaryPerAccount(@Param("msgAccId") UUID msgAccId, @Param("msgCreatedDate") Date msgCreatedDate, @Param("msgStatus") String msgStatus, @Param("msgSalesUserId") UUID msgSalesUserId, @Param("msgResellerId") UUID msgResellerId, @Param("msgSenderName") String msgSenderName, @Param("msgCreatedFromDate") Date msgCreatedFromDate, @Param("msgCreatedToDate") Date msgCreatedToDate);


//    daily saummary usage

    @Query(value = """
            SELECT CAST(msg_created_date AS DATE),
                     cast(SUM(CEILING(CAST(LENGTH(msg_message) AS NUMERIC) / 160)) as int),
                  cast( SUM(msg_cost_id) as numeric)
            FROM msg.message_queue_arc
            WHERE  1=1 and (case when  cast(:msgAccId as UUID) is not null then  msg_acc_id = :msgAccId else 1=1 end )
            and  ( case when   cast ( :msgCreatedFromDate as DATE)   is not null then
                              cast(msg_created_date as date) between cast(  :msgCreatedFromDate as Date) and  cast(  :msgCreatedToDate as Date)
                                 else  cast(msg_created_date as date) = cast(  :msgCreatedDate as Date)  end )
              AND msg_status NOT IN ('PENDING_CREDIT', 'PENDING_ENROUTE', 'PENDING_PROCESSING',
                                     'PROCESSING', 'Processing', 'OUTCRED', 'SENTERR', 'SYSTERR')
             and (case when cast(:msgSalesUserId as UUID) is not null then
                             exists(select 1 from js_core.jsc_accounts where acc_created_by = :msgSalesUserId and msg_acc_id = acc_id) else 1=1 end )
                          and (case when cast(:msgResellerId as uuid) is not null then
                            exists(select  1 from js_core.jsc_accounts where msg_acc_id = acc_id and acc_reseller_id = :msgResellerId) else 1=1 end )
            GROUP BY CAST(msg_created_date AS DATE)
            """, countQuery = """
            with y as (select count(*)  FROM msg.message_queue_arc
                        WHERE  1=1 and (case when  cast(:msgAccId as UUID) is not null then  msg_acc_id = :msgAccId else 1=1 end )
                        and  ( case when   cast ( :msgCreatedFromDate as DATE)   is not null then
                                          cast(msg_created_date as date) between cast(  :msgCreatedFromDate as Date) and  cast(  :msgCreatedToDate as Date)
                                             else  cast(msg_created_date as date) = cast(  :msgCreatedDate as Date)  end )
                          AND msg_status NOT IN ('PENDING_CREDIT', 'PENDING_ENROUTE', 'PENDING_PROCESSING',
                                                 'PROCESSING', 'Processing', 'OUTCRED', 'SENTERR', 'SYSTERR')
                         and (case when cast(:msgSalesUserId as UUID) is not null then
                             exists(select 1 from js_core.jsc_accounts where acc_created_by = :msgSalesUserId and msg_acc_id = acc_id) else 1=1 end )
                          and (case when cast(:msgResellerId as uuid) is not null then
                            exists(select  1 from js_core.jsc_accounts where msg_acc_id = acc_id and acc_reseller_id = :msgResellerId) else 1=1 end )
                           GROUP BY CAST(msg_created_date AS DATE))
                          select count(*) from y;
            """, nativeQuery = true)
    Page<Object[]> getDailySmsSummary(@Param("msgAccId") UUID msgAccId, @Param("msgCreatedDate") Date msgCreatedDate, @Param("msgCreatedFromDate") Date msgCreatedFromDate, @Param("msgCreatedToDate") Date msgCreatedToDate, @Param("msgSalesUserId") UUID msgSalesUserId, @Param("msgResellerId") UUID msgResellerId, Pageable pageable);


    @Query(value = """
            SELECT msg_status,
                   cast(SUM(CEILING(CAST(LENGTH(msg_message) AS NUMERIC) / 160)) as int),
                  cast( SUM(msg_cost_id) as numeric)
            FROM msg.message_queue_arc
            WHERE  1=1 and (case when  cast(:msgAccId as UUID) is not null then  msg_acc_id = :msgAccId else 1=1 end )
              and  ( case when   cast ( :msgCreatedFromDate as DATE)   is not null then
                              cast(msg_created_date as date) between cast(  :msgCreatedFromDate as Date) and  cast(  :msgCreatedToDate as Date)
                                 else  cast(msg_created_date as date) = cast(  :msgCreatedDate as Date) end )
              AND msg_status NOT IN ('PENDING_CREDIT', 'PENDING_ENROUTE', 'PENDING_PROCESSING', 'PROCESSING', 'Processing', 'OUTCRED', 'SENTERR', 'SYSTERR')
             and (case when cast(:msgSalesUserId as UUID) is not null then
                             exists(select 1 from js_core.jsc_accounts where acc_created_by = :msgSalesUserId and msg_acc_id = acc_id) else 1=1 end )
                          and (case when cast(:msgResellerId as uuid) is not null then
                            exists(select  1 from js_core.jsc_accounts where msg_acc_id = acc_id and acc_reseller_id = :msgResellerId) else 1=1 end )
            GROUP BY msg_status
            """, countQuery = """
            select count(*) from    msg.message_queue_arc
                        WHERE  1=1 and (case when  cast(:msgAccId as UUID) is not null then  msg_acc_id = :msgAccId else 1=1 end )
                          and  ( case when   cast ( :msgCreatedFromDate as DATE)   is not null then
                                          cast(msg_created_date as date) between cast(  :msgCreatedFromDate as Date) and  cast(  :msgCreatedToDate as Date)
                                             else  cast(msg_created_date as date) = cast(  :msgCreatedDate as Date) end )
                          AND msg_status NOT IN ('PENDING_CREDIT', 'PENDING_ENROUTE', 'PENDING_PROCESSING', 'PROCESSING', 'Processing', 'OUTCRED', 'SENTERR', 'SYSTERR')
             and (case when cast(:msgSalesUserId as UUID) is not null then
                             exists(select 1 from js_core.jsc_accounts where acc_created_by = :msgSalesUserId and msg_acc_id = acc_id) else 1=1 end )
                          and (case when cast(:msgResellerId as uuid) is not null then
                            exists(select  1 from js_core.jsc_accounts where msg_acc_id = acc_id and acc_reseller_id = :msgResellerId) else 1=1 end )
            """, nativeQuery = true)
    Page<Object[]> getStatusSmsSummary(@Param("msgAccId") UUID msgAccId, @Param("msgCreatedDate") Date msgCreatedDate, @Param("msgCreatedFromDate") Date msgCreatedFromDate, @Param("msgCreatedToDate") Date msgCreatedToDate, @Param("msgSalesUserId") UUID msgSalesUserId, @Param("msgResellerId") UUID msgResellerId, Pageable pageable);


}
