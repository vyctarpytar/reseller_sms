package com.spa.smart_gate_springboot.ndovuPay.withdraw;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
@Table(schema = "msg")
@Entity(name = "ndovu_pay_withdraw")
public class WithDrawRequest {
    @Id
    @GeneratedValue
    private UUID withDrawId;
    private BigDecimal withDrawAmount;
    private String withDrawPhoneNumber;
    private String withDrawCode;
    private LocalDateTime withDrawCreatedDate;
    private String withDrawCreatedByEmail;
    private UUID withDrawCreatedById;
    @Column(updatable = false, nullable = false)
    private UUID withDrawResellerId;
    @Enumerated(EnumType.STRING)
    private WithDrawStatus withDrawStatus;
    private String withDrawErrorDesc;
}
