package com.spa.smart_gate_springboot.account_setup.group;


import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
@Table(schema = "js_core")
@Entity(name = "acc_groups")
public class ChGroup {

    @Id
    @GeneratedValue
    private UUID groupId;
    private String groupName;
    private LocalDateTime groupCreationDate;
    private Long groupMembersNo;
    private String groupLeader;
    private String groupDescription;
    @Column(nullable = false)
    private UUID groupAccId;
    private UUID groupCreatedBy;
    private String groupCreatedByName;
    private String groupCode;
}
