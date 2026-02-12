package com.spa.smart_gate_springboot.messaging.send_message.airtel;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;


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
    @Column(nullable = false, name = "an_number")
    private String anNumber;

    @CreationTimestamp
  @Column(name = "an_created_date", updatable = false)
    private LocalDateTime anCreatedDate;
}



