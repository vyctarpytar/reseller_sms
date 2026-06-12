package com.spa.smart_gate_springboot.account_setup.wallet;

import com.spa.smart_gate_springboot.account_setup.reseller.Reseller;
import com.spa.smart_gate_springboot.account_setup.reseller.ResellerService;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
    private final ResellerService resellerService;
    private final WalletTransactionRepository walletTxRepository;
    private final com.spa.smart_gate_springboot.payment.mpesa.gateway.WaretechMpesaService mpesaService;

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
        // Unit price the reseller pays when buying SMS units from wallet balance — lets the frontend
        // cap a "buy units" purchase at the available balance (cost = units × walUnitPrice).
        BigDecimal unitPrice = resolveUnitPrice(user, reseller_id);
        row.put("walUnitPrice", unitPrice != null ? unitPrice.toPlainString() : null);

        StandardJsonResponse response = new StandardJsonResponse();
        response.setData("result", List.of(row), response);
        response.setTotal(1);
        return response;
    }

    /**
     * Live Safaricom paybill balance (working + utility float) for the B2C shortcode. TOP-only —
     * this is the platform's actual M-Pesa money, not an internal wallet ledger. Returns nulls if
     * the gateway is unreachable so the UI can show "unavailable" rather than error.
     */
    @PreAuthorize("hasAnyRole('ACCOUNTANT','SUPER_ADMIN','ADMIN')")
    @GetMapping("mpesa-balance")
    public StandardJsonResponse mpesaBalance(HttpServletRequest request) {
        User user = userService.getCurrentUser(request);
        if (user.getLayer() != Layers.TOP) {
            StandardJsonResponse forbidden = new StandardJsonResponse();
            forbidden.setSuccess(false);
            forbidden.setStatus(HttpStatus.FORBIDDEN.value());
            forbidden.setMessage("message", "Only TOP can view the M-Pesa paybill balance", forbidden);
            return forbidden;
        }

        var balance = mpesaService.queryBalance();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("shortcode", balance != null ? balance.getShortcode() : null);
        row.put("workingBalance", balance != null ? balance.getWorkingBalance() : null);
        row.put("utilityBalance", balance != null ? balance.getUtilityBalance() : null);
        row.put("available", balance != null);

        StandardJsonResponse response = new StandardJsonResponse();
        if (balance == null) {
            response.setMessage("message", "M-Pesa balance is temporarily unavailable", response);
        }
        response.setData("result", row, response);
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

    /**
     * Wallet statement — the full signed cash ledger (deposits, unit-purchase debits, withdrawals,
     * reversals, adjustments) newest-first, with the running balanceAfter stored on each row.
     * This is the only view of wallet movements other than withdrawals; paged via start/limit.
     */
    @PreAuthorize("hasAnyRole('ACCOUNTANT','SUPER_ADMIN','ADMIN','MANAGER')")
    @GetMapping("statement")
    public StandardJsonResponse statement(HttpServletRequest request,
                                          @RequestParam(required = false) String reseller_id,
                                          @RequestParam(defaultValue = "0") int start,
                                          @RequestParam(defaultValue = "10") int limit) {
        User user = userService.getCurrentUser(request);
        String walletCode = resolveWalletCode(user, reseller_id);

        Pageable pageable = PageRequest.of(start, limit <= 0 ? 10 : limit);
        Page<WalletTransaction> page =
                walletTxRepository.findByWalletCodeOrderByCreatedAtDesc(walletCode, pageable);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (WalletTransaction t : page.getContent()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("txId", t.getTxId());
            r.put("txType", t.getTxType() != null ? t.getTxType().name() : null);
            r.put("txLabel", t.getTxType() != null ? labelFor(t.getTxType()) : null);
            r.put("amount", t.getAmount());                 // signed: + credit, − debit
            r.put("direction", t.getAmount() != null && t.getAmount().signum() < 0 ? "DEBIT" : "CREDIT");
            r.put("balanceAfter", t.getBalanceAfter());
            r.put("narration", t.getNarration());
            r.put("reference", t.getExternalRef());
            r.put("createdAt", t.getCreatedAt());
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

    /** Unit price for the reseller whose wallet is being viewed; null for the TOP platform wallet. */
    private BigDecimal resolveUnitPrice(User user, String resellerIdParam) {
        UUID rsId = null;
        if (user.getLayer() == Layers.RESELLER) {
            rsId = user.getUsrResellerId();
        } else if (user.getLayer() == Layers.TOP && resellerIdParam != null && !resellerIdParam.isBlank()) {
            rsId = UUID.fromString(resellerIdParam);
        }
        if (rsId == null) return null;
        Reseller rs = resellerService.findById(rsId);
        return rs != null ? rs.getRsSmsUnitPrice() : null;
    }

    /** Human-readable label for a ledger entry type, for the statement table. */
    private String labelFor(WalletTxType type) {
        switch (type) {
            case DEPOSIT_MPESA: return "M-Pesa deposit";
            case UNIT_SALE_CREDIT: return "Units sold";
            case UNIT_PURCHASE_DEBIT: return "Units bought";
            case WITHDRAWAL: return "Withdrawal";
            case MPESA_CHARGE: return "M-Pesa charge";
            case WITHDRAWAL_REVERSAL: return "Withdrawal reversal";
            case MPESA_CHARGE_REVERSAL: return "Charge reversal";
            case ADJUSTMENT: return "Adjustment";
            default: return type.name();
        }
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
