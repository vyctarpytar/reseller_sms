package com.spa.smart_gate_springboot.account_setup.shortsetup;

import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.http.util.TextUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v2/setup")
public class MsgShortCodeSetUpController {

    private final MsgShortcodeSetupService msgShortcodeSetupService;
    private final UserService userService;

    @PostMapping("/initiate/{reqId}")
    public StandardJsonResponse assignSenderId(@PathVariable UUID reqId, @RequestBody MsgShortcodeSetup setup, HttpServletRequest request) {
        var auth = userService.getCurrentUser(request);
        return msgShortcodeSetupService.assignSenderId(reqId, auth, setup);

    }

    @PostMapping("/assign-account/{shId}")
    public StandardJsonResponse assignAccountToSetUp(@PathVariable UUID shId, @RequestParam @NotNull UUID accId, HttpServletRequest request) {
        var auth = userService.getCurrentUser(request);
        return msgShortcodeSetupService.assignAccountToSetUp(shId, auth, accId);

    }

    @PostMapping("/assign-accounts/{shId}")
    public StandardJsonResponse assignAccountToSetUpArray(@PathVariable UUID shId, @RequestBody @NotNull ShAccounts accIds, HttpServletRequest request) {
        StandardJsonResponse resp = new StandardJsonResponse();
        var auth = userService.getCurrentUser(request);
        String accIds1 = accIds.getAccIds();
        if (TextUtils.isEmpty(accIds1)) {
            resp.setStatus(400);
            resp.setSuccess(false);
            resp.setMessage("result", "No Accounts Selected", resp);
            return resp;
        }
        String[] accIdArray = accIds1.split(",");

        for (String s : accIdArray) {
            msgShortcodeSetupService.assignAccountToSetUp(shId, auth, UUID.fromString(s));
        }

        resp.setMessage("message", "Account Mapped Successfully", resp);
        return resp;

    }

    @PostMapping
    public StandardJsonResponse fetchAllSetups(HttpServletRequest request, @RequestBody ShFilterDto shFilterDto) {
        var auth = userService.getCurrentUser(request);
        return msgShortcodeSetupService.fetchAllSetups(auth, shFilterDto);
    }

    @GetMapping("distinct-sender-names")
    public StandardJsonResponse fetchDistinctSenderNames(HttpServletRequest request) {
        var auth = userService.getCurrentUser(request);
        return msgShortcodeSetupService.fetchDistinctResellerSenderNames(auth);
    }

    @GetMapping("distinct-status")
    public StandardJsonResponse fetchDistinctSenderStatus() {
        StandardJsonResponse resp = new StandardJsonResponse();
        List<String> statusList = Arrays.stream(ShStatus.values()).map(Enum::name).toList();
        resp.setTotal(statusList.size());
        resp.setData("result", statusList, resp);
        return resp;
    }

    @GetMapping("distinct-priority")
    public StandardJsonResponse fetchDistinctSenderPriority() {
        StandardJsonResponse resp = new StandardJsonResponse();
        List<String> statusList = Arrays.stream(ShPriority.values()).map(Enum::name).toList();
        resp.setTotal(statusList.size());
        resp.setData("result", statusList, resp);
        return resp;
    }

}

