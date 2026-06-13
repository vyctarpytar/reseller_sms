package com.spa.smart_gate_springboot.messaging.send_message.api;

import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.account_setup.invoice.CreditInvoDto;
import com.spa.smart_gate_springboot.account_setup.invoice.Invoice;
import com.spa.smart_gate_springboot.account_setup.invoice.InvoStatus;
import com.spa.smart_gate_springboot.account_setup.invoice.InvoiceRepository;
import com.spa.smart_gate_springboot.account_setup.invoice.InvoiceService;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.user.UsrStatus;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Sandbox (API-key authenticated) account self-service: read SMS balance, load credit, and poll the
 * resulting invoice. These let a client app (e.g. Nineyard) embed the load-SMS flow in its own UI
 * instead of sending users to the Synq portal.
 *
 * <p>Loading does NOT reimplement crediting — it delegates to the existing
 * {@link InvoiceService#accountLoadCredit(CreditInvoDto, User)} which creates a PENDING_PAYMENT
 * invoice and fires the M-Pesa STK push. The account is credited only when Safaricom's callback
 * settles the payment (the existing receivePayment / handleStkCallback path), so no money moves
 * here without the payer actually paying.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SandboxAccountService {

    private final ApiKeyRepository apiKeyRepository;
    private final AccountService accountService;
    private final UserService userService;
    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;

    /**
     * Resolve the API key record and reject it if missing, inactive or expired. Unlike the legacy
     * single-sms path (which only does findByApiKey), these endpoints move money, so we enforce the
     * same active+expiry contract that {@code existsValidApiKey} checks — here, on the fetched entity.
     */
    private ApiKey resolveKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("API KEY missing!!!");
        }
        ApiKey key = apiKeyRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("Invalid API key"));
        if (key.getActive() == null || !key.getActive()) {
            throw new RuntimeException("API key is inactive");
        }
        if (key.getExpirationDate() != null && key.getExpirationDate().before(new Date())) {
            throw new RuntimeException("API key has expired");
        }
        return key;
    }

    /** Current SMS balance (KSh) + derived units for the account this API key belongs to. */
    public StandardJsonResponse getBalance(String apiKey) {
        ApiKey key = resolveKey(apiKey);
        return accountService.getBalanceDto(key.getApiAccId());
    }

    /**
     * Load SMS credit for the API key's account: creates the invoice and launches the STK push via the
     * existing account-load flow. Returns the saved invoice (under {@code data.result}) whose
     * {@code invoCode} the caller polls with {@link #invoiceStatus(String, String)}.
     */
    public StandardJsonResponse loadCredit(String apiKey, CreditInvoDto dto) {
        ApiKey key = resolveKey(apiKey);
        UUID accId = key.getApiAccId();

        if (TextUtils.isEmpty(dto.getSmsLoadingMethod())) {
            dto.setSmsLoadingMethod("MONEY");
        }

        // accountLoadCredit resolves the account/reseller from the User and records invoCreatedBy,
        // which the M-Pesa settlement callback later resolves via userService.findById(...). So we
        // must hand it a REAL user that belongs to this account, not a synthetic one.
        List<User> users = userService.findByUsrAccId(accId);
        if (users.isEmpty()) {
            StandardJsonResponse resp = new StandardJsonResponse();
            resp.setSuccess(false);
            resp.setStatus(HttpStatus.BAD_REQUEST.value());
            resp.setMessage("message", "No user is attached to this account; cannot load credit.", resp);
            return resp;
        }

        // Prefer an ACTIVE user for invoCreatedBy — a DELETED/INACTIVE one is only a fallback so the
        // load still works even if the account's first user has been deactivated.
        User user = users.stream()
                .filter(u -> u.getUsrStatus() == UsrStatus.ACTIVE)
                .findFirst()
                .orElse(users.get(0));

        return invoiceService.accountLoadCredit(dto, user);
    }

    /**
     * Status of a load invoice, scoped to the API key's own account (a key may only see its own
     * invoices). The frontend polls this until {@code paid} is true (or a terminal failure status).
     */
    public StandardJsonResponse invoiceStatus(String apiKey, String invoiceCode) {
        ApiKey key = resolveKey(apiKey);
        StandardJsonResponse resp = new StandardJsonResponse();

        Invoice invoice = invoiceRepository.findByInvoCode(invoiceCode).orElse(null);
        if (invoice == null || !key.getApiAccId().equals(invoice.getInvoAccId())) {
            resp.setSuccess(false);
            resp.setStatus(HttpStatus.NOT_FOUND.value());
            resp.setMessage("message", "Invoice not found", resp);
            return resp;
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("invoiceCode", invoice.getInvoCode());
        result.put("status", invoice.getInvoStatus() == null ? null : invoice.getInvoStatus().name());
        result.put("paid", invoice.getInvoStatus() == InvoStatus.PAID);
        result.put("amount", invoice.getInvoAmount());
        result.put("failureReason", invoice.getInvoFailureReason());
        result.put("mpesaReceipt", invoice.getInvoMpesaReceipt());
        resp.setData("result", result, resp);
        return resp;
    }
}
