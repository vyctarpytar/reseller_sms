package com.spa.smart_gate_springboot.account_setup.smpp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** Request and response shape for the portal's SMPP connection screen. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmppConnectionDto {
    private UUID id;
    private UUID accId;
    private String connectionName;
    private String systemId;
    private String password;
    private String bindType;
    private Boolean isActive;
    /** SERVER (the client binds to us; default) or CLIENT (we bind out to an upstream SMSC; TOP only). */
    private String connectionType;
    /**
     * For a CLIENT connection: the upstream SMSC, supplied on save. For a SERVER connection: our own
     * gateway, filled in on the way out and ignored on the way in.
     */
    private String host;
    private Integer port;
    /** CLIENT only: lower goes first when a tenant has several upstreams. Defaults to 1. */
    private Integer priority;

    // Response only.
    private String accName;
    private LocalDateTime createdAt;
}
