package com.spa.smart_gate_springboot.dashboad.annual;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AnnualReportDto {
    private UUID id;
    private Integer year;
    private Integer quarter;
    private UUID accountId;
    private String accountName;
    private String resellerName;
    private UUID resellerId;
    private Long validityPeriod;
    private String senderId;
    private String senderIdProvider;
    
    // Monthly breakdown
    private MonthlyData month1;
    private MonthlyData month2;
    private MonthlyData month3;
    
    // Quarter totals
    private Long quarterTotalMessages;
    private BigDecimal quarterTotalRevenue;
    private Long quarterDeliveredCount;
    private Long quarterFailedCount;
    private BigDecimal quarterDeliveryRate;
    
    // Additional metrics
    private BigDecimal averageMessageCost;
    private Long uniqueCustomerCount;
    private String topPerformingMonth;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    public static class MonthlyData {
        private String monthName;
        private Long messageCount;
        private BigDecimal revenue;
        private Long deliveredCount;
        private Long failedCount;
        private BigDecimal deliveryRate;
    }
}
