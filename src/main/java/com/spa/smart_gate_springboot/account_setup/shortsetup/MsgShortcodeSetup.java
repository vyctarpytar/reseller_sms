package com.spa.smart_gate_springboot.account_setup.shortsetup;

import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.senderId.MsnProvider;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
    @Table(schema = "msg",uniqueConstraints = {@UniqueConstraint(columnNames = {"sh_code", "sh_acc_id"})})
@Entity(name = "shortcode_setup")
public class MsgShortcodeSetup {
    @Id
    @GeneratedValue
    private UUID shId;
    @Column(unique = true, nullable = false,name = "sh_code")
    @NotNull(message = "field cannot be Empty")
    private String shCode;
    private String shUser;
    private String shPassword;
    private String shCampaignId;
    private String shRequestUrl;
    private String shChannel;
    private String shResponseUrl;
    @Column(name = "sh_acc_id")
    private UUID shAccId;
    private String shPrsp;
    @Column(nullable = false)
    @NotNull(message = "field cannot be Empty")
    @Enumerated(EnumType.STRING)
    private ShPriority shPriority;
    @Column(nullable = false)
    @NotNull(message = "field cannot be Empty")
    private UUID shResellerId;

    @Column(nullable = false)
    @NotNull(message = "field cannot be Empty")
    @Enumerated(EnumType.STRING)
    private ShStatus shStatus;
    private UUID shMappedById;
    private String shCodeAlt;
    private String shPasswordAlt;
    private String shProviderAlt;

    @Column(nullable = false)
    @NotNull(message = "field cannot be Empty")
    private String shSenderType;

    /**
     * Carried over from the {@code msg.shortcode} row this mapping was copied from, so the send
     * path can pick the account's Airtel sender ID without a second hop through the registry.
     */
    @Column(name = "sh_msn_provider")
    @Enumerated(EnumType.STRING)
    private MsnProvider shMsnProvider;


}

