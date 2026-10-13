package com.spa.smart_gate_springboot.account_setup.smpp;

import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v2/smpp-connection")
public class SmppConnectionController {
    private final UserService userService;
    private final SmppConnectionService smppConnectionService;

    @GetMapping
    public StandardJsonResponse list(HttpServletRequest request,
                                     @RequestParam(required = false) String reseller_id,
                                     @RequestParam(required = false) String account_id) {
        var user = userService.getCurrentUser(request);
        return smppConnectionService.list(user, reseller_id, account_id);
    }

    /** Creates when {@code id} is absent, otherwise updates password / bind mode / name / active state. */
    @PostMapping
    public StandardJsonResponse save(HttpServletRequest request, @RequestBody SmppConnectionDto dto,
                                     @RequestParam(required = false) String reseller_id) {
        var user = userService.getCurrentUser(request);
        return smppConnectionService.save(dto, user, reseller_id);
    }
}
