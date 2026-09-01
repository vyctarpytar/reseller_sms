package com.spa.smart_gate_springboot.account_setup.shortsetup;


import com.spa.smart_gate_springboot.account_setup.account.AcStatus;
import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.account_setup.request.ReStatus;
import com.spa.smart_gate_springboot.account_setup.request.RequestEntity;
import com.spa.smart_gate_springboot.account_setup.request.RequestService;
import com.spa.smart_gate_springboot.account_setup.senderId.MsnProvider;
import com.spa.smart_gate_springboot.account_setup.senderId.SenderNameDto;
import com.spa.smart_gate_springboot.account_setup.senderId.ShortCode;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.errorhandling.ApplicationExceptionHandler;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.utils.GlobalUtils;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MsgShortcodeSetupService {
    private final MsgShortcodeSetupRepository msgShortcodeSetupRepository;
    private final RequestService requestService;
    private final AccountService accountService;
    private final GlobalUtils globalUtils;

    public MsgShortcodeSetup findByShId(UUID id) {
        return this.msgShortcodeSetupRepository.findByShId(id).orElseThrow(() -> new ApplicationExceptionHandler.resourceNotFoundException("Short COde Setup not found with Id : " + id));
    }

    /**
     * The sender ID this account is mapped to on one network — the send path's first choice, so an
     * account can carry its own Airtel sender ID independent of its reseller's.
     */
    public Optional<String> findSenderIdForProvider(UUID accId, MsnProvider provider) {
        return this.msgShortcodeSetupRepository
                .findFirstByShAccIdAndShMsnProviderOrderByShPriorityAsc(accId, provider)
                .map(MsgShortcodeSetup::getShCode);
    }

    /** Re-point this reseller's mappings of one sender ID at a different network. */
    public int updateMsnProviderByShCode(String shCode, UUID resellerId, MsnProvider provider) {
        return this.msgShortcodeSetupRepository.updateMsnProviderByShCode(shCode, resellerId, provider);
    }

    /** @see com.spa.smart_gate_springboot.account_setup.senderId.ShortCodeService#backfillMsnProvider() */
    public int backfillMsnProvider() {
        return this.msgShortcodeSetupRepository.backfillMsnProvider();
    }

    /**
     * The setup row the Safaricom senders read the sender type / package from. A name mapped on more
     * than one network yields several rows, so prefer the Safaricom one — these callers are the
     * SDP/Daraja path, and picking e.g. the Airtel row would read the wrong package.
     */
    public MsgShortcodeSetup findByShCodeAndShAccId(String shCode, UUID shAccId) {
        List<MsgShortcodeSetup> setups = this.msgShortcodeSetupRepository.findByShCodeAndShAccId(shCode, shAccId);
        return setups.stream()
                .filter(s -> s.getShMsnProvider() == null || s.getShMsnProvider() == MsnProvider.SAFARICOM)
                .findFirst()
                .or(() -> setups.stream().findFirst())
                .orElseThrow(() -> new ApplicationExceptionHandler.resourceNotFoundException("Short COde Setup not found with code : " + shCode));
    }

    public StandardJsonResponse assignSenderId(UUID reqId, User auth, MsgShortcodeSetup setup) {
        StandardJsonResponse resp = new StandardJsonResponse();

        RequestEntity requestEntity = requestService.findByid(reqId);
        setup.setShResellerId(requestEntity.getReResellerId());
        requestEntity.setReStatus(ReStatus.PROCESSED);
        setup.setShStatus(ShStatus.PENDING_MAPPING);
        msgShortcodeSetupRepository.saveAndFlush(setup);
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
        if (user.getLayer().equals(Layers.ACCOUNT)) {
            filterDto.setShResellerId(null);
            filterDto.setShAccId(user.getUsrAccId());
        }
        globalUtils.printToJson(filterDto, "error");

        Page<MsgShortcodeSetup> pagedData = msgShortcodeSetupRepository.findAllShortCodes(filterDto.getShResellerId(), filterDto.getShAccId(), filterDto.getShSenderId(), filterDto.getShStatus(), pageable);

        resp.setData("result", pagedData.getContent(), resp);
        resp.setMessage("message", "All setups found", resp);
        resp.setTotal((int) pagedData.getTotalElements());
        return resp;
    }

    public StandardJsonResponse assignAccountToSetUp(UUID shId, User auth, UUID accId) {
        StandardJsonResponse resp = new StandardJsonResponse();

        MsgShortcodeSetup setup = findByShId(shId);
        setup.setShAccId(accId);
        setup.setShMappedById(auth.getUsrId());
        setup.setShStatus(ShStatus.ACTIVE);
        msgShortcodeSetupRepository.saveAndFlush(setup);

        RequestEntity requestEntity = requestService.findByReSetUpId(shId);
        if (requestEntity != null && requestEntity.getReSetUpId() != null) {
            requestEntity.setReStatus(ReStatus.ACTIVE);
            requestService.save(requestEntity);
        }

        Account account = accountService.findByAccId(accId);
        account.setAccStatus(AcStatus.ACTIVE);
        //todo load
        //todo notify the admin to start sending sms.


        resp.setData("result", setup, resp);
        resp.setMessage("message", "Account Mapped Successfully", resp);
        return resp;
    }

    public StandardJsonResponse fetchDistinctResellerSenderNames(User user) {
        StandardJsonResponse response = new StandardJsonResponse();
        List<Object[]> rows = new ArrayList<>();

        if (user.getLayer().equals(Layers.ACCOUNT)) {
            rows = msgShortcodeSetupRepository.findDistinctSenderNames(null, user.getUsrAccId());
        } else if (user.getLayer().equals(Layers.TOP)) {
            rows = msgShortcodeSetupRepository.findDistinctSenderNames(null, null);
        } else if (user.getLayer().equals(Layers.RESELLER)) {
            rows = msgShortcodeSetupRepository.findDistinctSenderNames(user.getUsrResellerId(), null);
        } else {

        }
        List<SenderNameDto> msgQueues = toSenderNames(rows);
        response.setData("result", msgQueues, response);
        response.setTotal(msgQueues.size());
        return response;
    }

    /**
     * {@code [sh_code, sh_msn_provider]} rows to the shape the sender-name pickers read. A null
     * network only survives if the boot backfill could not run, so it is reported as SAFARICOM —
     * the value every sender ID predating the column carries.
     */
    public static List<SenderNameDto> toSenderNames(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new SenderNameDto(
                        (String) row[0],
                        row[1] == null ? MsnProvider.SAFARICOM.name() : (String) row[1]))
                .toList();
    }



    public void assignShortCodeToAccount(User auth, UUID accId, ShortCode shortCode) {
        try {
            MsgShortcodeSetup setup = new MsgShortcodeSetup();
            BeanUtils.copyProperties(shortCode, setup, globalUtils.getNullPropertyNames(shortCode));
            setup.setShId(null);
            setup.setShAccId(accId);
            setup.setShMappedById(auth.getUsrId());
            setup.setShStatus(ShStatus.ACTIVE);
            msgShortcodeSetupRepository.saveAndFlush(setup);
        } catch (Exception e) {
            log.error(e.getMessage());
        }

    }

    public void deleteAssignedShortCodes(UUID accId) {
        List<MsgShortcodeSetup> setupList = msgShortcodeSetupRepository.findByShAccId(accId);
        if (!setupList.isEmpty()) {
            msgShortcodeSetupRepository.deleteAll(setupList);
        }
    }
}
