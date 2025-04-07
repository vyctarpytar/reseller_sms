package com.spa.smart_gate_springboot.messaging.send_message.api;

import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/api-key")
@RequiredArgsConstructor
public class SeCuredApiController {

    private final ApiKeyService apiKeyService;
    private final UserService userService;


    @GetMapping()
    @PreAuthorize("hasAnyRole('ACCOUNTANT','SUPER_ADMIN','ADMIN')")
    public StandardJsonResponse getDevSandBox(HttpServletRequest request) {
        var user = userService.getCurrentUser(request);
        return  apiKeyService.getAccountApiKeyInfo(user);
    }


}
