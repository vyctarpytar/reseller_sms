package com.spa.smart_gate_springboot.messaging.send_message.api;

import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v2/api-key")
@RequiredArgsConstructor
@Slf4j
public class SeCuredApiController {

    private final ApiKeyService apiKeyService;
    private final UserService userService;
    private final AccountService accountService;


    @GetMapping()
    @PreAuthorize("hasAnyRole('ACCOUNTANT','SUPER_ADMIN','ADMIN')")
    public StandardJsonResponse getDevSandBox(HttpServletRequest request, @RequestParam(required = false) String account_id) {
        var user = userService.getCurrentUser(request);
        log.info("User Layer: {}", user.getLayer());
        if (user.getLayer().equals(Layers.ACCOUNT)) {
            return apiKeyService.getAccountApiKeyInfo(user);
        } else {
            if (TextUtils.isEmpty(account_id)) {
                throw new RuntimeException("Account ID is required");
            }
            Account acc = accountService.findByAccId(UUID.fromString(account_id));

            return apiKeyService.getAccountApiKeyInfo(acc);
        }
    }

}
