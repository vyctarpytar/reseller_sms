package com.spa.smart_gate_springboot.account_setup.senderId;

import com.spa.smart_gate_springboot.account_setup.shortsetup.MsgShortcodeSetupService;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.http.util.TextUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v2/shortcode")
public class ShortCodeController {

    private final ShortCodeService shortCodeService;
    private final MsgShortcodeSetupService msgShortcodeSetupService;
    private final UserService userService;

//    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PostMapping("/assign/{accId}")
    public StandardJsonResponse assignAccountToSetUpArray(@PathVariable UUID accId, @RequestBody @NotNull ShShorCode shIds, HttpServletRequest request) {
        StandardJsonResponse resp = new StandardJsonResponse();
        var auth = userService.getCurrentUser(request);

        //            delete before saving
        msgShortcodeSetupService.deleteAssignedShortCodes(accId);

        String shIds1 = shIds.getShIds();
        if (TextUtils.isEmpty(shIds1)) {
            resp.setMessage("result", "No SenderNames Assigned", resp);
            return resp;
        }
        String[] shIdArray = shIds1.split(",");

        for (String code : shIdArray) {
            shortCodeService.assignAccountToSetUp(accId, auth, code);
        }


        resp.setMessage("message", "Account Mapped Successfully", resp);
        return resp;

    }

    @PostMapping
    public StandardJsonResponse fetchAllSetups(HttpServletRequest request, @RequestBody ShFilterDto shFilterDto) {
        var auth = userService.getCurrentUser(request);
        return shortCodeService.fetchAllSetups(auth, shFilterDto);
    }

    @GetMapping("distinct-names")
    public StandardJsonResponse fetchDistinctSenderNames(HttpServletRequest request) {
        var auth = userService.getCurrentUser(request);
        return shortCodeService.fetchDistinctResellerSenderNames(auth);
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

    @PostMapping("register")
    public StandardJsonResponse registerSenderId(HttpServletRequest request, @RequestBody @Valid ShortCodeDto shortCode) {
        var auth = userService.getCurrentUser(request);
        return shortCodeService.registerSenderId(shortCode,auth);
    }

}

