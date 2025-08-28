package com.spa.smart_gate_springboot.ndovuPay;

import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.ndovuPay.withdraw.WithDrawDto;
import com.spa.smart_gate_springboot.ndovuPay.withdraw.WithDrawFilter;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v2/ndovupay")
@RequiredArgsConstructor
public class NdovuPayController {
    private final NdovupayService ndovupayService;
    private final UserService userService;

    @PreAuthorize("hasAnyRole('ACCOUNTANT','SUPER_ADMIN','ADMIN')")
    @GetMapping("balance")
    public StandardJsonResponse ndovuPayBalance(HttpServletRequest request) throws Exception {
        var user = userService.getCurrentUser(request);

        UUID usrResellerId ;
        if (user.getLayer().equals(Layers.RESELLER)) {
            usrResellerId = user.getUsrResellerId();
        } else if (user.getLayer().equals(Layers.TOP)) {
            usrResellerId = user.getUsrId();
        } else {
            throw new RuntimeException("Only resellers / TOP  can use withdraw operation");
        }
        return ndovupayService.getOrgBalances(usrResellerId);
    }

    @PreAuthorize("hasAnyRole('ACCOUNTANT','SUPER_ADMIN','ADMIN')")
    @GetMapping("rates")
    public StandardJsonResponse getNdovuPayTaarif()  {
        return ndovupayService.getNdovuPayRates();
    }


    @PreAuthorize("hasAnyRole('ACCOUNTANT','SUPER_ADMIN','ADMIN')")
    @PostMapping("initiate-withdraw")
    public StandardJsonResponse sendOtp(HttpServletRequest request, @RequestBody WithDrawDto withDrawDto) {
        var user = userService.getCurrentUser(request);
        return ndovupayService.initiateWithDrawOtp(user, withDrawDto);
    }

    @PreAuthorize("hasAnyRole('ACCOUNTANT','SUPER_ADMIN','ADMIN')")
    @PostMapping("finalize-withdraw")
    public StandardJsonResponse finalizewithdraw(HttpServletRequest request, @RequestBody WithDrawDto withDrawDto)  {
        var user = userService.getCurrentUser(request);
        return ndovupayService.finalizeWithdrawOtp(user, withDrawDto);
    }

    @PreAuthorize("hasAnyRole('ACCOUNTANT','SUPER_ADMIN','ADMIN')")
    @PostMapping("withdrawals")
    public StandardJsonResponse fetchWithdrawRequest(HttpServletRequest request, @RequestBody WithDrawFilter withDrawFilter,  @RequestParam(required = false) String reseller_id)  {
        if (reseller_id != null) {
            withDrawFilter.setWithDrawResellerId(UUID.fromString(reseller_id));
        }
        var user = userService.getCurrentUser(request);
        if(user.getLayer().equals(Layers.RESELLER)){
            withDrawFilter.setWithDrawResellerId(user.getUsrResellerId());
        } else if (user.getLayer().equals(Layers.TOP)) {
            withDrawFilter.setWithDrawResellerId(user.getUsrId());
        } else {
            throw new RuntimeException("Only resellers / TOP  can use withdraw operation");
        }
        return ndovupayService.fetchWithdrawRequest(user, withDrawFilter);
    }
    @GetMapping("/distinct-status")
    public StandardJsonResponse getDistinctMsgStatuses() {
        return ndovupayService.getDistinctWithDrawStatuses();
    }
}
