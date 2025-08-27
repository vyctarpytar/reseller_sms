package com.spa.smart_gate_springboot.account_setup.account;


import com.spa.smart_gate_springboot.MQRes.MQConfig;
import com.spa.smart_gate_springboot.MQRes.RMQPublisher;
import com.spa.smart_gate_springboot.account_setup.account.dtos.AcDelete;
import com.spa.smart_gate_springboot.account_setup.account.dtos.AcFilterDto;
import com.spa.smart_gate_springboot.account_setup.account.dtos.AccBalanceUpdate;
import com.spa.smart_gate_springboot.account_setup.account.dtos.BalanceDto;
import com.spa.smart_gate_springboot.account_setup.reseller.ResellerService;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.errorhandling.ApplicationExceptionHandler;
import com.spa.smart_gate_springboot.messaging.send_message.api.ApiKeyService;
import com.spa.smart_gate_springboot.user.Role;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.GlobalUtils;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {


    private final AccountRepository accountRepository;
    private final UserService userService;
    //    private final SmartGate smartGate;
    @Lazy
    private final ResellerService resellerService;
    private final GlobalUtils gu;
    private final ApiKeyService apiKeyService;

    private final RMQPublisher rmqPublisher;


    public Account findByAccId(UUID id) {
        return this.accountRepository.findById(id).orElseThrow(() -> new ApplicationExceptionHandler.resourceNotFoundException("Smart Gate Account not found with Id : " + id));
    }

    public Account save(Account acc) {
        return accountRepository.saveAndFlush(acc);
    }

    public StandardJsonResponse getBalanceDto(UUID accId) {
        StandardJsonResponse resp = new StandardJsonResponse();
        Account account = findByAccId(accId);
        String resellerName = "";
        if (account.getAccResellerId() != null) {
            resellerName = resellerService.findById(account.getAccResellerId()).getRsCompanyName();
        }
        if (account.getAccStatus() == null) {
            account.setAccStatus(AcStatus.ACTIVE);
        }

        BigDecimal divide = gu.getDivide(account.getAccMsgBal(), account.getAccSmsPrice());
        BalanceDto dto = BalanceDto.builder().accBalance(account.getAccMsgBal()).accStatus(account.getAccStatus().name()).accName(account.getAccName()).accResellerName(resellerName).accRate(account.getAccSmsPrice()).accToDisableOnDate(account.getAccToDisableOnDate()).accUnits(divide.longValue()).build();

        resp.setData("result", dto, resp);
        return resp;
    }


    public StandardJsonResponse getAccountByResellerId(UUID resellerId) {
        StandardJsonResponse resp = new StandardJsonResponse();
        List<Account> accountList = findAccountByResellerId(resellerId);
        resp.setData("result", accountList, resp);
        resp.setTotal(accountList.size());
        return resp;
    }

    public StandardJsonResponse getAccountByResellerId(UUID resellerId, AcFilterDto filterDto) {
        StandardJsonResponse resp = new StandardJsonResponse();


        var accName = filterDto.getAccName();
        if (!TextUtils.isEmpty(accName)) accName = "%" + accName + "%";
        var accStatus = filterDto.getAccStatus();
        var accAccId = filterDto.getAccAccId();
        var accCreatedDate = filterDto.getAccCreatedDate();
        var accDateFrom = filterDto.getAccDateFrom();
        var accDateTo = filterDto.getAccDateTo();
        var accOfficeMobile = filterDto.getAccOfficeMobile();
        if (!TextUtils.isEmpty(accOfficeMobile)) accOfficeMobile = "%" + accOfficeMobile + "%";

        List<Account> accountList = accountRepository.filterAccounts(accName, accStatus, accAccId, accCreatedDate, accDateFrom, accDateTo, accOfficeMobile);
        resp.setData("result", accountList, resp);
        resp.setTotal(accountList.size());
        return resp;
    }

    public StandardJsonResponse getAccountByCreatedById(UUID usrId) {
        StandardJsonResponse resp = new StandardJsonResponse();
        List<Account> accountList = accountRepository.findByAccCreatedBy(usrId);
        resp.setData("result", accountList, resp);
        resp.setTotal(accountList.size());
        return resp;
    }

    public List<Account> findAccountByResellerId(UUID resellerId) {
        return accountRepository.findByAccResellerId(resellerId);
    }

    public StandardJsonResponse getAccountByAccountId(UUID usrAccId) {
        StandardJsonResponse resp = new StandardJsonResponse();
        List<Account> accountList = new ArrayList<>();
        Account account = findByAccId(usrAccId);
        accountList.add(account);
        resp.setData("result", accountList, resp);
        resp.setTotal(accountList.size());
        return resp;
    }

    public StandardJsonResponse getAccountsWithoutSenderId(UUID resellerId) {
        StandardJsonResponse resp = new StandardJsonResponse();
        List<Account> accountList = accountRepository.getAccountsWithoutSenderId(resellerId);
        resp.setData("result", accountList, resp);
        resp.setTotal(accountList.size());
        return resp;
    }



    @Transactional
    public StandardJsonResponse saveAccount(Account accountdto, User user) {
        UUID acc = accountdto.getAccId();
        Account account = new Account();
        if (acc != null) {
            account = findByAccId(acc);
        }
        BeanUtils.copyProperties(accountdto, account, gu.getNullPropertyNames(accountdto));

        StandardJsonResponse resp = new StandardJsonResponse();
        account.setAccResellerId(user.getUsrResellerId());
        account.setAccCreatedBy(user.getUsrId());
        account.setAccCreatedDate(LocalDateTime.now());
        account.setAccUsername(accountdto.getAccName());

        if (account.getAccMsgBal() == null) {
            account.setAccMsgBal(BigDecimal.valueOf(50));
        }
        Account acco = save(account);
        resp.setData("result", acco, resp);
        if (acc == null) {
            createDefaultAdmin(acco);
        }
        apiKeyService.createApiKey(acco);

        resp.setMessage("message", "Account Created Successfully", resp);
        return resp;
    }

    private void createDefaultAdmin(Account account) {
        var admin = User.builder().layer(Layers.ACCOUNT).role(Role.ADMIN).email(account.getAccAdminEmail()).firstname(account.getAccName()).phoneNumber(account.getAccAdminMobile()).createdBy(account.getAccCreatedBy()).usrResellerId(account.getAccResellerId()).usrAccId(account.getAccId()).build();
        userService.createDefaultAdminUser(admin);
    }


    public BigDecimal getAccountBalancesForReseller(UUID rsId) {
        return accountRepository.getAccountBalancesForReseller(rsId);
    }

    public void handleUpdateOfAccountBalance(BigDecimal msgCostId, UUID accId, UUID accResellerId) {
        AccBalanceUpdate accBalanceUpdate = AccBalanceUpdate.builder().accId(accId).accResellerId(accResellerId).msgCost(msgCostId).build();

        try {
            rmqPublisher.publishToOutQueue(accBalanceUpdate, MQConfig.UPDATE_ACCOUNT_BALANCE);
        } catch (Exception e) {
            log.error("Error queueing update_balance");
        }

    }

    public void refundCostCharged(UUID msgAccId, BigDecimal msgCostId) {
        accountRepository.refundCostCharged(msgAccId,msgCostId);
    }

    public StandardJsonResponse deleteAccount(UUID accId, User user, AcDelete acDelete) {
        Account account = findByAccId(accId);
        account.setAccStatus(AcStatus.DELETED);
        account.setAccDeletedBy(user.getEmail());
        account.setAccDeletedDate(LocalDateTime.now());
        account.setAccDeletedReason(acDelete.getAcDeleteReason());
        save(account);

        // delete users

//        User

        StandardJsonResponse resp = new StandardJsonResponse();
        resp.setMessage("message", "Account Data will be deleted after 30 days", resp);
        return resp;
    }
}
