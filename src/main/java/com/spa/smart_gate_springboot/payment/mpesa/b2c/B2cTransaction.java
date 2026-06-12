package com.spa.smart_gate_springboot.payment.mpesa.b2c;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Records a B2C payout (a wallet withdrawal leg). Created PROCESSING when the gateway accepts it,
 * then flipped to SUCCESS/FAILED by the status poller.
 *
 * <p>{@code status} and {@code purpose} are STRING enums — pre-drop their CHECK constraints on every DB
 * (see the project's ddl-auto:update rule).
 */
@Entity
@Table(name = "b2c_transaction", schema = "msg",
        indexes = {
                @Index(name = "idx_b2c_conversation", columnList = "conversation_id"),
                @Index(name = "idx_b2c_status", columnList = "status"),
                @Index(name = "idx_b2c_ext_ref", columnList = "external_ref", unique = true)
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class B2cTransaction {

    @Id
    @GeneratedValue
    private UUID id;

    /** Wallet the payout was debited from (RS_&lt;rsId&gt; or TOP_PLATFORM). */
    @Column(name = "wallet_code", nullable = false)
    private String walletCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private B2cPurpose purpose;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    /** Net amount sent to the recipient (gross − charge). */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /** M-Pesa service charge withheld from the gross. */
    @Column(precision = 19, scale = 4)
    private BigDecimal charge;

    @Column(name = "conversation_id")
    private String conversationId;

    @Column(name = "originator_conversation_id")
    private String originatorConversationId;

    @Column(name = "mpesa_receipt")
    private String mpesaReceipt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private B2cTransactionStatus status;

    @Column(name = "response_code")
    private String responseCode;

    @Column(name = "response_description")
    private String responseDescription;

    @Column(name = "failure_reason")
    private String failureReason;

    /**
     * True once the wallet debit has been credited back (either auto-reversed on initiation failure,
     * or admin-reversed). Prevents a FAILED row from being reversed twice.
     */
    @Column(name = "reversed", nullable = false)
    @Builder.Default
    private boolean reversed = false;

    /** Idempotency / correlation key shared with the wallet ledger lines. */
    @Column(name = "external_ref", length = 120)
    private String externalRef;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_by_email")
    private String createdByEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
