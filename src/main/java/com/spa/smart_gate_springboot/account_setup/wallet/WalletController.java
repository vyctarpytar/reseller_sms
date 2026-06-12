package com.spa.smart_gate_springboot.account_setup.wallet;

import com.spa.smart_gate_springboot.account_setup.wallet.dto.BuyUnitsRequest;
import com.spa.smart_gate_springboot.account_setup.wallet.dto.WithdrawDto;
import com.spa.smart_gate_springboot.account_setup.wallet.dto.WithdrawFilter;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.payment.mpesa.b2c.B2cTransaction;
import com.spa.smart_gate_springboot.payment.mpesa.b2c.B2cTransactionRepository;
import com.spa.smart_gate_springboot.payment.mpesa.b2c.B2cTransactionStatus;
import com.spa.smart_gate_springboot.payment.mpesa.charge.MpesaB2cCharge;
import com.spa.smart_gate_springboot.payment.mpesa.charge.MpesaB2cChargeRepository;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cash-wallet API for RESELLER and TOP. Response shapes intentionally mirror the legacy NdovuPay
 * contract (walAmount / tarFrom-tarTo-tarCharges / withDraw* fields) so the existing React billing
 * slice + withdrawal pages keep working after repointing their URLs to /api/v2/wallet.
 */
@RestController
@RequestMapping("/api/v2/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final WalletWithdrawalService withdrawalService;
    private final WalletPurchaseService purchaseService;
    private final MpesaB2cChargeRepository chargeRepository;
    private final B2cTransactionRepository b2cRepository;
    private final UserService userService;

    /** Wallet balance — array with a single row carrying walAmount (string), to match the frontend. */
    @PreAuthorize("hasAnyRole('ACCOUNTANT','SUPER_ADMIN','ADMIN')")
    @GetMapping("balance")
    public StandardJsonResponse balance(HttpServletRequest request,
                                        @RequestParam(required = false) String reseller_id) {
        User user = userService.getCurrentUser(request);
        Wallet wallet = resolveWallet(user, reseller_id);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("walAmount", wallet.getAvailableBalance().toPlainString());
        row.put("walCurrency", wallet.getCurrency());
        row.put("walType", "MPESA");
        row.put("walCode", wallet.getWalletCode());

        StandardJsonResponse response = new StandardJsonResponse();
        response.setData("result", List.of(row), response);
        response.setTotal(1);
        return response;
    }

    /** M-Pesa charge bands, mapped to the legacy tariff field names the frontend renders. */
    @PreAuthorize("hasAnyRole('ACCOUNTANT','SUPER_ADMIN','ADMIN')")
    @GetMapping("rates")
    public StandardJsonResponse rates() {
        List<MpesaB2cCharge> bands = chargeRepository.findAllByOrderByMinAmountAsc();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (MpesaB2cCharge b : bands) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("tarId", b.getId());
            r.put("tarFrom", b.getMinAmount());
            r.put("tarTo", b.getMaxAmount());
            r.put("tarType", "MPESA");
            r.put("tarCharges", b.getCharge());
            rows.add(r);
        }
        StandardJsonResponse response = new StandardJsonResponse();
        response.setData("result", rows, response);
        response.setTotal(rows.size());
        return response;
    }

    @PreAuthorize("hasAnyRole('ACCOUNTANT','SUPER_ADMIN','ADMIN')")
    @PostMapping("initiate-withdraw")
    public StandardJsonResponse initiateWithdraw(HttpServletRequest request, @RequestBody WithdrawDto dto) {
        User user = userService.getCurrentUser(request);
        return withdrawalService.initiateWithdrawOtp(user, dto);
    }

    @PreAuthorize("hasAnyRole('ACCOUNTANT','SUPER_ADMIN','ADMIN')")
    @PostMapping("finalize-withdraw")
    public StandardJsonResponse finalizeWithdraw(HttpServletRequest request, @RequestBody WithdrawDto dto) {
        User user = userService.getCurrentUser(request);
        return withdrawalService.finalizeWithdrawOtp(user, dto);
    }

    /** Paged payout history, mapped to legacy withDraw* field names. */
    @PreAuthorize("hasAnyRole('ACCOUNTANT','SUPER_ADMIN','ADMIN')")
    @PostMapping("withdrawals")
    public StandardJsonResponse withdrawals(HttpServletRequest request, @RequestBody WithdrawFilter filter,
                                            @RequestParam(required = false) String reseller_id) {
        User user = userService.getCurrentUser(request);
        String walletCode = resolveWalletCode(user, reseller_id);

        int limit = filter.getLimit() == 0 ? 10 : filter.getLimit();
        Pageable pageable = PageRequest.of(filter.getStart(), limit);

        B2cTransactionStatus status = parseStatus(filter.getWithDrawStatus());
        LocalDateTime from = filter.getWithDrawDateFrom() == null ? null
                : LocalDateTime.ofInstant(filter.getWithDrawDateFrom().toInstant(), ZoneId.systemDefault());
        LocalDateTime to = filter.getWithDrawDateTo() == null ? null
                : LocalDateTime.ofInstant(filter.getWithDrawDateTo().toInstant(), ZoneId.systemDefault());

        Page<B2cTransaction> page = b2cRepository.findFiltered(walletCode, status, from, to, pageable);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (B2cTransaction t : page.getContent()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("withDrawId", t.getId());
            r.put("withDrawAmount", t.getAmount());
            r.put("withDrawCharge", t.getCharge());
            r.put("withDrawCreatedDate", t.getCreatedAt());
            r.put("withDrawStatus", t.getStatus() != null ? t.getStatus().name() : null);
            r.put("withDrawPhoneNumber", t.getPhoneNumber());
            r.put("withDrawCreatedByEmail", t.getCreatedByEmail());
            r.put("withDrawReceipt", t.getMpesaReceipt());
            rows.add(r);
        }
        StandardJsonResponse response = new StandardJsonResponse();
        response.setData("result", rows, response);
        response.setTotal((int) page.getTotalElements());
        return response;
    }

    @GetMapping("/distinct-status")
    public StandardJsonResponse distinctStatus() {
        List<String> statuses = Arrays.stream(B2cTransactionStatus.values()).map(Enum::name).toList();
        StandardJsonResponse response = new StandardJsonResponse();
        response.setData("result", statuses, response);
        response.setTotal(statuses.size());
        return response;
    }

    /** Reseller buys SMS units from TOP using wallet balance (no STK). */
    @PreAuthorize("hasAnyRole('ACCOUNTANT','SUPER_ADMIN','ADMIN','MANAGER')")
    @PostMapping("buy-units")
    public StandardJsonResponse buyUnits(HttpServletRequest request, @RequestBody BuyUnitsRequest body) {
        User user = userService.getCurrentUser(request);
        return purchaseService.buyUnitsFromWallet(user, body);
    }

    /** Admin reversal of a terminally-failed B2C payout (re-credits the wallet). */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ACCOUNTANT')")
    @PostMapping("reverse-withdrawal/{b2cId}")
    public StandardJsonResponse reverseWithdrawal(HttpServletRequest request, @PathVariable UUID b2cId) {
        User user = userService.getCurrentUser(request);
        return withdrawalService.reverseWithdrawal(b2cId, user);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Resolves (and lazily creates) the wallet the caller is acting on, with the correct owner type. */
    private Wallet resolveWallet(User user, String resellerIdParam) {
        if (user.getLayer() == Layers.TOP) {
            // TOP may inspect a specific reseller's wallet; otherwise the platform wallet.
            if (resellerIdParam != null && !resellerIdParam.isBlank()) {
                UUID rsId = UUID.fromString(resellerIdParam);
                return walletService.getOrCreate(WalletOwnerType.RESELLER, rsId,
                        WalletService.walletCodeForReseller(rsId));
            }
            return walletService.getOrCreateTop();
        }
        if (user.getLayer() == Layers.RESELLER && user.getUsrResellerId() != null) {
            return walletService.getOrCreateReseller(user.getUsrResellerId());
        }
        throw new RuntimeException("Only resellers / TOP can use wallet operations");
    }

    private String resolveWalletCode(User user, String resellerIdParam) {
        return resolveWallet(user, resellerIdParam).getWalletCode();
    }

    private B2cTransactionStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return B2cTransactionStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
