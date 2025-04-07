package com.spa.smart_gate_springboot.account_setup.account;

import com.spa.smart_gate_springboot.account_setup.account.audit.AccountAudit;
import com.spa.smart_gate_springboot.account_setup.account.audit.AccountAuditRepository;
import com.spa.smart_gate_springboot.account_setup.account.dtos.BalanceDto;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
@EnableScheduling
@Slf4j
public class AccountSPAService {
    private final AccountRepository accountRepository;
    private final RestTemplate restTemplate;
    private final AccountAuditRepository auditRepository;


    // Schedule the method to run every hour 3600000
//    @Scheduled(fixedRate = 3600000)
//    public void disableSmartRevenue() {
//        Calendar calendar = Calendar.getInstance();
//        calendar.add(Calendar.DAY_OF_MONTH, -1);
//        Date dateOneDayBefore = calendar.getTime();
//        List<Account> accountList = accountRepository.findByAccToDisableOnDateAfterAndAccDeliveryUrlNotNull(dateOneDayBefore);
//        MathContext mathContext = new MathContext(2, RoundingMode.HALF_UP);
//        accountList.stream().map(account -> {
//            String accDeliveryUrl = account.getAccDeliveryUrl() + "/spamng/disable-account.action";
//
//            BalanceDto balanceDto = BalanceDto.builder().accBalance(account.getAccMsgBal()).accStatus(account.getAccStatus().name()).accName(account.getAccName()).accToDisableOnDate(account.getAccToDisableOnDate()).accRate(account.getAccSmsPrice()).accUnits(account.getAccMsgBal().divide(account.getAccSmsPrice(), mathContext)).build();
//            restTemplate.postForObject(accDeliveryUrl, balanceDto, Object.class);
//            return balanceDto;
//        });
////         .collect(Collectors.toList());
//    }

    public StandardJsonResponse enableSmartRevenue(UUID accId, User user) {
        StandardJsonResponse resp = new StandardJsonResponse();
        Account account = accountRepository.findById(accId).orElse(null);
        if (account == null || TextUtils.isEmpty(account.getAccDeliveryUrl())) {
            resp.setMessage("message", account.getAccName() + " failed to enable - No External end-point", resp);
            resp.setSuccess(false);
            resp.setStatus(500);
            return resp;
        }
        String accDeliveryUrl = account.getAccDeliveryUrl() + "/spamng/enable-account.action";
        try {
            MathContext mathContext = new MathContext(18, RoundingMode.HALF_UP);
            BigDecimal units = account.getAccMsgBal().divide(account.getAccSmsPrice(), mathContext);
            BalanceDto balanceDto = BalanceDto.builder().accBalance(account.getAccMsgBal())
                    .accStatus(account.getAccStatus().name()).accName(account.getAccName()).accToDisableOnDate(null)
                    .accRate(account.getAccSmsPrice()).accUnits(units.longValue()).build();

            restTemplate.postForObject(accDeliveryUrl, balanceDto, Object.class);
        } catch (Exception e) {
            resp.setMessage("message", account.getAccName() + " failed to enable ", resp);
            resp.setSuccess(false);
            resp.setStatus(500);
            log.error(e.getMessage());
            return resp;
        }
        account.setAccToDisableOnDate(null);
        account.setAccToDiableTimer(null);
        account.setAccStatus(AcStatus.ACTIVE);
        accountRepository.saveAndFlush(account);
        AccountAudit audit = AccountAudit.builder().accId(account.getAccId()).accActionBy(user.getUsrId()).accActionByName(user.getEmail()).accActionDate(LocalDateTime.now()).accAction("ENABLE_ACCOUNT").build();
        auditRepository.saveAndFlush(audit);
        resp.setMessage("message", account.getAccName() + " enabled Successfully ", resp);
        resp.setData("result", account, resp);
        return resp;
    }

    public StandardJsonResponse disableSmartRevenue(UUID accId, User user, Date disableDate, int accToDiableTimer) {
        StandardJsonResponse resp = new StandardJsonResponse();
        Account account = accountRepository.findById(accId).orElse(null);
        if (account == null || TextUtils.isEmpty(account.getAccDeliveryUrl())) {
            return resp;
        }
        account.setAccToDisableOnDate(disableDate);
        account.setAccToDiableTimer(accToDiableTimer);

        account.setAccStatus(AcStatus.DISABLED_BY_ACCOUNTS);
        accountRepository.saveAndFlush(account);

        String accDeliveryUrl = account.getAccDeliveryUrl() + "/spamng/disable-account.action";
        MathContext mathContext = new MathContext(18, RoundingMode.HALF_UP);
        BalanceDto balanceDto = BalanceDto.builder().accBalance(account.getAccMsgBal()).accStatus(account.getAccStatus().name()).accName(account.getAccName()).accToDisableOnDate(account.getAccToDisableOnDate()).accRate(account.getAccSmsPrice())
                .accUnits(account.getAccMsgBal().divide(account.getAccSmsPrice(), mathContext).longValue()).build();
        restTemplate.postForObject(accDeliveryUrl, balanceDto, Object.class);

        AccountAudit audit = AccountAudit.builder().accId(account.getAccId()).accActionBy(user.getUsrId()).accActionByName(user.getEmail()).accActionDate(LocalDateTime.now()).accAction("DISABLE_ACCOUNT").build();
        auditRepository.saveAndFlush(audit);

        return resp;
    }


}
