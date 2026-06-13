package com.spa.smart_gate_springboot.dashboad;

import com.spa.smart_gate_springboot.account_setup.account.AcStatus;
import com.spa.smart_gate_springboot.account_setup.account.AccountRepository;
import com.spa.smart_gate_springboot.account_setup.reseller.ResellerRepo;
import com.spa.smart_gate_springboot.account_setup.senderId.ShortCodeRepository;
import com.spa.smart_gate_springboot.account_setup.wallet.Wallet;
import com.spa.smart_gate_springboot.account_setup.wallet.WalletRepository;
import com.spa.smart_gate_springboot.account_setup.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Role-scoped overview cards for the top of the dashboard. TOP sees the platform census (every
 * reseller, every account, every sender ID, total reseller cash, units in circulation). A reseller
 * sees the same shape scoped to itself — only its accounts, its sender IDs and its own wallet
 * balance — with the platform-only figures (reseller census, units-in-circulation liability) omitted.
 */
@Service
@RequiredArgsConstructor
public class DashOverviewService {
    private final ResellerRepo resellerRepo;
    private final AccountRepository accountRepository;
    private final ShortCodeRepository shortCodeRepository;
    private final WalletRepository walletRepository;

    /**
     * @param resellerId      null = platform-wide (TOP); otherwise every count is scoped to this reseller.
     * @param includePlatform true only for the platform view — adds the reseller census and the
     *                         units-in-circulation figure, which don't apply to a single reseller.
     */
    public Map<String, Object> buildOverview(UUID resellerId, boolean includePlatform) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("scope", includePlatform ? "TOP" : "RESELLER");

        // Reseller census — platform only.
        if (includePlatform) {
            long rsActive = resellerRepo.countByActiveFlag(true);
            long rsInactive = resellerRepo.countByActiveFlag(false);
            Map<String, Object> resellers = new LinkedHashMap<>();
            resellers.put("active", rsActive);
            resellers.put("inactive", rsInactive);
            resellers.put("total", rsActive + rsInactive);
            out.put("resellers", resellers);
        }

        // Accounts by status.
        List<Object[]> accRows = resellerId == null
                ? accountRepository.countByStatus()
                : accountRepository.countByStatusForReseller(resellerId);
        Map<AcStatus, Long> accByStatus = new EnumMap<>(AcStatus.class);
        AcStatus[] statuses = AcStatus.values();
        for (Object[] row : accRows) {
            if (row[0] == null) continue; // accounts with no status set — not counted in any bucket
            int ordinal = ((Number) row[0]).intValue();
            if (ordinal < 0 || ordinal >= statuses.length) continue; // defensive: unknown ordinal
            accByStatus.put(statuses[ordinal], ((Number) row[1]).longValue());
        }
        long accActive = accByStatus.getOrDefault(AcStatus.ACTIVE, 0L);
        long accOutOfCredit = accByStatus.getOrDefault(AcStatus.OUT_OF_CREDIT, 0L);
        // Disjoint from active/out-of-credit; DELETED accounts are excluded entirely.
        long accInactive = accByStatus.getOrDefault(AcStatus.SUSPENDED, 0L)
                + accByStatus.getOrDefault(AcStatus.DISABLED_BY_ACCOUNTS, 0L);
        Map<String, Object> accounts = new LinkedHashMap<>();
        accounts.put("active", accActive);
        accounts.put("inactive", accInactive);
        accounts.put("outOfCredit", accOutOfCredit);
        accounts.put("total", accActive + accInactive + accOutOfCredit);
        out.put("accounts", accounts);

        // Sender IDs: [promotional, transactional, mapped, pending, total].
        List<Object[]> sidRows = resellerId == null
                ? shortCodeRepository.senderIdStats()
                : shortCodeRepository.senderIdStatsForReseller(resellerId);
        Object[] sid = sidRows.isEmpty() ? new Object[]{0L, 0L, 0L, 0L, 0L} : sidRows.get(0);
        Map<String, Object> senderIds = new LinkedHashMap<>();
        senderIds.put("promotional", num(sid[0]));
        senderIds.put("transactional", num(sid[1]));
        senderIds.put("mapped", num(sid[2]));     // mapped to at least one account (ShStatus.ACTIVE)
        senderIds.put("pending", num(sid[3]));    // not yet mapped (ShStatus.PENDING_MAPPING)
        senderIds.put("total", num(sid[4]));
        out.put("senderIds", senderIds);

        // Wallet balance — TOP: cash across all reseller wallets; reseller: its own available balance.
        BigDecimal walletBalance;
        if (resellerId == null) {
            walletBalance = nz(walletRepository.sumResellerAvailableBalance());
        } else {
            walletBalance = walletRepository.findByWalletCode(WalletService.walletCodeForReseller(resellerId))
                    .map(Wallet::getAvailableBalance).orElse(BigDecimal.ZERO);
        }
        out.put("resellerWalletBalance", walletBalance.toPlainString());

        // Units in circulation (unconsumed) = reseller pools + account balances — platform only.
        if (includePlatform) {
            BigDecimal units = nz(resellerRepo.sumAllocatableUnits())
                    .add(nz(accountRepository.sumAllAccountMsgBal()));
            out.put("unitsInCirculation", units.toPlainString());
        }

        out.put("currency", "KES");
        return out;
    }

    private static long num(Object v) {
        return v == null ? 0L : ((Number) v).longValue();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
