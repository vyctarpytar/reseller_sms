package com.spa.smart_gate_springboot.account_setup.blacklist;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "blacklisted_contacts", schema = "msg")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlackList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bc_id")
    private Long bcId;
    @Column(nullable = false, name = "bc_msisdn", unique = true)
    private String bcMsisdn;
}
