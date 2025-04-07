package com.spa.smart_gate_springboot.account_setup.invoice;

import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/invoice")
public class InvoiceController {
    private final InvoiceService invoiceService;
    private final UserService userService;

    @PreAuthorize("hasAnyRole('ACCOUNTANT','SUPER_ADMIN')")
    @PostMapping("mark-as-paid/{invoId}")
    public StandardJsonResponse markInvoiceAsPaid(@PathVariable UUID invoId, @RequestBody InvoPaidDto invoPaidDto, HttpServletRequest request) {
        var user = userService.getCurrentUser(request);
        return invoiceService.markAsPaidCredit(invoId, user, invoPaidDto);
    }

    @GetMapping("/distinct-statuses")
    public StandardJsonResponse getDistinctInvoiceStatuses() {
        StandardJsonResponse resp = new StandardJsonResponse();
        List<String> invoStatus = Arrays.stream(InvoStatus.values()).map(Enum::name).collect(Collectors.toList());
        resp.setTotal(invoStatus.size());
        resp.setData("result", invoStatus, resp);
        return resp;

    }

    @PreAuthorize("hasAnyRole('ACCOUNTANT','SUPER_ADMIN', 'ADMIN')")
    @GetMapping("reseller-summary")
    public StandardJsonResponse getAllInvoices(HttpServletRequest request) {
        var user = userService.getCurrentUser(request);
        if (user.getLayer().equals(Layers.ACCOUNT))
            throw new RuntimeException("User is not a reseller / Top / Wallet Not Mapped");


        UUID usrResellerId = null;
        if (user.getLayer().equals(Layers.RESELLER)) {
            usrResellerId = user.getUsrResellerId();
        } else if (user.getLayer().equals(Layers.TOP)) {
            usrResellerId = user.getUsrId();
        }

        if (usrResellerId == null) throw new RuntimeException("User is not a reseller / Top / Wallet Not Mapped");

        return invoiceService.getResellerInvoicesPerYearSummary(usrResellerId);
    }


}
