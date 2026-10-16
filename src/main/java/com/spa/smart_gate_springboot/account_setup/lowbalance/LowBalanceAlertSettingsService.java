package com.spa.smart_gate_springboot.account_setup.lowbalance;

import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountRepository;
import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.account_setup.reseller.Reseller;
import com.spa.smart_gate_springboot.account_setup.reseller.ResellerRepo;
import com.spa.smart_gate_springboot.account_setup.reseller.ResellerService;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import lombok.RequiredArgsConstructor;
import org.apache.http.util.TextUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Self-service low-balance alert level ({@code LowBalanceAlertCron}): an account manages its own
 * {@code acc_reorder_level}, a reseller its own {@code rs_reorder_level}. Deliberately narrower than
 * the account / reseller save endpoints, which take the whole entity — balances and unit price
 * included — and so are not something to open up to the tenant themselves.
 * <p>
 * The target follows the caller's tenant scope: an ACCOUNT user is pinned to their own account; a
 * RESELLER (or TOP) user drilled into an account ({@code account_id}) edits that account, validated by
 * {@link AccountService#resolveAccountScope}; otherwise a RESELLER user edits their own reseller and a
 * TOP user the reseller they are drilled into ({@code reseller_id}).
 */
@Service
@RequiredArgsConstructor
public class LowBalanceAlertSettingsService {
    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final ResellerService resellerService;
    private final ResellerRepo resellerRepo;

    /** Same default the cron applies to tenants that have not set a level. */
    @Value("${sms.low-balance.threshold:100}")
    private BigDecimal defaultThreshold;

    public StandardJsonResponse get(User user, String resellerId, String accountId) {
        UUID accId = accountService.resolveAccountScope(user, accountId);
        if (accId != null) {
            return accountResponse(accountService.findByAccId(accId));
        }
        return resellerResponse(resellerService.findById(resolveResellerId(user, resellerId)));
    }

    /** {@code level} null resets to the platform default; 0 turns alerts off. */
    public StandardJsonResponse save(User user, String resellerId, String accountId, Long level) {
        if (level != null && level < 0) {
            StandardJsonResponse resp = new StandardJsonResponse();
            resp.setSuccess(false);
            resp.setMessage("message", "Alert level cannot be negative", resp);
            return resp;
        }
        UUID accId = accountService.resolveAccountScope(user, accountId);
        if (accId != null) {
            accountService.findByAccId(accId); // 404 for an unknown account
            accountRepository.updateReorderLevel(accId, level);
            return withMessage(accountResponse(accountService.findByAccId(accId)), level);
        }
        UUID rsId = resolveResellerId(user, resellerId);
        resellerService.findById(rsId);
        resellerRepo.updateReorderLevel(rsId, level == null ? null : String.valueOf(level));
        return withMessage(resellerResponse(resellerService.findById(rsId)), level);
    }

    private UUID resolveResellerId(User user, String resellerId) {
        if (user.getLayer().equals(Layers.RESELLER)) {
            return user.getUsrResellerId();
        }
        if (user.getLayer().equals(Layers.TOP) && !TextUtils.isEmpty(resellerId)) {
            return UUID.fromString(resellerId);
        }
        throw new AccessDeniedException("Select a reseller or account to manage its low-balance alerts");
    }

    private StandardJsonResponse accountResponse(Account acc) {
        Long own = acc.getAccReorderLevel();
        return response("ACCOUNT", acc.getAccName(), own, acc.getAccMsgBal());
    }

    private StandardJsonResponse resellerResponse(Reseller rs) {
        String raw = rs.getRsReorderLevel() == null ? "" : rs.getRsReorderLevel().trim();
        Long own = raw.matches("[0-9]+") ? Long.valueOf(raw) : null; // non-numeric legacy text = default
        return response("RESELLER", rs.getRsCompanyName(), own, rs.getRsAllocatableUnit());
    }

    private StandardJsonResponse response(String scope, String name, Long own, BigDecimal balance) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scope", scope);
        result.put("name", name);
        result.put("level", own != null ? BigDecimal.valueOf(own) : defaultThreshold);
        result.put("usingDefault", own == null);
        result.put("defaultLevel", defaultThreshold);
        result.put("balance", balance == null ? BigDecimal.ZERO : balance);

        StandardJsonResponse resp = new StandardJsonResponse();
        resp.setData("result", result, resp);
        return resp;
    }

    private StandardJsonResponse withMessage(StandardJsonResponse resp, Long level) {
        String message = level == null ? "Alert level reset to the default"
                : level == 0 ? "Low-balance alerts turned off"
                : "You will be alerted when units fall to " + level;
        resp.setMessage("message", message, resp);
        return resp;
    }
}
