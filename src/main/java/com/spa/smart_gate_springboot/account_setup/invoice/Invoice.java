package com.spa.smart_gate_springboot.account_setup.invoice;


import com.spa.smart_gate_springboot.dto.Layers;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
@Table(schema = "msg")
@Entity(name = "credit_invoice")
public class Invoice {
    @Id
    @GeneratedValue
    private UUID invoId;
    @Column(unique = true, nullable = false)
    private String invoCode;
    private UUID invoAccId;
    private UUID invoResellerId;
    private String invoPayerName;
    @Enumerated(EnumType.STRING)
    private Layers invoLayer;
    private String invoCreatedByEmail;
    private LocalDateTime invoCreatedDate;
    private LocalDateTime invoDueDate;
    private UUID invoCreatedBy;
    @Enumerated(EnumType.STRING)
    private InvoStatus invoStatus;
    private BigDecimal invoAmount;
    private BigDecimal invoTaxRate;
    private BigDecimal invoAmountAfterTax;
    private String invoPayerMobileNumber;

    /** Safaricom CheckoutRequestID returned when the STK push is launched; used to match the stkCallback back to this invoice. */
    @Column(unique = true)
    private String invoCheckoutRequestId;
    /** ResultDesc from a failed/cancelled stkCallback, kept for support/audit. */
    private String invoFailureReason;
    /** M-Pesa receipt number reported on a successful stkCallback (settlement still flows through the C2B confirmation). */
    private String invoMpesaReceipt;


    private String invoMarkedPaidByEmail;
    private UUID invoMarkedPaidById;
    private LocalDateTime invoMarkedPaidDate;
    private String invoMarkedPaidReference;
    private Date invoMarkedPaidValueDate;
    private BigDecimal invoMarkedPaidAmount;

    private String invoMonthName;
    private Integer invoMonthId;
}
