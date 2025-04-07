package com.spa.smart_gate_springboot.account_setup.shortsetup;


import com.spa.smart_gate_springboot.account_setup.account.AcStatus;
import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.account_setup.request.ReStatus;
import com.spa.smart_gate_springboot.account_setup.request.RequestEntity;
import com.spa.smart_gate_springboot.account_setup.request.RequestService;
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

    public MsgShortcodeSetup findByShCodeAndShAccId(String shCode, UUID shAccId) {
        return this.msgShortcodeSetupRepository.findByShCodeAndShAccId(shCode, shAccId).orElseThrow(() -> new ApplicationExceptionHandler.resourceNotFoundException("Short COde Setup not found with code : " + shCode));
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
        List<String> msgQueues = new ArrayList<>();

        if (user.getLayer().equals(Layers.ACCOUNT)) {
            msgQueues = msgShortcodeSetupRepository.findDistinctSenderNames(null, user.getUsrAccId());
        } else if (user.getLayer().equals(Layers.TOP)) {
            msgQueues = msgShortcodeSetupRepository.findDistinctSenderNames(null, null);
        } else if (user.getLayer().equals(Layers.RESELLER)) {
            msgQueues = msgShortcodeSetupRepository.findDistinctSenderNames(user.getUsrResellerId(), null);
        } else {

        }
        response.setData("result", msgQueues, response);
        response.setTotal(msgQueues.size());
        return response;
    }


//    @PostConstruct
    public void cleanInvalidStatus() {
        List<MsgShortcodeSetup> list = msgShortcodeSetupRepository.findByShStatusIsNull();
        for (MsgShortcodeSetup setup : list) {
            setup.setShStatus(ShStatus.PENDING_MAPPING);
            msgShortcodeSetupRepository.saveAndFlush(setup);
        }
    }

//    @PostConstruct
    public void cleanInvalidReselledIds() {
        List<MsgShortcodeSetup> list = msgShortcodeSetupRepository.findByShResellerIdIsNull();
        for (MsgShortcodeSetup setup : list) {
            setup.setShResellerId(UUID.fromString("d9221e5a-f7d2-4be9-b29f-0fa85dbe30e4"));
            msgShortcodeSetupRepository.saveAndFlush(setup);
        }
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
