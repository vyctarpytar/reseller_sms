package com.spa.smart_gate_springboot.messaging.send_message.airtel;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;



@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@ToString
@Table(schema = "msg" ,uniqueConstraints = {@UniqueConstraint(columnNames = {"an_number"})})
@Entity(name = "airtel_numbers")
@Builder
public class AirtelNumber {
    @Id
    @GeneratedValue
    @Column(name = "an_id")
    private Long anId;
    @Column(nullable = false, name = "an_number")
    private String anNumber;

    @CreatedDate
  @Column(name = "an_created_date")
    private LocalDateTime anCreatedDate;
}



