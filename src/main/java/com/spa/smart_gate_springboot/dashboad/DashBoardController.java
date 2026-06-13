package com.spa.smart_gate_springboot.dashboad;

import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.messaging.send_message.dtos.FilterDto;
import com.spa.smart_gate_springboot.user.Role;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v2/dash")
@RequiredArgsConstructor
public class DashBoardController {
    private final DashBoardService dashBoardService;
    private final DashOverviewService dashOverviewService;
    private final UserService userService;
    private final AccountService accountService;

    @PostMapping
    public StandardJsonResponse getMainDashBoard(HttpServletRequest request, @RequestBody FilterDto filterDto, @RequestParam(required = false) String reseller_id, @RequestParam(required = false) String account_id) {
        User user = userService.getCurrentUser(request);
        if (user.getLayer().equals(Layers.ACCOUNT)) {
            filterDto.setMsgAccId(user.getUsrAccId());
        } else if (user.getRole().equals(Role.SALE)) {
            // show dashboard for sales user
            filterDto.setMsgSaleUserId(user.getUsrId());
        } else if (user.getLayer().equals(Layers.RESELLER)) {
            filterDto.setMsgResellerId(user.getUsrResellerId());
        }

        if (user.getLayer().equals(Layers.TOP)) {
            if (reseller_id != null) {
                filterDto.setMsgResellerId(UUID.fromString(reseller_id));
            }
        }
        // Drill-down: scope dashboard stats to the single account being viewed.
        UUID accScope = accountService.resolveAccountScope(user, account_id);
        if (accScope != null) {
            filterDto.setMsgAccId(accScope);
        }
        return dashBoardService.getMainDashboard(filterDto);
    }

    /**
     * Role-scoped overview cards (reseller/account census, sender IDs, wallet balance, units in
     * circulation). TOP sees the platform totals; a reseller sees only its own figures. TOP may pass
     * {@code reseller_id} to drill into a single reseller (then the platform-only cards are dropped).
     */
    @GetMapping("overview")
    public StandardJsonResponse overview(HttpServletRequest request, @RequestParam(required = false) String reseller_id) {
        User user = userService.getCurrentUser(request);
        UUID scopeReseller;
        boolean includePlatform;
        switch (user.getLayer()) {
            case TOP -> {
                includePlatform = reseller_id == null;
                scopeReseller = reseller_id != null ? UUID.fromString(reseller_id) : null;
            }
            case RESELLER -> {
                includePlatform = false;
                scopeReseller = user.getUsrResellerId();
            }
            default -> {
                StandardJsonResponse forbidden = new StandardJsonResponse();
                forbidden.setSuccess(false);
                forbidden.setStatus(HttpStatus.FORBIDDEN.value());
                forbidden.setMessage("message", "Overview is available to TOP and reseller users only", forbidden);
                return forbidden;
            }
        }
        StandardJsonResponse response = new StandardJsonResponse();
        response.setData("result", dashOverviewService.buildOverview(scopeReseller, includePlatform), response);
        return response;
    }
}
