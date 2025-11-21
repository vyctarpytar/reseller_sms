package com.spa.smart_gate_springboot.messaging.send_message.api;

import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v2/api-key")
@RequiredArgsConstructor
public class SeCuredApiController {

    private final ApiKeyService apiKeyService;
    private final UserService userService;


    @GetMapping()
    @PreAuthorize("hasAnyRole('ACCOUNTANT','SUPER_ADMIN','ADMIN')")
    public StandardJsonResponse getDevSandBox(HttpServletRequest request, @RequestParam String account_id) {
        var user = userService.getCurrentUser(request);
        if (!user.getLayer().equals(Layers.ACCOUNT)) {
            if (account_id == null) {
                throw new RuntimeException("Account ID is required");
            }
            user = userService.findById(UUID.fromString(account_id));

            return  apiKeyService.getAccountApiKeyInfo(user);
        }
        return  apiKeyService.getAccountApiKeyInfo(user);
    }


}
