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
@Table(schema = "msg", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"sh_code", "sh_reseller_id", "sh_msn_provider"})})
@Entity(name = "shortcode")
public class ShortCode {
    @Id
    @GeneratedValue
    private UUID shId;
    /**
     * NOT globally unique: the same sender ID string is registered once per network it lives on
     * (MERIDIANBET on Safaricom and on Airtel are two rows), and two resellers may hold the same
     * name. Uniqueness is the (code, reseller, network) constraint on the table.
     */
    @Column(name = "sh_code", nullable = false)
    @NotNull(message = "shCode field cannot be Empty")
    private String shCode;
    private String shUser;
    private String shPassword;
    private String shCampaignId;
    private String shChannel;
    private String shPrsp;

    @Column(name = "sh_reseller_id", nullable = false, updatable = false)
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

    /**
     * Mobile service network this sender ID belongs to. Nullable because rows created before the
     * column existed have no value — they are backfilled to {@link MsnProvider#SAFARICOM} on boot.
     */
    @Column(name = "sh_msn_provider")
    @Enumerated(EnumType.STRING)
    private MsnProvider shMsnProvider;


}

