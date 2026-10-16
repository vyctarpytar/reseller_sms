package com.spa.smart_gate_springboot.account_setup.lowbalance;

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

/** The caller's own low-balance alert level — see {@link LowBalanceAlertSettingsService}. */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v2/low-balance-alert")
public class LowBalanceAlertSettingsController {
    private final UserService userService;
    private final LowBalanceAlertSettingsService service;

    @GetMapping
    public StandardJsonResponse get(HttpServletRequest request,
                                    @RequestParam(required = false) String reseller_id,
                                    @RequestParam(required = false) String account_id) {
        return service.get(userService.getCurrentUser(request), reseller_id, account_id);
    }

    /** Body {@code {"level": 250}}; {@code null} resets to the default, {@code 0} turns alerts off. */
    @PostMapping
    public StandardJsonResponse save(HttpServletRequest request, @RequestBody LevelDto body,
                                     @RequestParam(required = false) String reseller_id,
                                     @RequestParam(required = false) String account_id) {
        return service.save(userService.getCurrentUser(request), reseller_id, account_id, body.level());
    }

    public record LevelDto(Long level) {
    }
}
