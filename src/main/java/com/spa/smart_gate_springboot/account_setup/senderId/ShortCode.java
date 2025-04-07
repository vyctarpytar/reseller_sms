package com.spa.smart_gate_springboot.account_setup.senderId;

import com.spa.smart_gate_springboot.account_setup.shortsetup.ShPriority;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@ToString
@Table(schema = "msg")
@Entity(name = "shortcode")
public class ShortCode {
    @Id
    @GeneratedValue
    private UUID shId;
    @Column(unique = true, nullable = false)
    @NotNull(message = "shCode field cannot be Empty")
    private String shCode;
    private String shUser;
    private String shPassword;
    private String shCampaignId;
    private String shChannel;
    private String shPrsp;

    @Column(nullable = false,updatable = false)
    @NotNull(message = "shResellerId field cannot be Empty")
    private UUID shResellerId;

    @Column(nullable = false)
    @NotNull(message = "shStatus field cannot be Empty")
    @Enumerated(EnumType.STRING)
    private ShStatus shStatus;

    @Column(nullable = false)
    @NotNull(message = "shPriority field cannot be Empty")
    @Enumerated(EnumType.STRING)
    private ShPriority shPriority;


    private UUID shCreatedById;
    private String shCreatedByname;
    private LocalDateTime shCreatedDate;

    @Column(nullable = false)
    @NotNull(message = "field cannot be Empty")
    private String shSenderType;


}

