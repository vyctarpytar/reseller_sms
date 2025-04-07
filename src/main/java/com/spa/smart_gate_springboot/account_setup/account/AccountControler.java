package com.spa.smart_gate_springboot.account_setup.account;


import com.spa.smart_gate_springboot.account_setup.account.dtos.AcDisableDto;
import com.spa.smart_gate_springboot.account_setup.account.dtos.AcDto;
import com.spa.smart_gate_springboot.account_setup.account.dtos.AcFilterDto;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.messaging.send_message.api.ApiKeyService;
import com.spa.smart_gate_springboot.user.Role;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v2/account")
public class AccountControler {
    private final UserService userService;
    private final AccountService accountService;
    private final AccountSPAService accountSPAService;
//    private final ApiKeyService apiKeyService;

    @GetMapping("/reseller/{resellerId}")
    public StandardJsonResponse getAccountByResellerId(@PathVariable @NotNull UUID resellerId) {
        return accountService.getAccountByResellerId(resellerId);
    }

    @GetMapping("/{accId}")
    public StandardJsonResponse getAccountByResellerId2(@PathVariable UUID accId) {
        return accountService.getAccountByAccountId(accId);

    }

    @GetMapping()
    public StandardJsonResponse getAccountByResellerId2(HttpServletRequest request) {
        var user = userService.getCurrentUser(request);
        if (user.getLayer().equals(Layers.ACCOUNT)) {
            return accountService.getAccountByAccountId(user.getUsrAccId());
        } else if (user.getRole().equals(Role.SALE)) {
            return accountService.getAccountByCreatedById(user.getUsrId());
        }
        return accountService.getAccountByResellerId(user.getUsrResellerId());
    }

    @PostMapping("filter")
    public StandardJsonResponse getAccountByResellerFilter(HttpServletRequest request, @RequestBody AcFilterDto acFilterDto) {
        var user = userService.getCurrentUser(request);
        if (user.getRole().equals(Role.SALE)) {
            return accountService.getAccountByCreatedById(user.getUsrId());
        }
        return accountService.getAccountByResellerId(user.getUsrResellerId(), acFilterDto);
    }

    @GetMapping("/balance")
    public StandardJsonResponse getBalanceDto(HttpServletRequest request) {
        var user = userService.getCurrentUser(request);
        return accountService.getBalanceDto(user.getUsrAccId());
    }

    @GetMapping("without-sender-id")
    public StandardJsonResponse getAccountsWithoutSenderId(HttpServletRequest request) {
        var user = userService.getCurrentUser(request);
        return accountService.getAccountsWithoutSenderId(user.getUsrResellerId());
    }

    @PreAuthorize("hasAnyAuthority('sale:create_customer', 'management:create','admin:create')")
    @PostMapping
    public StandardJsonResponse saveAccount(@RequestBody Account account, HttpServletRequest request) {
        var user = userService.getCurrentUser(request);
        return accountService.saveAccount(account, user);
    }

    @PreAuthorize("hasAnyRole('ACCOUNTANT','SUPER_ADMIN')")
    @PostMapping("enable/{accId}")
    public StandardJsonResponse enableAccount(@PathVariable UUID accId, HttpServletRequest request) {
        var user = userService.getCurrentUser(request);
        return accountSPAService.enableSmartRevenue(accId, user);
    }

    @PreAuthorize("hasAnyRole('ACCOUNTANT','SUPER_ADMIN')")
    @PostMapping("disable/{accId}")
    public StandardJsonResponse disableSmartRevenue(@PathVariable UUID accId, @RequestBody AcDisableDto dto, HttpServletRequest request) {
        var user = userService.getCurrentUser(request);
        return accountSPAService.disableSmartRevenue(accId, user, dto.getAccDisableDate(), dto.getAccToDiableTimer());
    }


    @GetMapping("distionct-status")
    public StandardJsonResponse getDistinctStatus() {
        StandardJsonResponse resp = new StandardJsonResponse();
        List<AcDto> acDtos = Arrays.stream(AcStatus.values()).map(a -> AcDto.builder().acName(a.name()).build()).collect(Collectors.toList());
        resp.setTotal(acDtos.size());
        resp.setData("result", acDtos, resp);
        return resp;
    }
}

