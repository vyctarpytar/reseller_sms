package com.spa.smart_gate_springboot.account_setup.smpp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A row of {@code smpp.smpp_connections}. The table is OWNED by the SMPP gateway
 * ({@code sms_smmp_gateway}, entity {@code com.smmp.gateway.entity.SmppConnection}), which reads it on
 * every bind; this service only provisions rows from the portal. Keep the column names identical to
 * the gateway's so {@code ddl-auto: update} here never adds a column of its own.
 *
 * <p>{@code connectionType} / {@code bindType} are enums on the gateway side. They are plain strings
 * here on purpose: a second {@code @Enumerated} mapping would be a second place for the CHECK-constraint
 * trap to bite when a value is added.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "smpp", name = "smpp_connections")
@Entity(name = "smpp_connections")
public class SmppConnection {
    @Id
    @GeneratedValue
    private UUID id;
    private UUID resellerId;
    private UUID accId;
    private String connectionName;
    private String systemId;
    private String password;
    private String connectionType;
    private String bindType;
    // CLIENT rows only: the upstream SMSC we bind out to.
    private String host;
    private Integer port;
    // SERVER rows only (informational; the gateway listens on its own configured port).
    private Integer bindPort;
    private Integer maxConnections;
    private Integer enquireLinkTimer;
    private Integer responseTimeout;
    private Integer bindTimeout;
    private Boolean isActive;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
