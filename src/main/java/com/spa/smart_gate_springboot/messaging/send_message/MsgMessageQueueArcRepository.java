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
            AND ( cast(:msgStatusCsv as text) IS NULL OR msg_status = ANY(string_to_array(:msgStatusCsv, ',')) )
            AND (case when :msgMessage IS not  NULL then  msg_message ilike :msgMessage else 1=1 end )
            AND (case when :msgSubmobileNo IS not  NULL then  msg_sub_mobile_no ilike :msgSubmobileNo else 1=1 end )
                AND (case when :msgSenderName IS not  NULL then   coalesce(msg_sender_id_name,'-1') = :msgSenderName else 1=1 end )
              and (case when cast(:msgResellerId as uuid) is not null then
                  exists(select 1 from js_core.reseller where  msg_reseller_id = rs_id and ( rs_id =:msgResellerId or rs_created_by =:msgResellerId )) else 1=1 end )
            """, nativeQuery = true)
    Page<MsgMessageQueueArc> findByMessagesArcFilters(@Param("msgAccId") UUID msgAccId, @Param("msgResellerId") UUID msgResellerId, @Param("msgGrpId") UUID msgGrpId,
                                                      @Param("msgCreatedDate") Date msgCreatedDate,
                                                      @Param("msgStatus") String msgStatus, @Param("msgStatusCsv") String msgStatusCsv, @Param("msgSubmobileNo") String msgSubmobileNo, @Param("msgMessage") String msgMessage,
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


    /**
     * Messages that have a client callback URL and a delivery report received
     * (msg_delivered_date set), but whose delivery report has not yet been pushed
     * to the client server (msg_client_delivery_status still 'PENDING').
     * A row is only returned if it has never been attempted, or its last attempt
     * was before {@code retryBefore} (i.e. older than the retry interval).
     * Used by the {@code ClientDeliveryResponses} cron.
     */
    @Query(value = """
            SELECT * FROM msg.message_queue_arc
            WHERE msg_callback_url IS NOT NULL
              AND msg_client_delivery_status = 'PENDING'
              AND msg_delivered_date IS NOT NULL
              AND cast(msg_created_date as date) >= :createdOnOrAfter
              AND (msg_last_callback_attempt IS NULL OR msg_last_callback_attempt <= :retryBefore)
            ORDER BY msg_last_callback_attempt ASC NULLS FIRST
            """, nativeQuery = true)
    Page<MsgMessageQueueArc> findPendingClientCallbacks(@Param("retryBefore") java.time.LocalDateTime retryBefore,
                                                        @Param("createdOnOrAfter") java.time.LocalDate createdOnOrAfter,
                                                        Pageable pageable);


    /**
     * Messages that have a client callback URL but were never sent / never received a
     * delivery report (msg_delivered_date IS NULL) and are stuck in a non-deliverable
     * status (e.g. PENDING_CREDIT, RS_CREDIT_ISSUE). Only messages older than
     * {@code stuckBefore} (a grace period) are returned, so transient queue states are
     * not reported as failures. Same per-row retry back-off as the delivery callback.
     * Used by the {@code ClientDeliveryResponses} cron.
     */
    @Query(value = """
            SELECT * FROM msg.message_queue_arc
            WHERE msg_callback_url IS NOT NULL
              AND msg_client_delivery_status = 'PENDING'
              AND msg_delivered_date IS NULL
              AND msg_status IN (:statuses)
              AND msg_created_date <= :stuckBefore
              AND cast(msg_created_date as date) >= :createdOnOrAfter
              AND (msg_last_callback_attempt IS NULL OR msg_last_callback_attempt <= :retryBefore)
            ORDER BY msg_last_callback_attempt ASC NULLS FIRST
            """, nativeQuery = true)
    Page<MsgMessageQueueArc> findStuckClientCallbacks(@Param("statuses") List<String> statuses,
                                                      @Param("stuckBefore") java.time.LocalDateTime stuckBefore,
                                                      @Param("retryBefore") java.time.LocalDateTime retryBefore,
                                                      @Param("createdOnOrAfter") java.time.LocalDate createdOnOrAfter,
                                                      Pageable pageable);

    @Query(value = """
            SELECT * FROM msg.message_queue_arc m WHERE cast(m.msg_acc_id as UUID) = cast( :accountId as UUID)
                        AND m.msg_status = :msgStatus
                        AND cast(m.msg_created_date as date) > :createdAfter
            """, nativeQuery = true)
    List<MsgMessageQueueArc> getMsgPendingCreditForAccount(@Param("accountId") UUID accountId,
                                                           @Param("msgStatus") String msgStatus,
                                                           @Param("createdAfter") java.time.LocalDate createdAfter);





    /**
     * Next batch of failed sends to retry — across ALL tenants in ONE indexed query, oldest first,
     * bounded by {@code :limit}. Replaces the old per-reseller {@code resendSmsPagable}, which the
     * cron called once per (reseller × status) every few seconds, each call first loading the
     * reseller's accounts and then passing them as a giant {@code IN (...)} list — so its cost grew
     * with tenant count.
     * <p>
     * {@code coalesce(msg_sent_retried, true) = false} keeps each message to a single retry, and the
     * {@code msg_created_date} comparison is left un-cast so Postgres can use an index. Wants a
     * supporting index — see the prod note on {@code SchedulingConfig.retryFailedMessages}.
     * <p>
     * <b>OTPs are never retried.</b> A one-time code is only useful for the few seconds around the
     * user's login attempt; re-sending it minutes later just delivers a stale (often already-expired,
     * already-superseded) code and reads as a duplicate/suspicious SMS to the recipient. The exclusion
     * is done in SQL, not in the cron loop, so skipped OTPs don't eat slots out of {@code :limit}.
     * The word-boundary regex ({@code \\yotp\\y}, case-insensitive) matches the standalone word only —
     * it won't swallow a legitimate message containing "otp" inside another word (footpath, hotplate).
     */
    @Query(value = """
            SELECT * FROM msg.message_queue_arc
            WHERE msg_status IN (:statuses)
              AND coalesce(msg_sent_retried, true) = false
              AND msg_created_date >= :createdOnOrAfter
              AND coalesce(msg_message, '') !~* '\\yotp\\y'
            ORDER BY msg_created_date
            LIMIT :limit
            """, nativeQuery = true)
    List<MsgMessageQueueArc> findRetryBatch(@Param("statuses") List<String> statuses,
                                            @Param("createdOnOrAfter") java.time.LocalDateTime createdOnOrAfter,
                                            @Param("limit") int limit);


    /**
     * Claim a whole re-send batch in ONE statement: stamp each row with its freshly minted
     * {@code msg_code}, flip it back to {@code PENDING_PROCESSING} and mark it retried.
     * <p>
     * Replaces the per-message {@code save()} the retry cron used to do inside its send loop. Those
     * entities are <b>detached</b> (the cron isn't transactional, so each repository call gets its own
     * EntityManager), which made every {@code save()} a {@code merge()} — a SELECT + an UPDATE + a
     * commit, per message. A 500-row tick therefore cost ~1500 round-trips before a single SMS left the
     * box; now it costs one.
     * <p>
     * {@code ids} and {@code codes} are positionally-paired CSV lists — same length, same order — zipped
     * back into rows by Postgres' two-argument {@code unnest}, so no SQL is string-built in Java.
     * msgCodes are URL-safe Base64 ({@code UniqueCodeGenerator}), so they can never contain a comma.
     */
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
                UPDATE msg.message_queue_arc a
                SET msg_code = v.code,
                    msg_status = 'PENDING_PROCESSING',
                    msg_sent_retried = true
                FROM unnest(cast(string_to_array(:ids, ',') as uuid[]),
                            string_to_array(:codes, ',')) AS v(id, code)
                WHERE a.msg_id = v.id
            """)
    int markResendDispatching(@Param("ids") String ids, @Param("codes") String codes);


    /**
     * Flag a batch of messages as retried in ONE statement — the Airtel-fallback half of the retry cron,
     * which keeps its existing {@code msg_code} (Airtel mints its own on response) and so only needs the
     * flag. Set <em>before</em> the sends, so a message can't loop if its send blows up.
     */
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
                UPDATE msg.message_queue_arc
                SET msg_sent_retried = true
                WHERE msg_id = ANY (cast(string_to_array(:ids, ',') as uuid[]))
            """)
    int markSentRetried(@Param("ids") String ids);


    @Query(value = """
            select   * from msg.message_queue_arc
            where msg_status = 'SENT'
             and cast(extract(hour from msg_created_date) as integer) <= :maxCreatedHour
            and cast(msg_created_date as date) = :today
            and msg_error_desc ilike '%Request processed successfully%'
            and COALESCE(msg_sent_retried,FALSE) = false
            """, nativeQuery = true)
    Page<MsgMessageQueueArc> resendSentStatusAfter4hrs(@Param("maxCreatedHour") int maxCreatedHour,
                                                       @Param("today") java.time.LocalDate today,
                                                       Pageable pageable);



    List<MsgMessageQueueArc> findByMsgCode(String msgCode);




    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
                UPDATE msg.message_queue_arc
                SET msg_client_delivery_status = 'NOTIFIED',
                    msg_retry_count = 0,
                    msg_last_callback_attempt = :attemptTime
                WHERE msg_id = :msgId
            """)
    void markClientCallbackNotified(@Param("msgId") UUID msgId, @Param("attemptTime") java.time.LocalDateTime attemptTime);


    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
                UPDATE msg.message_queue_arc
                SET msg_retry_count = :counter,
                    msg_last_callback_attempt = :attemptTime
                WHERE msg_id = :msgId
            """)
    void updateClientCallbackRetry(@Param("msgId") UUID msgId, @Param("counter") int counter, @Param("attemptTime") java.time.LocalDateTime attemptTime);


    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
                UPDATE msg.message_queue_arc
                SET msg_client_delivery_status = 'CALLBACK_FAILED',
                    msg_retry_count = :counter,
                    msg_last_callback_attempt = :attemptTime
                WHERE msg_id = :msgId
            """)
    void markClientCallbackFailed(@Param("msgId") UUID msgId, @Param("counter") int counter, @Param("attemptTime") java.time.LocalDateTime attemptTime);


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
              msg_delivered_date = :deliveredDate,
              msg_Request_Id = :msgRequestId
              where msg_code = :msgCode
              and msg_sub_mobile_no =:msisdn
            """)
    void updateDeliverNote(@Param("msgStatus") String msgStatus,
                           @Param("msgRequestId") String msgRequestId,
                           @Param("msisdn") String msisdn,
                           @Param("msgCode") String msgCode,
                           @Param("deliveredDate") java.time.LocalDateTime deliveredDate);


    /**
     * Whether this msisdn has ever been reported {@code DeliveredToTerminal} — the carrier's terminal
     * success status. Used by the Airtel path to avoid learning a number into {@code msg.airtel_numbers}
     * that Safaricom has already delivered to.
     *
     * <p>Recipients are stored exactly as they were submitted (0722…, 254722…, +254722…), so callers
     * pass every equivalent form of the number and we match on the set.
     *
     * <p>Served by the partial index {@code idx_mqa_delivered_msisdn} — see
     * {@code db/performance_indexes.sql}; without it this seq-scans the arc table on the send path.
     */
    @Query(nativeQuery = true, value = """
            select exists(
                select 1 from msg.message_queue_arc
                where msg_status = 'DeliveredToTerminal'
                  and msg_sub_mobile_no in (:msisdns)
            )
            """)
    boolean existsDeliveredToTerminal(@Param("msisdns") Collection<String> msisdns);


}


