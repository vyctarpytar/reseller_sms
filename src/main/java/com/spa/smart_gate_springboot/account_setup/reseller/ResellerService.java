package com.spa.smart_gate_springboot.account_setup.reseller;

import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountRepository;
import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.account_setup.account.dtos.AcDelete;
import com.spa.smart_gate_springboot.account_setup.account.dtos.BalanceDto;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.messaging.send_message.api.ApiKeyService;
import com.spa.smart_gate_springboot.ndovuPay.NdovupayService;
import com.spa.smart_gate_springboot.user.Permission;
import com.spa.smart_gate_springboot.user.Role;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResellerService {
    private final ResellerRepo resellerRepo;
    private final UserService userService;
    private final NdovupayService ndovupayService;
    private final ApiKeyService apiKeyService;
    private final AccountService accountService;

    public StandardJsonResponse saveReseller(Reseller reseller) {
        StandardJsonResponse resp = new StandardJsonResponse();
        UUID rdId = reseller.getRsId();
        Reseller reseller1 = save(reseller);
        if (rdId == null) {
            createDefaultAccount(reseller1);
            createDefaultAdmin(reseller1);
            ndovupayService.createNdovuPayOrganisation(reseller1);
        }
        resp.setData("result", reseller1, resp);
        resp.setMessage("message", "ok", resp);
        return resp;
    }

    public Reseller save(Reseller reseller) {
        return resellerRepo.saveAndFlush(reseller);
    }

    private void createDefaultAdmin(Reseller reseller1) {
        var admin = User.builder().layer(Layers.RESELLER).role(Role.ADMIN).permissions(Collections.singleton(Permission.ADMIN_CREATE)).email(reseller1.getRsEmail()).firstname(reseller1.getRsContactPerson()).phoneNumber(reseller1.getRsPhoneNumber()).createdBy(reseller1.getRsCreatedBy()).usrResellerId(reseller1.getRsId()).build();
        userService.createDefaultAdminUser(admin);
    }

    private void createDefaultAccount(Reseller reseller1) {
        Account acc = Account.builder().accResellerId(reseller1.getRsId()).accMsgBal(BigDecimal.TEN)
                .accCreatedBy(reseller1.getRsCreatedBy()).accCreatedDate(LocalDateTime.now()).accSmsPrice(reseller1.getRsSmsUnitPrice())
                .accCity(reseller1.getRaCity()).accCountry(reseller1.getRaCountry()).accOfficeMobile(reseller1.getRsPhoneNumber())
                .accName(reseller1.getRsCompanyName()).accAdminEmail(reseller1.getRsEmail()).accAdminMobile(reseller1.getRsPhoneNumber()).build();
        accountService.save(acc);


    }

    public StandardJsonResponse getResellerById(UUID rsId, String name) {
        StandardJsonResponse resp = new StandardJsonResponse();
        if (name.equalsIgnoreCase(Layers.TOP.name()) && rsId != null) {
            resp.setData("result", findById(rsId), resp);
        }
        resp.setMessage("message", "ok", resp);
        return resp;
    }

    public StandardJsonResponse getResellersBalance(UUID rsId) {
        StandardJsonResponse resp = new StandardJsonResponse();
        Reseller reseller = findById(rsId);
//        BigDecimal rsMsgBal = reseller.getRsMsgBal() == null ? new BigDecimal("-1") : reseller.getRsMsgBal();
//        BigDecimal allocatedBalance = accountRepository.getAccountBalancesForReseller(reseller.getRsId());
//        BigDecimal rsAllocatableMsgBal = reseller.getRsMsgBal().subtract(allocatedBalance);
//        long accUnits = globalUtils.getDivide(rsMsgBal, reseller.getRsSmsUnitPrice()).longValue();
        BalanceDto dto = BalanceDto.builder()
//                .accBalance(rsMsgBal)
                .rsAllocatableMsgBal(reseller.getRsAllocatableUnit()).accStatus(reseller.getRsStatus())
                .accName(reseller.getRsCompanyName()).accResellerName(reseller.getRsCompanyName()).accRate(reseller.getRsSmsUnitPrice())
//                .accUnits(accUnits)
                .build();
        resp.setData("result", dto, resp);
        resp.setMessage("message", "ok", resp);
        return resp;
    }


    public Reseller findById(UUID rsId) {
        return resellerRepo.findById(rsId).orElseThrow(() -> new NotFoundException("reseller not found with id: " + rsId));
    }


    public StandardJsonResponse getAllReseller(User user) {
        StandardJsonResponse resp = new StandardJsonResponse();
        if (user.getLayer().equals(Layers.TOP)) {
            List<Reseller> list = resellerRepo.findAll();
            resp.setData("result", list, resp);
        }

        resp.setMessage("message", "ok", resp);
        return resp;
    }



//        @PostConstruct
    private void createApiKeys() {
        List<Reseller> allResellers = resellerRepo.findAll();
        for (Reseller res : allResellers) {
            List<Account> allAccounts = accountService.findByAccResellerId(res.getRsId());
            for (Account acc : allAccounts) {
                try {
                    apiKeyService.createApiKey(acc);
                }catch (Exception e) {
                    log.error("createApiKey", e);
                }
            }

        }
    }


    public StandardJsonResponse getTopLevelBalance() {
        StandardJsonResponse resp = new StandardJsonResponse();
        BalanceDto dto = BalanceDto.builder().accBalance(BigDecimal.ZERO).rsAllocatableMsgBal(BigDecimal.ZERO).accStatus("ACTIVE").accName("SYNQAFRICA").accResellerName("SYNQAFRICA").accRate(BigDecimal.ZERO).accUnits(0L).build();
        resp.setData("result", dto, resp);
        resp.setMessage("message", "ok", resp);
        return resp;
    }


    public StandardJsonResponse deleteReseller(UUID rsId, User user, AcDelete acDelete) {
        List<Account> allAccounts = accountService.findByAccResellerId(rsId);
        
        // Process account deletions in parallel using CompletableFuture
        List<CompletableFuture<Void>> deletionFutures = allAccounts.stream()
                .map(acc -> CompletableFuture.runAsync(() -> {
                    try {
                        accountService.deleteAccount(acc.getAccId(), user, acDelete);
                    } catch (Exception e) {
                        log.error("Error deleting account with ID: {}", acc.getAccId(), e);
                        throw new RuntimeException("Failed to delete account: " + acc.getAccId(), e);
                    }
                }))
                .toList();
        
        // Wait for all deletions to complete
        CompletableFuture<Void> allDeletions = CompletableFuture.allOf(
                deletionFutures.toArray(new CompletableFuture[0])
        );
        
        try {
            allDeletions.join(); // Wait for all parallel operations to complete
        } catch (Exception e) {
            log.error("Error during parallel account deletion for reseller: {}", rsId, e);
            throw new RuntimeException("Failed to delete some accounts for reseller: " + rsId, e);
        }
        
        StandardJsonResponse resp = new StandardJsonResponse();
        resp.setMessage("Reseller and Account Data will be deleted after 30 days", "ok", resp);
        return resp;
    }
}
