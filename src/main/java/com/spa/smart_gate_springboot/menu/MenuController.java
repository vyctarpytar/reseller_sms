package com.spa.smart_gate_springboot.menu;

import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/menu")
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    public StandardJsonResponse getAllMenusTop(HttpServletRequest request,
                                               @RequestParam(value = "reseller_id", required = false) String resellerId,
                                               @RequestParam(value = "account_id", required = false) String accountId) {
        return menuService.findMenuByLayer(request, resellerId, accountId);
    }

}
