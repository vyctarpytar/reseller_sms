package com.spa.smart_gate_springboot.account_setup.senderId;


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
import lombok.RequiredArgsConstructor;
import org.apache.http.util.TextUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShortCodeService {
    private final ShortCodeRepository shortCodeRepository;
    private final MsgShortcodeSetupService msgShortcodeSetupService;
    private final RequestService requestService;
    private final AccountService accountService;
    private final GlobalUtils globalUtils;


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
        ShortCode shortCode = shortCodeRepository.findByShCode(shCode).orElseThrow(() -> new IllegalArgumentException("No short code found for shCode: " + shCode));
        msgShortcodeSetupService.assignShortCodeToAccount(auth, accId, shortCode);


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


    public StandardJsonResponse registerSenderId(ShortCodeDto shortCodeDto, User auth) {

        ShortCode shortCode = shortCodeRepository.findByShCode(shortCodeDto.getShCode()).orElse(new ShortCode());

        StandardJsonResponse response = new StandardJsonResponse();
        if (!TextUtils.isEmpty(shortCode.getShCode())) {
            response.setMessage("message", "Failed!!!  Sender Id " + shortCodeDto.getShCode() + " already exists ", response);
            response.setSuccess(false);
            return response;
        }
        shortCode.setShResellerId(auth.getUsrResellerId());
        shortCode.setShCreatedDate(LocalDateTime.now());
        shortCode.setShCreatedById(auth.getUsrId());
        shortCode.setShStatus(ShStatus.PENDING_MAPPING);
        shortCode.setShCreatedById(auth.getUsrId());
        shortCode.setShChannel("KENYA.SAFARICOM");
        shortCode.setShPriority(ShPriority.PRIMARY);
        shortCode.setShPrsp("WEISER");
        shortCode.setShCode(shortCodeDto.getShCode());
        shortCode.setShCreatedByname(auth.getEmail());
        shortCodeRepository.saveAndFlush(shortCode);

        response.setData("result", shortCode, response);
        response.setMessage("message", "Sender-Id Saved successfully", response);
        response.setTotal(1);
        return response;
    }
}
