package com.spa.smart_gate_springboot.account_setup.wallet;

import com.spa.smart_gate_springboot.account_setup.credit.Credit;
import com.spa.smart_gate_springboot.account_setup.credit.CreditService;
import com.spa.smart_gate_springboot.account_setup.reseller.Reseller;
import com.spa.smart_gate_springboot.account_setup.reseller.ResellerService;
import com.spa.smart_gate_springboot.account_setup.wallet.dto.BuyUnitsRequest;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.utils.GlobalUtils;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Reseller buys SMS units from TOP using cash-wallet balance (no STK). All-or-nothing in one
 * transaction: debit reseller wallet → credit TOP wallet → allocate units. If allocation throws,
 * the whole transaction rolls back so cash is never lost without units.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletPurchaseService {

    private final WalletService walletService;
    private final ResellerService resellerService;
    private final CreditService creditService;
    private final GlobalUtils gu;

    @Transactional
    public StandardJsonResponse buyUnitsFromWallet(User user, BuyUnitsRequest request) {
        StandardJsonResponse response = new StandardJsonResponse();

        if (user.getLayer() != Layers.RESELLER || user.getUsrResellerId() == null) {
            response.setSuccess(false);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setMessage("message", "Only a reseller can buy units from a wallet", response);
            return response;
        }

        Reseller rs = resellerService.findById(user.getUsrResellerId());
        BigDecimal unitPrice = rs.getRsSmsUnitPrice();
        if (unitPrice == null || unitPrice.signum() <= 0) {
            response.setSuccess(false);
            response.setStatus(HttpStatus.EXPECTATION_FAILED.value());
            response.setMessage("message", "Reseller unit price is not configured", response);
            return response;
        }

        // Resolve units + cash cost from whichever the caller supplied.
        BigDecimal units;
        BigDecimal cost;
        if (request.getUnits() != null && request.getUnits().signum() > 0) {
            units = request.getUnits().setScale(0, RoundingMode.DOWN);
            cost = units.multiply(unitPrice);
        } else if (request.getAmount() != null && request.getAmount().signum() > 0) {
            cost = request.getAmount();
            units = gu.getDivide(cost, unitPrice).setScale(0, RoundingMode.DOWN);
        } else {
            response.setSuccess(false);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setMessage("message", "Provide units or amount to buy", response);
            return response;
        }

        if (units.signum() <= 0 || cost.signum() <= 0) {
            response.setSuccess(false);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setMessage("message", "Amount too small to buy any units", response);
            return response;
        }

        String resellerCode = WalletService.walletCodeForReseller(rs.getRsId());
        Wallet wallet = walletService.getOrCreate(WalletOwnerType.RESELLER, rs.getRsId(), resellerCode);
        if (wallet.getAvailableBalance().compareTo(cost) < 0) {
            response.setSuccess(false);
            response.setStatus(HttpStatus.EXPECTATION_FAILED.value());
            response.setMessage("message", "Insufficient wallet balance. Available KES "
                    + wallet.getAvailableBalance() + ", required KES " + cost, response);
            return response;
        }

        // Idempotency key. A client-supplied key makes a genuine retry a no-op; otherwise we derive a
        // UNIQUE key per request — NOT keyed on (rsId,cost), which would falsely collide two distinct
        // purchases of the same amount and silently skip the second debit while still allocating units.
        String idem = (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank())
                ? request.getIdempotencyKey()
                : "RS_UNITBUY_" + rs.getRsId() + "_" + UUID.randomUUID();

        // Operation-level guard: if this exact key was already processed, the whole purchase already ran
        // (the wallet ledger + the non-idempotent unit allocation). Returning here prevents a replayed
        // key from re-allocating free units.
        if (walletService.isProcessed(idem)) {
            response.setMessage("message", "This purchase was already processed", response);
            return response;
        }

        // 1) Debit reseller wallet, 2) credit TOP wallet, 3) allocate units — same transaction.
        walletService.debit(resellerCode, cost, WalletTxType.UNIT_PURCHASE_DEBIT, idem,
                "Bought " + units + " SMS units from TOP", user.getUsrId());
        walletService.credit(WalletService.TOP_WALLET_CODE, cost, WalletTxType.UNIT_SALE_CREDIT, idem + "_TOP",
                "Units sold to reseller " + rs.getRsCompanyName(), user.getUsrId());

        Credit credit = creditService.allocateResellerUnitsFromWallet(rs, units, cost, user);

        response.setMessage("message", "Bought " + units + " units for KES " + cost
                + ". New allocatable balance: " + rs.getRsAllocatableUnit(), response);
        response.setData("result", credit, response);
        return response;
    }
}
