package com.spa.smart_gate_springboot.dashboad.annual;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "annual_reports", schema = "msg")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnualReport {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(nullable = false)
    private Integer year;
    
    @Column(nullable = false)
    private Integer quarter; // 1, 2, 3, 4
    
    @Column(nullable = false)
    private UUID accountId;
    
    private String accountName;
    private String resellerName;
    private UUID resellerId;
    
    // Monthly data for the quarter
    private Long month1MessageCount;
    private BigDecimal month1Revenue;
    private Long month1DeliveredCount;
    private Long month1FailedCount;
    
    private Long month2MessageCount;
    private BigDecimal month2Revenue;
    private Long month2DeliveredCount;
    private Long month2FailedCount;
    
    private Long month3MessageCount;
    private BigDecimal month3Revenue;
    private Long month3DeliveredCount;
    private Long month3FailedCount;
    
    // Quarter totals
    private Long quarterTotalMessages;
    private BigDecimal quarterTotalRevenue;
    private Long quarterDeliveredCount;
    private Long quarterFailedCount;
    private BigDecimal quarterDeliveryRate; // Percentage
    
    // Additional metrics
    private BigDecimal averageMessageCost;
    private Long uniqueCustomerCount;
    private String topPerformingMonth;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    private String status; // PROCESSING, COMPLETED, FAILED
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "PROCESSING";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
