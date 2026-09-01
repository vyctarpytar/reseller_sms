package com.spa.smart_gate_springboot.account_setup.senderId;

import com.spa.smart_gate_springboot.utils.AppTime;


import com.spa.smart_gate_springboot.account_setup.account.AcStatus;
import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.account_setup.request.ReStatus;
import com.spa.smart_gate_springboot.account_setup.request.RequestEntity;
import com.spa.smart_gate_springboot.account_setup.request.RequestService;
import com.spa.smart_gate_springboot.account_setup.shortsetup.MsgShortcodeSetupService;
import com.spa.smart_gate_springboot.account_setup.shortsetup.ShPriority;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.utils.GlobalUtils;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShortCodeService {
    private final ShortCodeRepository shortCodeRepository;
    private final MsgShortcodeSetupService msgShortcodeSetupService;
    private final RequestService requestService;
    private final AccountService accountService;
    private final GlobalUtils globalUtils;
    private final JdbcTemplate jdbcTemplate;


    /**
     * Stamp the network on sender IDs that predate the {@code sh_msn_provider} column (all of them
     * Safaricom), so provider filtering never has to special-case null. Idempotent — it re-runs on
     * every boot and must never take the app down with it, hence the swallowed exception.
     */
    @PostConstruct
    void backfillMsnProvider() {
        try {
            dropLegacySenderIdUniques();
            int shortCodes = shortCodeRepository.backfillMsnProvider();
            int setups = msgShortcodeSetupService.backfillMsnProvider();
            if (shortCodes > 0 || setups > 0) {
                log.info("Backfilled MSN provider = SAFARICOM on {} shortcode(s) and {} setup(s)", shortCodes, setups);
            }
        } catch (Exception e) {
            log.error("MSN provider backfill failed (sender IDs with a null provider will not match a provider filter): {}", e.getMessage(), e);
        }
    }

    /**
     * Drop the uniqueness rules that predate networks — {@code UNIQUE (sh_code)} on the registry and
     * {@code UNIQUE (sh_code, sh_acc_id)} on the mappings, both written when a sender ID name was
     * assumed to live on exactly one network. The entities now declare versions that include
     * {@code sh_msn_provider}, but {@code ddl-auto: update} only ever ADDS constraints, so without
     * this the old ones survive alongside the new and still reject the second network.
     *
     * <p>Matched by shape rather than by name — the names are Hibernate hashes
     * ({@code uk_h7m27r3baog5gv13owm788i90}) that need not agree across environments. The rule is
     * "constrains sh_code but ignores the network", which cannot match the constraints we want to
     * keep. Idempotent, so it re-runs harmlessly on every boot.
     */
    private void dropLegacySenderIdUniques() {
        for (String table : List.of("msg.shortcode", "msg.shortcode_setup")) {
            try {
                List<String> legacy = jdbcTemplate.queryForList("""
                        select c.conname
                          from pg_constraint c
                         where c.conrelid = ?::regclass
                           and c.contype = 'u'
                           and (select a.attnum from pg_attribute a
                                 where a.attrelid = c.conrelid and a.attname = 'sh_code') = any (c.conkey)
                           and (select a.attnum from pg_attribute a
                                 where a.attrelid = c.conrelid and a.attname = 'sh_msn_provider') <> all (c.conkey)
                        """, String.class, table);
                for (String name : legacy) {
                    jdbcTemplate.execute("alter table " + table + " drop constraint if exists \"" + name + "\"");
                    log.warn("Dropped pre-network UNIQUE '{}' on {} — sender ID uniqueness now includes sh_msn_provider", name, table);
                }
            } catch (Exception e) {
                log.error("Could not drop the pre-network UNIQUE on {} — registering a sender ID on a second network will fail there: {}", table, e.getMessage(), e);
            }
        }
    }

    public StandardJsonResponse assignSenderId(UUID reqId, User auth, ShortCode setup) {
        StandardJsonResponse resp = new StandardJsonResponse();

        RequestEntity requestEntity = requestService.findByid(reqId);
        setup.setShResellerId(requestEntity.getReResellerId());
        requestEntity.setReStatus(ReStatus.PROCESSED);
        setup.setShStatus(ShStatus.PENDING_MAPPING);
        shortCodeRepository.saveAndFlush(setup);
        requestEntity.setReSetUpId(setup.getShId());
        requestService.save(requestEntity);
        resp.setData("result", requestEntity, resp);
        resp.setMessage("message", "Sender-Id issued successfully", resp);
        return resp;
    }

    public StandardJsonResponse fetchAllSetups(User user, ShFilterDto filterDto) {
        StandardJsonResponse resp = new StandardJsonResponse();

        if (filterDto.getLimit() == 0) filterDto.setLimit(10);
        filterDto.setSortColumn("sh_code");

        Pageable pageable = PageRequest.of(filterDto.getStart(), filterDto.getLimit(), Sort.by(filterDto.getSortColumn()).descending());

        if (!TextUtils.isEmpty(filterDto.getShSenderId())) {
            filterDto.setShSenderId("%" + filterDto.getShSenderId() + "%");
        }
        if (user.getLayer().equals(Layers.RESELLER)) {
            filterDto.setShResellerId(user.getUsrResellerId());
        }

        globalUtils.printToJson(filterDto, "error");

        Page<ShortCode> pagedData = shortCodeRepository.findAllShortCodes(filterDto.getShResellerId(), filterDto.getShSenderId(), filterDto.getShStatus(), pageable);

        resp.setData("result", pagedData.getContent(), resp);
        resp.setMessage("message", "All setups found", resp);
        resp.setTotal((int) pagedData.getTotalElements());
        return resp;
    }


    public StandardJsonResponse assignAccountToSetUp(UUID accId, User auth, String shCode) {
        StandardJsonResponse resp = new StandardJsonResponse();
        // The UI assigns by name, and a name can be registered on several networks — map every one
        // of them, so an account picking MERIDIANBET can send on Safaricom and Airtel alike.
        List<ShortCode> shortCodes = shortCodeRepository.findByShCodeAndShResellerId(shCode, auth.getUsrResellerId());
        if (shortCodes.isEmpty()) {
            throw new IllegalArgumentException("No short code found for shCode: " + shCode);
        }
        for (ShortCode shortCode : shortCodes) {
            msgShortcodeSetupService.assignShortCodeToAccount(auth, accId, shortCode);
        }


        Account account = accountService.findByAccId(accId);
        account.setAccStatus(AcStatus.ACTIVE);
        accountService.save(account);


        resp.setData("result", account, resp);
        resp.setMessage("message", "Account Mapped Successfully", resp);
        return resp;
    }


    public StandardJsonResponse fetchDistinctResellerSenderNames(User user) {
        StandardJsonResponse response = new StandardJsonResponse();
        List<String> msgQueues = new ArrayList<>();
        if (user.getLayer().equals(Layers.RESELLER)) {
            msgQueues = shortCodeRepository.findDistinctSenderNames(user.getUsrResellerId());
        } else {
            return msgShortcodeSetupService.fetchDistinctResellerSenderNames(user);
        }
        response.setData("result", msgQueues, response);
        response.setTotal(msgQueues.size());
        return response;
    }


    /**
     * The sender ID to send with on one network. The account's own mapping wins; failing that the
     * reseller's registry entry is used, so a reseller that registered e.g. one Airtel sender ID
     * covers all of its accounts without a per-account mapping. Empty when neither exists — the
     * caller decides whether that is fatal or falls back to a configured default.
     */
    public Optional<String> findSenderIdForProvider(UUID accId, UUID resellerId, MsnProvider provider) {
        if (accId != null) {
            Optional<String> mapped = msgShortcodeSetupService
                    .findSenderIdForProvider(accId, provider);
            if (mapped.isPresent()) return mapped;
        }
        if (resellerId == null) return Optional.empty();
        return shortCodeRepository
                .findFirstByShResellerIdAndShMsnProviderOrderByShPriorityAsc(resellerId, provider)
                .map(ShortCode::getShCode);
    }

    /**
     * Correct the network of an existing sender ID, for one wrongly stamped SAFARICOM by the
     * backfill. To have the same name on a second network, register it again instead — that is no
     * longer a duplicate.
     */
    public StandardJsonResponse updateSenderIdProvider(ShortCodeProviderDto dto, User auth) {
        StandardJsonResponse response = new StandardJsonResponse();

        List<ShortCode> existing = shortCodeRepository
                .findByShCodeAndShResellerId(dto.getShCode(), auth.getUsrResellerId());
        if (existing.isEmpty()) {
            response.setMessage("message", "Failed!!!  Sender Id " + dto.getShCode() + " not found for this reseller", response);
            response.setSuccess(false);
            return response;
        }
        if (existing.size() > 1) {
            response.setMessage("message", "Failed!!!  Sender Id " + dto.getShCode()
                    + " is registered on more than one network — nothing to move", response);
            response.setSuccess(false);
            return response;
        }

        ShortCode shortCode = existing.get(0);
        shortCode.setShMsnProvider(dto.getShMsnProvider());
        shortCode.setShChannel("KENYA." + dto.getShMsnProvider().name());
        shortCodeRepository.saveAndFlush(shortCode);

        int mappings = msgShortcodeSetupService.updateMsnProviderByShCode(
                dto.getShCode(), auth.getUsrResellerId(), dto.getShMsnProvider());

        response.setData("result", shortCode, response);
        response.setMessage("message", "Sender-Id " + dto.getShCode() + " moved to " + dto.getShMsnProvider()
                + " (" + mappings + " account mapping(s) updated)", response);
        response.setTotal(1);
        return response;
    }

    public StandardJsonResponse registerSenderId(ShortCodeDto shortCodeDto, User auth) {

        StandardJsonResponse response = new StandardJsonResponse();
        MsnProvider provider = shortCodeDto.getShMsnProvider() == null ? MsnProvider.SAFARICOM : shortCodeDto.getShMsnProvider();

        // The network is part of the identity: MERIDIANBET on Safaricom and MERIDIANBET on Airtel
        // are two different sender IDs, registered separately with each carrier. Only the same name
        // on the SAME network is a duplicate.
        ShortCode shortCode = shortCodeRepository
                .findByShCodeAndShResellerIdAndShMsnProvider(shortCodeDto.getShCode(), auth.getUsrResellerId(), provider)
                .orElse(new ShortCode());

        if (!TextUtils.isEmpty(shortCode.getShCode())) {
            response.setMessage("message", "Failed!!!  Sender Id " + shortCodeDto.getShCode()
                    + " already exists on " + provider, response);
            response.setSuccess(false);
            return response;
        }
        shortCode.setShResellerId(auth.getUsrResellerId());
        shortCode.setShCreatedDate(AppTime.now());
        shortCode.setShCreatedById(auth.getUsrId());
        shortCode.setShStatus(ShStatus.PENDING_MAPPING);
        shortCode.setShCreatedById(auth.getUsrId());
        shortCode.setShMsnProvider(provider);
        shortCode.setShChannel("KENYA." + provider.name());
        shortCode.setShPriority(ShPriority.PRIMARY);
        shortCode.setShPrsp("WEISER");
        shortCode.setShCode(shortCodeDto.getShCode());
        shortCode.setShCreatedByname(auth.getEmail());
        shortCode.setShSenderType(shortCodeDto.getShSenderType() == null ? "PROMOTION" : shortCodeDto.getShSenderType());
        shortCodeRepository.saveAndFlush(shortCode);

        response.setData("result", shortCode, response);
        response.setMessage("message", "Sender-Id Saved successfully", response);
        response.setTotal(1);
        return response;
    }
}
