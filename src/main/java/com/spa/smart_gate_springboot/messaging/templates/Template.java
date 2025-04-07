package com.spa.smart_gate_springboot.messaging.templates;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@ToString
@Table(schema = "msg", uniqueConstraints = {@UniqueConstraint(columnNames = {"tmpAccById", "tmpMessage"})})
@Entity(name = "msg_templates")
@Builder
public class Template {
    @Id
    @GeneratedValue
    private UUID tmpId;
    @Column(updatable = false)
    private UUID tmpCreatedById;
    private UUID tmpAccById;
    private UUID tmpResellerById;
    @Column(updatable = false)
    private LocalDateTime tmpCreatedOn;
    @Column(insertable = false)
    private LocalDateTime tmpUpdatedOn;
    @Column(insertable = false)
    private UUID tmpUpdatedBy;
    private String tmpName;
    private String tmpMessage;
    private String tmpAccName;
    private String tmpResellerName;
}

