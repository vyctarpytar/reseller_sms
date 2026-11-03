package com.spa.smart_gate_springboot.messaging.send_message.airtel;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


/**
 * A recipient we route to Airtel. A number lands here only when a Safaricom delivery report came back
 * {@code DeliveryImpossible} for it — see {@link AiretelService#recordDeliveryImpossible(String)}.
 */
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@ToString
@Table(schema = "msg")
@Entity(name = "airtel_numbers")
@Builder
public class AirtelNumber {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "an_id")
    private Long anId;
    /** Normalized to 254XXXXXXXXX. */
    @Column(nullable = false, name = "an_number")
    private String anNumber;

    /**
     * Why the number was recorded. Always {@code DeliveryImpossible} for rows written under the current
     * rule; null marks a row learned under the old "every Airtel send" rule, which the boot sweep deletes.
     */
    @Column(name = "an_reason")
    private String anReason;

    @CreationTimestamp
    @Column(name = "an_created_date", updatable = false)
    private LocalDateTime anCreatedDate;
}
