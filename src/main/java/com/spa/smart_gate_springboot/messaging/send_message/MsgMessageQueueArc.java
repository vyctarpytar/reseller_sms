package com.spa.smart_gate_springboot.messaging.send_message;

import com.spa.smart_gate_springboot.utils.AppTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@ToString
/**
 * The archive row for one SMS send — the source of truth for billing, retries and DLRs.
 *
 * <p><b>Created date.</b> {@link #msgCreatedDate} / {@link #msgCreatedTime} are stamped in
 * {@link #stampCreated()} from {@link AppTime#now()}, i.e. this JVM's clock in {@code Africa/Nairobi}.
 * They used to carry Hibernate's {@code @CreationTimestamp}, which reads the ambient VM default zone
 * ({@code Clock.systemDefaultZone()}, captured at bootstrap) — correct only for as long as nothing
 * moves that process-global. Stamping explicitly makes the zone impossible to drift, and keeps this
 * column consistent with every other timestamp the platform writes.
 */
@Table(schema = "msg")
@Entity(name = "message_queue_arc")
public class MsgMessageQueueArc {
    @Id
    @GeneratedValue
    private UUID msgId;
    private String msgExternalId;
    /**
     * Stable idempotency key for one logical send (set at publish time, carried in {@link MsgQueue}).
     * The unique index on this column is what makes processing idempotent: a redelivered message fails
     * the insert ({@code DataIntegrityViolationException}) and is skipped instead of re-debited/re-sent.
     * <p>
     * ddl-auto adds this column automatically, but NOT the unique index — create it manually per env:
     * <pre>
     * CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS uq_mqa_dedup_key
     *   ON msg.message_queue_arc (msg_dedup_key) WHERE msg_dedup_key IS NOT NULL;
     * </pre>
     * Partial (non-null) so the legacy rows that predate this column (all NULL) never collide.
     */
    @Column(length = 200)
    private String msgDedupKey;
    @Column(nullable = false)
    @NotNull(message = "field cannot be null")
    private UUID msgAccId;
    private String msgCode;
    private BigDecimal msgUsrId;
    /** Stamped by {@link #stampCreated()} from {@code AppTime.now()} — see the class javadoc. */
    @Column(nullable = false)
    private LocalDateTime msgCreatedDate;
    private String msgStatus;
    private LocalDateTime msgDeliveredDate;
    private String msgSubMobileNo;
    private BigDecimal msgSubCatId;
    @Column(nullable = false)
    @NotNull(message = "msgSubMobileNo cannot be null")
    private String msgMessage;
    private BigDecimal msgCostId;
    private Long msgCampId;
    private Long msgThreadId;
    private int msgRetryCount;
    @Column(nullable = false)
    @NotNull(message = "msgClientDeliveryStatus cannot be null")
    private String msgClientDeliveryStatus;
    private LocalDateTime msgThreadTime;
    private String msgSenderLevel;
    private String msgErrorCode;
    @Column(length = 10000)
    private String msgErrorDesc;
    /** Same instant as {@link #msgCreatedDate}; kept for the legacy reports that read it. */
    private LocalDateTime msgCreatedTime;
    private String msgWhyResent;
    private Long msgPriorityId;
    private UUID msgCreatedBy;
    private String msgCreatedByEmail;
    private UUID msgGroupId;
    private Boolean msgSentRetried = Boolean.FALSE;

    private String msgAccName;
    private String msgSenderIdName;
    private UUID msgResellerId;
    private String msgResellerName;
    private Integer msgPage;
    private String msgSourceIpAddress;
    private String msgRequestId;

    /**
     * Client server URL supplied via the API. When set, the delivery report for
     * this message is POSTed to this URL by the {@code ClientDeliveryResponses} cron.
     */
    @Column(length = 1000)
    private String msgCallbackUrl;

    /**
     * Timestamp of the last attempt to POST the delivery report to {@link #msgCallbackUrl}.
     * Used to back off failed callbacks (retry at most once every 30 minutes).
     */
    private LocalDateTime msgLastCallbackAttempt;

    /**
     * Stamp the creation timestamps from the JVM clock in EAT, on insert only. Both fields get the
     * same instant. A value already set by the caller wins (the Airtel path sets its own), so this
     * only fills the gap left by {@code BeanUtils.copyProperties}, which cannot carry
     * {@code MsgQueue}'s {@code java.util.Date} into these {@code LocalDateTime} fields.
     */
    @PrePersist
    void stampCreated() {
        LocalDateTime now = AppTime.now();
        if (msgCreatedDate == null) {
            msgCreatedDate = now;
        }
        if (msgCreatedTime == null) {
            msgCreatedTime = now;
        }
    }
}
