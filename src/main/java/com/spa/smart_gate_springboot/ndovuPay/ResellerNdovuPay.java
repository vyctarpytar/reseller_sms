package com.spa.smart_gate_springboot.ndovuPay;

import com.spa.smart_gate_springboot.dto.Layers;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
@Table(schema = "msg")
@Entity(name = "ndovu_pay_config")
public class ResellerNdovuPay{
    @Id
    @GeneratedValue
    private UUID ndId;
    @Column(unique = true, updatable = false)
    private UUID ndResellerId;
    private  String ndDisbursement;
    private  int ndOrganization;
    private  String ndCollection;
    private  String ndBranch;

    @Enumerated(EnumType.STRING)
    private Layers ndLayers;

}
