package com.spa.smart_gate_springboot.messaging.send_message;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface MsgMessageQueueArcRepository extends JpaRepository<MsgMessageQueueArc, UUID> {


    @Query(value = """
             SELECT * FROM msg.message_queue_arc
             WHERE 1=1 and (case when   cast(:msgAccId as UUID) is not null then  msg_acc_id =  cast(:msgAccId as UUID)  else 1=1 end )
                   AND (case when cast ( :msgGrpId as UUID) IS not NULL then   msg_group_id = cast ( :msgGrpId as UUID)  else 1=1 end)
             AND ( case when   cast ( :msgCreatedDate as DATE)   is not null then cast(msg_created_date as date) = cast(  :msgCreatedDate as Date) else 1=1 end )
             AND ( case when   cast ( :msgDateFrom as DATE)   is not null and  cast ( :msgDateTo as DATE)   is not null  
                             then cast(msg_created_date as date) >= cast(  :msgDateFrom as Date) and  cast(msg_created_date as date) <= cast(  :msgDateTo as Date)  else 1=1 end )
            AND (case when :msgStatus IS not  NULL then  msg_status = :msgStatus else 1=1 end )
            AND (case when :msgMessage IS not  NULL then  msg_message ilike :msgMessage else 1=1 end )
            AND (case when :msgSubmobileNo IS not  NULL then  msg_sub_mobile_no ilike :msgSubmobileNo else 1=1 end )
                AND (case when :msgSenderName IS not  NULL then   coalesce(msg_sender_id_name,'-1') = :msgSenderName else 1=1 end )
              and (case when cast(:msgResellerId as uuid) is not null then
                  exists(select 1 from js_core.reseller where  msg_reseller_id = rs_id and ( rs_id =:msgResellerId or rs_created_by =:msgResellerId )) else 1=1 end )
            """, nativeQuery = true)
    Page<MsgMessageQueueArc> findByMessagesArcFilters(@Param("msgAccId") UUID msgAccId, @Param("msgResellerId") UUID msgResellerId, @Param("msgGrpId") UUID msgGrpId,
                                                      @Param("msgCreatedDate") Date msgCreatedDate,
                                                      @Param("msgStatus") String msgStatus, @Param("msgSubmobileNo") String msgSubmobileNo, @Param("msgMessage") String msgMessage,
                                                      @Param("msgSenderName") String msgSenderName,
                                                      @Param("msgDateFrom") Date msgDateFrom,
                                                      @Param("msgDateTo") Date msgDateTo,
                                                      Pageable pageable);


    @Query(value = """
            SELECT DISTINCT m.msg_status FROM msg.message_queue_arc m 
            where
                 1= 1 
                 and case when cast( :msgAccId as UUID) is not null then   msg_acc_id = cast(:msgAccId as UUID) else 1=1 end
                 and case when cast( :perReseller as UUID) is not null then   
                     exists(select 1 from js_core.jsc_accounts where acc_id = msg_acc_id and acc_reseller_id =  cast(:perReseller as UUID) )
                     else 1=1 end
            """, nativeQuery = true)
    List<String> findDistinctMsgStatus(@Param("msgAccId") UUID msgAccId, @Param("perReseller") UUID perReseller);


    @Query(value = """
            SELECT  TO_CHAR(msg_created_date, 'HH24:MI') as  msg_created_date,msg_status, cast( count(*) as int ) as msg_count
                        FROM msg.message_queue_arc
                        WHERE 1=1 and (case when cast(:msgAccId as UUID) is not null then  msg_acc_id = :msgAccId else 1=1 end )
            
                          and  ( case when   cast ( :msgCreatedFromDate as DATE)   is not null then
                              cast(msg_created_date as date) between cast(  :msgCreatedFromDate as Date) and  cast(  :msgCreatedToDate as Date)
                                 else  cast(msg_created_date as date) = cast(  :msgCreatedDate as Date) end )
                        AND (case when :msgStatus IS NOT  NULL then  msg_status = :msgStatus else 1=1 end )
                         and (case when cast(:msgSalesUserId as UUID) is not null then
                             exists(select 1 from js_core.jsc_accounts where acc_created_by = :msgSalesUserId and msg_acc_id = acc_id) else 1=1 end )
                        and (case when cast(:msgResellerId as uuid) is not null then
                            exists(select  1 from js_core.jsc_accounts where msg_acc_id = acc_id and acc_reseller_id = :msgResellerId) else 1=1 end )
                          AND (case when :msgSenderName IS not  NULL then   coalesce(msg_sender_id_name,'-1') = :msgSenderName else 1=1 end )
                        GROUP BY  TO_CHAR(msg_created_date, 'HH24:MI'),msg_status order by msg_created_date asc
            """, nativeQuery = true)
    List<Object[]> getTimeSeriesDataForToday(@Param("msgAccId") UUID msgAccId, @Param("msgCreatedDate") Date msgCreatedDate, @Param("msgStatus") String msgStatus, @Param("msgSalesUserId") UUID msgSalesUserId, @Param("msgResellerId") UUID msgResellerId, @Param("msgSenderName") String msgSenderName, @Param("msgCreatedFromDate") Date msgCreatedFromDate, @Param("msgCreatedToDate") Date msgCreatedToDate);

    @Query(value = """
            SELECT  msg_status,cast( count(*) as int ) as msg_count
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
                        GROUP BY msg_status
            """, nativeQuery = true)
    List<Object[]> getMessageStatusStatForToday(@Param("msgAccId") UUID msgAccId, @Param("msgCreatedDate") Date msgCreatedDate, @Param("msgStatus") String msgStatus, @Param("msgSalesUserId") UUID msgSalesUserId, @Param("msgResellerId") UUID msgResellerId, @Param("msgSenderName") String msgSenderName, @Param("msgCreatedFromDate") Date msgCreatedFromDate, @Param("msgCreatedToDate") Date msgCreatedToDate);

//
//    @Query(value = """
//             SELECT
//                    msg_id, msg_status,msg_acc_id, msg_message, msg_cost_id, msg_camp_id,msg_thread_id,
//                    msg_retry_count, msg_client_delivery_status,acc_delivery_url
//                    FROM
//                    msg.message_queue_arc,js_core.jsc_accounts
//                    where
//                    msg_client_delivery_status = 'PENDING'
//                    and msg_status != 'SENT'
//                    and msg_acc_id != 3114
//                    and acc_id = msg_acc_id and acc_delivery_url is not null
//                    and msg_delivered_date >= (current_date -2)
//            """, nativeQuery = true)


    Page<MsgMessageQueueArc> findByMsgClientDeliveryStatusAndMsgExternalIdIsNotNullAndMsgCreatedDateGreaterThanEqual(String clientDeliveryStatus, Date yeterdat, Pageable pageable);

    @Query(value = """
            SELECT * FROM msg.message_queue_arc m WHERE cast(m.msg_acc_id as UUID) IN :accountIds
                        AND m.msg_status = :msgStatus
                        AND cast(m.msg_created_date as date) > current_date - 3
            """, nativeQuery = true)
    List<MsgMessageQueueArc> getMsgPendingCreditForReseller(@Param("accountIds") Set<UUID> accountIds, @Param("msgStatus") String msgStatus);


    @Query(value = """
            SELECT * FROM msg.message_queue_arc m WHERE cast(m.msg_acc_id as UUID) = cast( :accountId as UUID)
                        AND m.msg_status = :msgStatus
                        AND cast(m.msg_created_date as date) > current_date - 3
            """, nativeQuery = true)
    List<MsgMessageQueueArc> getMsgPendingCreditForAccount(@Param("accountId") UUID accountId, @Param("msgStatus") String msgStatus);


    @Query(value = """
            SELECT * FROM msg.message_queue_arc m where msg_status = 'SENT' and cast(msg_created_date as date) > current_date - 3
            """, countQuery = """
            SELECT count(*) FROM msg.message_queue_arc m where msg_status = 'SENT' and cast(msg_created_date as date) > current_date - 3
            """, nativeQuery = true)
    Page<MsgMessageQueueArc> getWeiserPendingDNR(Pageable pageable);


    @Query(value = """
            SELECT * FROM msg.message_queue_arc m WHERE cast(m.msg_acc_id as UUID) IN :accountIds
                        AND m.msg_status = :msgStatus
                        AND cast(m.msg_created_date as date) = current_date
            and coalesce( m.msg_sent_retried, true) = false
            """, nativeQuery = true)
    Page<MsgMessageQueueArc> resendSmsPagable(@Param("accountIds") Set<UUID> accountIds, @Param("msgStatus") String msgStatus, Pageable pageable);


    @Query(value = """
            select   * from msg.message_queue_arc
            where msg_status = 'SENT'
             and extract(hour  from msg_created_date ) <= extract(hour from  NOW() AT TIME ZONE 'Africa/Nairobi') - 4
            and cast(msg_created_date as date) = current_date
            and msg_error_desc ilike '%Request processed successfully%'
            and COALESCE(msg_sent_retried,FALSE) = false
            """, nativeQuery = true)
    Page<MsgMessageQueueArc> resendSentStatusAfter4hrs(Pageable pageable);

    @Query(value = """
               select   * from msg.message_queue_arc
                        where msg_status = 'SENT'
                          and msg_message ilike '%MAR-2025%'
                        and cast(msg_created_date as date) > current_date-30
                        and msg_error_desc ilike '%Request processed successfully%'
                          and msg_acc_id = 'e555ca75-2e1f-42f9-a03b-435e2214b4c1'
                        and COALESCE(msg_sent_retried,FALSE) = false;
            """, nativeQuery = true)
    Page<MsgMessageQueueArc> resendSentStatusAfter4hrsMurangaMarch(Pageable pageable);


    List<MsgMessageQueueArc> findAllByMsgExternalIdAndMsgAccIdAndMsgSubMobileNo(String msgExternalId, UUID msgAccId, String msgSubMobileNo);

    List<MsgMessageQueueArc> findByMsgCode(String msgCode);


    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
                UPDATE msg.message_queue_arc
                SET msg_client_delivery_status = 'FAILED', 
                    msg_error_desc = :message, 
                    msg_retry_count = :counter
                WHERE msg_id = :msgId
            """)
    void updateMessageDeliveryToFailed(@Param("msgId") UUID msgId, @Param("message") String message, @Param("counter") int counter);


    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
              update msg.message_queue_arc
              set msg_status = :msgStatus,
              msg_status_code = :msgStatusCode,
                          msg_Status_desc = :msgResponse
              where msg_code in (:msgCode)
                          and msg_status = 'PENDING_PROCESSING'
            """)
    void updateInitialReceiveNote(@Param("msgStatus") String msgStatus, @Param("msgStatusCode") int msgStatusCode, @Param("msgCode") List<String> msgCode, @Param("msgResponse") String msgResponse);


    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
              update msg.message_queue_arc
              set msg_status = :msgStatus,
              msg_delivered_date = now(),
              msg_Request_Id = :msgRequestId
              where msg_code = :msgCode
              and msg_sub_mobile_no =:msisdn
            """)
    void updateDeliverNote(@Param("msgStatus") String msgStatus, @Param("msgRequestId") String msgRequestId, @Param("msisdn") String msisdn, @Param("msgCode") String msgCode);


}


