package com.spa.smart_gate_springboot.messaging.operatorPrefix;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@ToString
@Table(schema = "js_core")
@Entity(name = "operator_prefix")
public class OperatorPrefix {
    @Id
    @SequenceGenerator(name = "operator_prefix_op_id_seq", sequenceName = "operator_prefix_op_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long opId;
    private Long opPrefix;
    private String opOperator;
}
