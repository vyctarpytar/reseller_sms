package com.spa.smart_gate_springboot.account_setup.wallet;

import com.spa.smart_gate_springboot.account_setup.reseller.Reseller;
import com.spa.smart_gate_springboot.account_setup.reseller.ResellerRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Ensures the platform (TOP) cash wallet and a cash wallet per existing reseller exist on startup.
 * All wallets start at balance 0 — historical balances are NOT replayed (the cash wallet is new money).
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(50)
public class WalletBootstrap implements ApplicationRunner {

    private final WalletService walletService;
    private final ResellerRepo resellerRepo;

    @Override
    public void run(ApplicationArguments args) {
        try {
            walletService.getOrCreateTop();
        } catch (Exception e) {
            log.error("Failed to ensure TOP_PLATFORM wallet: {}", e.getMessage());
        }

        int count = 0;
        for (Reseller r : resellerRepo.findAll()) {
            try {
                walletService.getOrCreateReseller(r.getRsId()); // idempotent — no-op if already present
                count++;
            } catch (Exception e) {
                log.error("Failed to ensure wallet for reseller {}: {}", r.getRsId(), e.getMessage());
            }
        }
        log.info("[WalletBootstrap] Ensured TOP wallet + wallets for {} resellers", count);
    }
}
