package com.spa.smart_gate_springboot.account_setup.credit;

import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.account_setup.invoice.CreditInvoDto;
import com.spa.smart_gate_springboot.account_setup.invoice.InvoiceFilter;
import com.spa.smart_gate_springboot.account_setup.invoice.InvoiceService;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.http.util.TextUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v2/credit")
public class CreditController {
    private final CreditService creditService;
    private final UserService userService;
    private final InvoiceService invoiceService;
    private final AccountService accountService;

    //logged in as top level
    @PostMapping("reseller")
    public StandardJsonResponse getCreditHistoryAsReseller(HttpServletRequest request, @RequestBody CreditFilter creditFilter,@RequestParam(required = false) String reseller_id) {

        if (reseller_id != null) {
            creditFilter.setResellerId(UUID.fromString(reseller_id));
        }

        var user = userService.getCurrentUser(request);
        if (user.getLayer().equals(Layers.TOP)) {
            return creditService.getCreditLoadedToResellers(user, creditFilter);
//            return creditService.getAllResellerCreditHistory(user, creditFilter);
        } else {
            return creditService.getResellerCreditHistory(user.getUsrResellerId());
        }
    }

    @GetMapping("reseller/{resellerId}")
    public StandardJsonResponse getCreditHistoryAsReseller(@PathVariable @NotNull(message = "Field missing") UUID resellerId) {
        return creditService.getResellerCreditHistory(resellerId);
    }

    @PreAuthorize("hasAnyAuthority('super_admin:create','accountant:load_credit')")
    @PostMapping
    public StandardJsonResponse saveCredit(@RequestBody Credit credit, HttpServletRequest request) {
        var user = userService.getCurrentUser(request);
        return creditService.saveCredit(credit, user);
    }

    // logged in as account / reseller -self
    @PostMapping("create-invoice")
    public StandardJsonResponse createSMSInvoice(@RequestBody CreditInvoDto credit, HttpServletRequest request) {
        var user = userService.getCurrentUser(request);

        if(TextUtils.isEmpty(credit.getSmsLoadingMethod())) credit.setSmsLoadingMethod("MONEY");

        if (user.getLayer().equals(Layers.RESELLER)) {
            return invoiceService.resellerLoadCredit(credit, user);
        }
        return invoiceService.accountLoadCredit(credit, user);
    }

    // Manager-approval flow (initiate -> approve) removed: credit is loaded directly via saveCredit /
    // create-invoice. No more PENDING_APPROVAL step.


    //logged in as account - show my credits
    @PostMapping("account")
    public StandardJsonResponse getCreditHistoryAsAccount(HttpServletRequest request) {
        var user = userService.getCurrentUser(request);
        return creditService.getCreditHistoryAsAccount(user.getUsrAccId());
    }

    //logged in as reseller - show account credits
    @PostMapping("reseller-account")
    public StandardJsonResponse getCreditHistoryAsAccountByReseller(@RequestBody CreditFilter creditFilter, HttpServletRequest request, @RequestParam(required = false) String account_id) {
        var user = userService.getCurrentUser(request);

        if (user.getLayer().equals(Layers.ACCOUNT)) creditFilter.setAccId(user.getUsrAccId());

        // Drill-down: scope to the single account being viewed (ownership enforced).
        UUID accScope = accountService.resolveAccountScope(user, account_id);
        if (accScope != null) creditFilter.setAccId(accScope);

        if (creditFilter.getAccId() == null) {
            //show all accounts for reseller
            return creditService.getAllResellerCreditHistory(user, creditFilter);
        }
        return creditService.getCreditHistoryAsAccount(creditFilter.getAccId());
    }

    @PostMapping("invoice-list")
    public StandardJsonResponse getFilteredInvoice(HttpServletRequest request, @RequestBody InvoiceFilter invoiceFilter, @RequestParam(required = false) String account_id) {
        User user = userService.getCurrentUser(request);
        if (user.getLayer().equals(Layers.ACCOUNT)) invoiceFilter.setInvoAccId(user.getUsrAccId());
        if (user.getLayer().equals(Layers.RESELLER)) invoiceFilter.setInvoResellerId(user.getUsrResellerId());
        // Drill-down: scope invoices to the single account being viewed (ownership enforced).
        UUID accScope = accountService.resolveAccountScope(user, account_id);
        if (accScope != null) invoiceFilter.setInvoAccId(accScope);
        return invoiceService.getAllInvoices(invoiceFilter);
    }


    @GetMapping("distinct-status")
    public StandardJsonResponse fetchDistinctStatus() {
        StandardJsonResponse resp = new StandardJsonResponse();
        List<String> statusList = Arrays.stream(CrStatus.values()).map(Enum::name).toList();
        resp.setTotal(statusList.size());
        resp.setData("result", statusList, resp);
        return resp;
    }


    //logged in as reseller - show account credits
    @PostMapping("reseller-account-download")
    public ResponseEntity<byte[]> getCreditHistoryAsAccountByResellerDownload(@RequestBody CreditFilter creditFilter, HttpServletRequest request) {
        var user = userService.getCurrentUser(request);

        if (user.getLayer().equals(Layers.ACCOUNT)) creditFilter.setAccId(user.getUsrAccId());

        byte[] excelBytes = new byte[0];
        if (creditFilter.getAccId() == null) {
            //show all accounts for reseller
       //     excelBytes = creditService.getAllResellerCreditHistoryDownload(user, creditFilter);
        } else {
         //   excelBytes = creditService.getCreditHistoryAsAccountDownload(creditFilter.getAccId());
        }


        if (excelBytes == null) {
//            log.error("excel without data");
            return ResponseEntity.status(500).body(null);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=credit_statement.xlsx");
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }



    @PostMapping("reverse-top-up/{crId}")
    public StandardJsonResponse reverseCredit(@PathVariable UUID crId, HttpServletRequest request) {
//        var user = userService.getCurrentUser(request);
        return creditService.reverseCredit(crId);
    }

}
