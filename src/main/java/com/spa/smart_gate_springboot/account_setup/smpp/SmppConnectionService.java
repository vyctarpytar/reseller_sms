package com.spa.smart_gate_springboot.account_setup.smpp;

import com.spa.smart_gate_springboot.account_setup.account.AcStatus;
import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountRepository;
import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.account_setup.reseller.ResellerRepo;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.user.Role;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.utils.AppTime;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Provisions the rows the SMPP gateway ({@code sms_smmp_gateway}) works from. Two kinds:
 * <ul>
 *   <li><b>{@code SERVER}</b> — a downstream client binds to us with credentials issued here. Tied to
 *       the account its traffic is billed against ({@code acc_id} becomes {@code msgAccId} on every
 *       {@code submit_sm} the gateway hands to {@code MQReceiverSynq}). Resellers manage their own.</li>
 *   <li><b>{@code CLIENT}</b> — we bind out to an upstream SMSC (AIM is one) with credentials <i>they</i>
 *       issued. TOP only: it makes our gateway open a socket to an arbitrary host, and its credentials
 *       are the platform's, not the reseller's. The gateway picks one per send by tenant — the
 *       account's own upstream if it has one, else its reseller's, lowest {@code priority} first — so
 *       nothing anywhere names a connection UUID.</li>
 * </ul>
 *
 * <p>Rules that follow from how the gateway works, not from taste:
 * <ul>
 *   <li><b>{@code system_id} is immutable and rows are never deleted.</b> The gateway caches
 *       system_id -> (connection, reseller, account) in Redis for 30 minutes, so a renamed or recycled
 *       system_id would bill the previous owner until that entry expired. Pause instead.</li>
 *   <li><b>Credential lengths are SMPP 3.4's, not the column widths:</b> system_id is at most 15
 *       characters and password at most 8. jsmpp rejects the bind PDU past those.</li>
 *   <li><b>Edits to a CLIENT row reach the gateway on its next bind</b>, not instantly: it keeps the
 *       session it already holds, and the config it bound with, in memory.</li>
 *   <li><b>Pausing ({@code is_active = false}) does reach live sessions:</b> the gateway's
 *       {@code ConnectionPauseEnforcer} polls the table and drops any bound session of a paused row
 *       within ~10s, then refuses its rebinds. Nothing here calls the gateway.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmppConnectionService {
    private static final String SERVER = "SERVER";
    private static final String CLIENT = "CLIENT";
    private static final Set<String> BIND_TYPES = Set.of("TRANSCEIVER", "TRANSMITTER", "RECEIVER");
    // What we issue: easy to read out and retype.
    private static final Pattern ISSUED_SYSTEM_ID = Pattern.compile("^[A-Za-z0-9_.-]{3,15}$");
    private static final Pattern ISSUED_PASSWORD = Pattern.compile("^[\\x21-\\x7E]{6,8}$");
    // What an upstream issued us: only the protocol's own limits apply.
    private static final Pattern UPSTREAM_SYSTEM_ID = Pattern.compile("^[\\x21-\\x7E]{1,15}$");
    private static final Pattern UPSTREAM_PASSWORD = Pattern.compile("^[\\x21-\\x7E]{1,8}$");
    private static final Pattern HOST = Pattern.compile("^[A-Za-z0-9]([A-Za-z0-9.-]{0,98}[A-Za-z0-9])?$");

    private final SmppConnectionRepo smppConnectionRepo;
    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final ResellerRepo resellerRepo;

    /** Where clients point their bind. Shown next to the credentials so the reseller can share all four. */
    @Value("${smpp.gateway.public-host}")
    private String gatewayHost;
    @Value("${smpp.gateway.port}")
    private Integer gatewayPort;

    /**
     * Stamp {@code acc_smpp_enabled} on accounts whose connections predate the column (the rows the
     * gateway's hand-run SQL scripts created). Idempotent, and never allowed to block startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void backfillSmppEnabled() {
        try {
            int updated = accountRepository.backfillSmppEnabled();
            if (updated > 0) {
                log.info("[SMPP] flagged {} account(s) that already had an active SMPP connection", updated);
            }
        } catch (Exception e) {
            log.error("[SMPP] acc_smpp_enabled backfill failed — flags may be stale until a connection is saved", e);
        }
    }

    public StandardJsonResponse list(User user, String resellerId, String accountId) {
        requireNotAccountLayer(user);
        // Validates a reseller's drill-down; TOP may look at any account.
        UUID accScope = accountService.resolveAccountScope(user, accountId);

        List<SmppConnection> connections;
        if (accScope != null) {
            connections = smppConnectionRepo.findByAccIdOrderByCreatedAtDesc(accScope);
        } else if (user.getLayer().equals(Layers.RESELLER)) {
            connections = smppConnectionRepo.findByResellerIdOrderByCreatedAtDesc(user.getUsrResellerId());
        } else if (!TextUtils.isEmpty(resellerId)) {
            connections = smppConnectionRepo.findByResellerIdOrderByCreatedAtDesc(UUID.fromString(resellerId));
        } else {
            connections = smppConnectionRepo.findAllByOrderByCreatedAtDesc();
        }
        // Upstream rows carry the platform's own SMSC credentials: never hand them to a reseller.
        boolean top = user.getLayer().equals(Layers.TOP);
        connections = connections.stream().filter(c -> top || SERVER.equals(c.getConnectionType())).toList();

        Map<UUID, String> accNames = accountRepository
                .findAllById(connections.stream().map(SmppConnection::getAccId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Account::getAccId, Account::getAccName));
        List<SmppConnectionDto> result = connections.stream()
                .map(c -> toDto(c, accNames.get(c.getAccId())))
                .toList();

        StandardJsonResponse resp = new StandardJsonResponse();
        resp.setData("result", result, resp);
        resp.setTotal(result.size());
        return resp;
    }

    /** @param resellerId the reseller a TOP user is drilled into; only consulted for an account-less CLIENT row */
    @Transactional
    public StandardJsonResponse save(SmppConnectionDto dto, User user, String resellerId) {
        requireNotAccountLayer(user);
        if (Role.VIEWER.equals(user.getRole())) {
            throw new AccessDeniedException("View-only users cannot manage SMPP connections");
        }
        if (dto.getId() != null) {
            return update(dto, user);
        }
        return CLIENT.equals(dto.getConnectionType()) ? createUpstream(dto, user, resellerId) : create(dto, user);
    }

    private StandardJsonResponse create(SmppConnectionDto dto, User user) {
        if (!TextUtils.isEmpty(dto.getConnectionType()) && !SERVER.equals(dto.getConnectionType())) {
            return failure("Connection type must be SERVER or CLIENT");
        }
        if (dto.getAccId() == null) {
            return failure("Select the account this connection belongs to");
        }
        Account account = ownedAccount(dto.getAccId(), user);
        String unusable = unusableAccount(account);
        if (unusable != null) {
            return failure(unusable);
        }

        String systemId = trimmed(dto.getSystemId());
        if (!ISSUED_SYSTEM_ID.matcher(systemId).matches()) {
            return failure("Username must be 3-15 characters: letters, digits, '_', '-' or '.'");
        }
        String invalid = validatePasswordAndBindType(dto, SERVER);
        if (invalid != null) {
            return failure(invalid);
        }
        if (smppConnectionRepo.existsBySystemIdIgnoreCase(systemId)) {
            return failure("Username '" + systemId + "' is already taken — choose another");
        }

        SmppConnection connection = base(dto, systemId, account.getAccResellerId())
                .accId(account.getAccId())
                .connectionName(connectionName(dto, account.getAccName() + " - SMPP"))
                .connectionType(SERVER)
                .bindPort(gatewayPort)
                .build();
        connection = smppConnectionRepo.saveAndFlush(connection);
        syncAccountFlag(account.getAccId());

        log.info("[SMPP] connection {} created for acc={} by user={}", systemId, account.getAccId(), user.getUsrId());
        return success(connection, account.getAccName(), "SMPP connection created");
    }

    private StandardJsonResponse createUpstream(SmppConnectionDto dto, User user, String resellerId) {
        requireTop(user);

        // An upstream belongs to one account, or — with no account — to every account of the reseller.
        Account account = null;
        UUID ownerResellerId;
        if (dto.getAccId() != null) {
            account = accountService.findByAccId(dto.getAccId());
            String unusable = unusableAccount(account);
            if (unusable != null) {
                return failure(unusable);
            }
            ownerResellerId = account.getAccResellerId();
        } else if (!TextUtils.isEmpty(resellerId) && resellerRepo.existsById(UUID.fromString(resellerId))) {
            ownerResellerId = UUID.fromString(resellerId);
        } else {
            return failure("Open the reseller this upstream belongs to, or select an account");
        }

        String systemId = trimmed(dto.getSystemId());
        if (!UPSTREAM_SYSTEM_ID.matcher(systemId).matches()) {
            return failure("System ID must be 1-15 characters with no spaces (SMPP allows at most 15)");
        }
        String invalid = validatePasswordAndBindType(dto, CLIENT);
        if (invalid == null) {
            invalid = validateUpstreamAddress(dto);
        }
        if (invalid != null) {
            return failure(invalid);
        }
        if (smppConnectionRepo.existsBySystemIdIgnoreCase(systemId)) {
            return failure("System ID '" + systemId + "' is already used by another connection");
        }

        SmppConnection connection = base(dto, systemId, ownerResellerId)
                .accId(account == null ? null : account.getAccId())
                .connectionName(connectionName(dto, "Upstream " + dto.getHost().trim()))
                .connectionType(CLIENT)
                .host(dto.getHost().trim())
                .port(dto.getPort())
                .build();
        connection = smppConnectionRepo.saveAndFlush(connection);

        log.info("[SMPP] upstream {} -> {}:{} created for reseller={} acc={} by user={}", systemId,
                connection.getHost(), connection.getPort(), ownerResellerId, connection.getAccId(), user.getUsrId());
        return success(connection, account == null ? null : account.getAccName(), "Upstream SMPP connection created");
    }

    private StandardJsonResponse update(SmppConnectionDto dto, User user) {
        SmppConnection connection = smppConnectionRepo.findById(dto.getId()).orElse(null);
        if (connection == null) {
            return failure("We couldn't find that SMPP connection");
        }
        boolean upstream = CLIENT.equals(connection.getConnectionType());
        Account account = null;
        if (upstream) {
            requireTop(user);
        } else {
            account = ownedAccount(connection.getAccId(), user);
        }

        if (dto.getSystemId() != null && !dto.getSystemId().trim().equals(connection.getSystemId())) {
            return failure("The username cannot be changed once saved — pause this connection and add a new one");
        }
        if (dto.getPassword() == null) {
            dto.setPassword(connection.getPassword());
        }
        String invalid = validatePasswordAndBindType(dto, connection.getConnectionType());
        if (invalid != null) {
            return failure(invalid);
        }
        if (upstream && (dto.getHost() != null || dto.getPort() != null)) {
            if (dto.getHost() == null) dto.setHost(connection.getHost());
            if (dto.getPort() == null) dto.setPort(connection.getPort());
            invalid = validateUpstreamAddress(dto);
            if (invalid != null) {
                return failure(invalid);
            }
            connection.setHost(dto.getHost().trim());
            connection.setPort(dto.getPort());
        }

        connection.setPassword(dto.getPassword());
        connection.setBindType(TextUtils.isEmpty(dto.getBindType()) ? connection.getBindType() : dto.getBindType());
        if (!TextUtils.isEmpty(dto.getConnectionName())) {
            connection.setConnectionName(connectionName(dto, null));
        }
        if (dto.getPriority() != null) {
            connection.setPriority(Math.max(1, dto.getPriority()));
        }
        boolean wasActive = Boolean.TRUE.equals(connection.getIsActive());
        if (dto.getIsActive() != null) {
            connection.setIsActive(dto.getIsActive());
        }
        boolean nowActive = Boolean.TRUE.equals(connection.getIsActive());
        connection.setUpdatedAt(AppTime.now());
        connection = smppConnectionRepo.saveAndFlush(connection);
        if (!upstream) {
            syncAccountFlag(connection.getAccId());
        }

        log.info("[SMPP] connection {} updated (active={}) by user={}", connection.getSystemId(), connection.getIsActive(), user.getUsrId());
        String message = wasActive == nowActive ? "SMPP connection updated"
                : nowActive ? "SMPP connection activated"
                : "SMPP connection paused — a live session is dropped within a few seconds";
        return success(connection, accName(account, connection), message);
    }

    /** Fields every new row shares, whichever way it faces. */
    private SmppConnection.SmppConnectionBuilder base(SmppConnectionDto dto, String systemId, UUID resellerId) {
        return SmppConnection.builder()
                .resellerId(resellerId)
                .systemId(systemId)
                .password(dto.getPassword())
                .bindType(TextUtils.isEmpty(dto.getBindType()) ? "TRANSCEIVER" : dto.getBindType())
                .maxConnections(5)
                .enquireLinkTimer(30)
                .responseTimeout(60)
                .bindTimeout(5000)
                .isActive(true)
                .priority(dto.getPriority() == null ? 1 : Math.max(1, dto.getPriority()))
                .createdAt(AppTime.now())
                .updatedAt(AppTime.now());
    }

    /**
     * The account flag mirrors "has at least one active SERVER connection"; it is never set
     * independently, and an upstream (CLIENT) row does not make an account an SMPP client.
     */
    private void syncAccountFlag(UUID accId) {
        accountRepository.updateSmppEnabled(accId,
                smppConnectionRepo.existsByAccIdAndConnectionTypeAndIsActiveTrue(accId, SERVER));
    }

    private Account ownedAccount(UUID accId, User user) {
        Account account = accountService.findByAccId(accId);
        if (user.getLayer().equals(Layers.RESELLER)
                && !Objects.equals(account.getAccResellerId(), user.getUsrResellerId())) {
            throw new AccessDeniedException("Account does not belong to your reseller");
        }
        return account;
    }

    private String unusableAccount(Account account) {
        if (AcStatus.DELETED.equals(account.getAccStatus())) {
            return "That account has been deleted";
        }
        if (account.getAccResellerId() == null) {
            return "That account is not attached to a reseller";
        }
        return null;
    }

    private void requireNotAccountLayer(User user) {
        if (user.getLayer().equals(Layers.ACCOUNT)) {
            throw new AccessDeniedException("SMPP connections are managed by the reseller");
        }
    }

    private void requireTop(User user) {
        if (!user.getLayer().equals(Layers.TOP)) {
            throw new AccessDeniedException("Upstream SMPP connections are managed by the platform operator");
        }
    }

    private String validatePasswordAndBindType(SmppConnectionDto dto, String connectionType) {
        boolean upstream = CLIENT.equals(connectionType);
        Pattern pattern = upstream ? UPSTREAM_PASSWORD : ISSUED_PASSWORD;
        if (dto.getPassword() == null || !pattern.matcher(dto.getPassword()).matches()) {
            return upstream
                    ? "Password must be 1-8 characters with no spaces (SMPP allows at most 8)"
                    : "Password must be 6-8 characters with no spaces (SMPP allows at most 8)";
        }
        if (!TextUtils.isEmpty(dto.getBindType()) && !BIND_TYPES.contains(dto.getBindType())) {
            return "Bind mode must be one of TRANSCEIVER, TRANSMITTER or RECEIVER";
        }
        return null;
    }

    private String validateUpstreamAddress(SmppConnectionDto dto) {
        if (dto.getHost() == null || !HOST.matcher(dto.getHost().trim()).matches()) {
            return "Enter the upstream SMSC host as an IP address or hostname";
        }
        if (dto.getPort() == null || dto.getPort() < 1 || dto.getPort() > 65535) {
            return "Enter the upstream SMSC port (1-65535)";
        }
        return null;
    }

    private static String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    private String connectionName(SmppConnectionDto dto, String fallback) {
        String name = TextUtils.isEmpty(dto.getConnectionName()) ? fallback : dto.getConnectionName().trim();
        return name.length() > 100 ? name.substring(0, 100) : name;
    }

    private String accName(Account account, SmppConnection connection) {
        if (account != null) {
            return account.getAccName();
        }
        return connection.getAccId() == null ? null
                : accountRepository.findById(connection.getAccId()).map(Account::getAccName).orElse(null);
    }

    private SmppConnectionDto toDto(SmppConnection c, String accName) {
        boolean upstream = CLIENT.equals(c.getConnectionType());
        return SmppConnectionDto.builder()
                .id(c.getId())
                .accId(c.getAccId())
                .accName(accName)
                .connectionName(c.getConnectionName())
                .connectionType(c.getConnectionType())
                .systemId(c.getSystemId())
                .password(c.getPassword())
                .bindType(c.getBindType())
                .isActive(Boolean.TRUE.equals(c.getIsActive()))
                .priority(c.getPriority())
                // SERVER: where the client connects to us. CLIENT: where we connect to them.
                .host(upstream ? c.getHost() : gatewayHost)
                .port(upstream ? c.getPort() : gatewayPort)
                .createdAt(c.getCreatedAt())
                .build();
    }

    private StandardJsonResponse success(SmppConnection connection, String accName, String message) {
        StandardJsonResponse resp = new StandardJsonResponse();
        resp.setData("result", toDto(connection, accName), resp);
        resp.setMessage("message", message, resp);
        return resp;
    }

    private StandardJsonResponse failure(String message) {
        StandardJsonResponse resp = new StandardJsonResponse();
        resp.setSuccess(false);
        resp.setStatus(400);
        resp.setMessage("message", message, resp);
        return resp;
    }
}
