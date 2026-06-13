package com.spa.smart_gate_springboot.account_setup.credit;

import com.spa.smart_gate_springboot.account_setup.account.AcStatus;
import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.account_setup.invoice.InvoStatus;
import com.spa.smart_gate_springboot.account_setup.invoice.Invoice;
import com.spa.smart_gate_springboot.account_setup.invoice.InvoiceRepository;
import com.spa.smart_gate_springboot.account_setup.request.ReStatus;
import com.spa.smart_gate_springboot.account_setup.reseller.Reseller;
import com.spa.smart_gate_springboot.account_setup.reseller.ResellerService;
import com.spa.smart_gate_springboot.account_setup.wallet.WalletOwnerType;
import com.spa.smart_gate_springboot.account_setup.wallet.WalletService;
import com.spa.smart_gate_springboot.account_setup.wallet.WalletTxType;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.messaging.send_message.QueueMsgService;
import com.spa.smart_gate_springboot.user.Role;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.utils.GlobalUtils;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import com.spa.smart_gate_springboot.utils.UniqueCodeGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreditService {
    private final CreditRepository creditRepository;
    private final AccountService accountService;
    private final ResellerService resellerService;
    private final QueueMsgService queueMsgService;
    private final InvoiceRepository invoiceRepository;
    private final GlobalUtils gu;
    private final WalletService walletService;

    /**
     * Credits the reseller's cash wallet when an account it owns buys SMS units with real money
     * (cash-only: a Payment id is present). The KSh the account paid becomes the reseller's cash;
     * units inventory is deducted separately. Idempotent on the M-Pesa transId (smsPaymentRef).
     */
    private void creditResellerCashOnUnitSale(Credit credit, Reseller rs) {
        if (credit.getSmsPaymentId() == null) return; // manual/non-cash grant — not real money in
        try {
            walletService.credit(
                    WalletService.walletCodeForReseller(rs.getRsId()),
                    credit.getSmsPayAmount(),
                    WalletTxType.UNIT_SALE_CREDIT,
                    credit.getSmsPaymentRef(),
                    "SMS units purchased by account " + credit.getSmsAccountName(),
                    credit.getSmsCreatedBy(),
                    rs.getRsId(),
                    credit.getSmsAccId());
        } catch (Exception e) {
            log.error("Failed to credit reseller {} cash wallet on unit sale (ref {}): {}",
                    rs.getRsId(), credit.getSmsPaymentRef(), e.getMessage());
            throw e; // money correctness: fail the credit rather than silently drop the cash
        }
    }

    /**
     * Credits the platform (TOP) cash wallet when a reseller buys units from TOP with real money
     * via STK (cash-only). Idempotent on the M-Pesa transId.
     */
    private void creditTopCashOnUnitSale(Credit credit) {
        if (credit.getSmsPaymentId() == null) return;
        try {
            walletService.credit(
                    WalletService.TOP_WALLET_CODE,
                    credit.getSmsPayAmount(),
                    WalletTxType.UNIT_SALE_CREDIT,
                    credit.getSmsPaymentRef(),
                    "SMS units purchased by reseller " + credit.getSmsResellerName(),
                    credit.getSmsCreatedBy(),
                    credit.getSmsResellerId(),
                    null);
        } catch (Exception e) {
            log.error("Failed to credit TOP cash wallet on reseller unit sale (ref {}): {}",
                    credit.getSmsPaymentRef(), e.getMessage());
            throw e;
        }
    }

    /**
     * UNIT statement legs for an account buying units (real money): the units leave the reseller and
     * land on the account. The money leg is recorded separately as the reseller cash credit. Legs use
     * the payment ref (+ leg suffix) for idempotency; null ref (manual grant) → always inserted.
     */
    private void recordAccountPurchaseUnitLegs(Credit credit, Reseller rs, BigDecimal unitsLoaded,
                                               BigDecimal resellerUnitsAfter, BigDecimal accountUnitsAfter) {
        String ref = credit.getSmsPaymentRef();
        walletService.recordUnitLeg(WalletOwnerType.RESELLER, rs.getRsId(), rs.getRsId(), credit.getSmsAccId(),
                unitsLoaded.negate(), resellerUnitsAfter, WalletTxType.UNIT_SALE,
                ref == null ? null : ref + "_U_RS",
                "Units sold to account " + credit.getSmsAccountName(), credit.getSmsCreatedBy());
        walletService.recordUnitLeg(WalletOwnerType.ACCOUNT, credit.getSmsAccId(), rs.getRsId(), credit.getSmsAccId(),
                unitsLoaded, accountUnitsAfter, WalletTxType.UNIT_PURCHASE,
                ref == null ? null : ref + "_U_ACC",
                "Units purchased from reseller " + rs.getRsCompanyName(), credit.getSmsCreatedBy());
    }

    /**
     * UNIT statement legs for a reseller acquiring units from TOP (STK top-up, manual grant or wallet
     * purchase): units leave the TOP pool and land on the reseller. The TOP unit balance is not tracked
     * here so its {@code balanceAfter} is null. {@code baseRef} null (manual grant) → legs always inserted.
     */
    private void recordResellerFromTopUnitLegs(UUID resellerId, String resellerName, BigDecimal units,
                                               BigDecimal resellerUnitsAfter, String baseRef, UUID createdBy) {
        walletService.recordUnitLeg(WalletOwnerType.RESELLER, resellerId, resellerId, null,
                units, resellerUnitsAfter, WalletTxType.UNIT_PURCHASE,
                baseRef == null ? null : baseRef + "_U_RS", "Units purchased from TOP", createdBy);
        walletService.recordUnitLeg(WalletOwnerType.TOP, null, resellerId, null,
                units.negate(), null, WalletTxType.UNIT_SALE,
                baseRef == null ? null : baseRef + "_U_TOP",
                "Units sold to reseller " + resellerName, createdBy);
    }

    public Credit save(Credit credit) {
        return creditRepository.save(credit);
    }


@Transactional
    public StandardJsonResponse saveCreditMajiByte(Credit credit) {
        StandardJsonResponse response = new StandardJsonResponse();

        if (credit.getSmsPaymentId() == null) {
            boolean canLoad = validateResellerBalance(credit.getSmsResellerId(), credit.getSmsPayAmount());
            if (!canLoad) {
                log.error("You cannot Assign more than your Allocatable Units");
                response.setStatus(org.springframework.http.HttpStatus.FORBIDDEN.value());
                response.setSuccess(false);
                response.setMessage("message", "You cannot Assign more than your Allocatable Units", response);
                return response;
            }
        }


        Layers layers = Layers.ACCOUNT;
        credit.setSmsCreatedBy(UUID.fromString("8225cfe4-e679-42b5-be7c-b98314991b39"));
        credit.setSmsCreatedByName("MAJIBYTE_LOGGED_IN_USER");
        credit.setSmsCreatedDate(LocalDateTime.now());
        credit.setCrStatus(CrStatus.PROCESSED);

        if (layers.name().equalsIgnoreCase(Layers.TOP.name())) {
            topLevelLoadCredit(credit, response);
        }
        else if (credit.getSmsAccId() != null && (layers.name().equalsIgnoreCase(Layers.RESELLER.name()))) {
            resellerLoadCredit(credit, response);
        }
        else if (layers.name().equalsIgnoreCase(Layers.RESELLER.name())) {
            resellerLoadSelfCredit(credit, response);
        } else if (layers.name().equalsIgnoreCase(Layers.ACCOUNT.name())) {
            accountLoadCredit(credit, response);
        } else {
            log.error("Unknown layer : {}", layers.name());
            throw new RuntimeException("Layer not Mapped" + layers.name());
        }


        //todo  disabled this
//        UniqueCodeGenerator ug = new UniqueCodeGenerator();
//        String xPlainCode = ug.getUniqueCode();

        //log the invoice

//        Invoice invoice = Invoice.builder().invoCode("SMS" + xPlainCode).invoResellerId(user.getUsrResellerId()).invoAccId(credit.getSmsAccId()).invoStatus(InvoStatus.PENDING_PAYMENT).invoPayerMobileNumber(null).invoLayer(user.getLayer()).invoCreatedByEmail(user.getEmail()).invoCreatedDate(LocalDateTime.now()).invoDueDate(LocalDateTime.now().plusDays(1)).invoCreatedBy(user.getUsrId()).invoAmount(credit.getSmsPayAmount()).invoTaxRate(BigDecimal.valueOf(0.16)).invoAmountAfterTax(new BigDecimal("1.16").multiply(credit.getSmsPayAmount())).invoMonthName(getMonthNameFromDate(LocalDateTime.now())).invoMonthId(getMonthIdFromDate(LocalDateTime.now())).build();
//        invoiceRepository.saveAndFlush(invoice);

        Credit save = save(credit);
        response.setData("result", save, response);
        return response;
    }

    public StandardJsonResponse saveCredit(Credit credit, User user) {
        StandardJsonResponse response = new StandardJsonResponse();

        if (credit.getSmsPaymentId() == null) {
            boolean canLoad = validateResellerBalance(user, credit.getSmsPayAmount());
            if (!canLoad) {
                log.error("You cannot Assign more than your Allocatable Units");
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setSuccess(false);
                response.setMessage("message", "You cannot Assign more than your Allocatable Units", response);
                return response;
            }
        }


        Layers layers = user.getLayer();
        credit.setSmsCreatedBy(user.getUsrId());
        credit.setSmsCreatedByName(user.getEmail());
        credit.setSmsCreatedDate(LocalDateTime.now());
        credit.setCrStatus(CrStatus.PROCESSED);

        if (layers.name().equalsIgnoreCase(Layers.TOP.name())) {
            topLevelLoadCredit(credit, response);
        } else if (credit.getSmsAccId() != null && (layers.name().equalsIgnoreCase(Layers.RESELLER.name()))) {
            resellerLoadCredit(credit, response);
        } else if (layers.name().equalsIgnoreCase(Layers.RESELLER.name())) {
            resellerLoadSelfCredit(credit, response);
        } else if (layers.name().equalsIgnoreCase(Layers.ACCOUNT.name())) {
            accountLoadCredit(credit, response);
        } else {
            log.error("Unknown layer : {}", layers.name());
            throw new RuntimeException("Layer not Mapped" + layers.name());
        }

        // Log an invoice only for manual loads (no pre-existing invoice). Payment-settled credits
        // (smsPaymentId != null) already have an invoice created upfront at create-invoice time by
        // InvoiceService — re-creating one here produced a duplicate PENDING_PAYMENT row on the
        // account's invoice list (one PAID + one phantom PENDING for the same top-up).
        if (credit.getSmsPaymentId() == null) {
            UniqueCodeGenerator ug = new UniqueCodeGenerator();
            String xPlainCode = ug.getUniqueCode();

            Invoice invoice = Invoice.builder().invoCode("SMS" + xPlainCode).invoResellerId(user.getUsrResellerId()).invoAccId(credit.getSmsAccId()).invoStatus(InvoStatus.PENDING_PAYMENT).invoPayerMobileNumber(null).invoLayer(user.getLayer()).invoCreatedByEmail(user.getEmail()).invoCreatedDate(LocalDateTime.now()).invoDueDate(LocalDateTime.now().plusDays(1)).invoCreatedBy(user.getUsrId()).invoAmount(credit.getSmsPayAmount()).invoTaxRate(BigDecimal.ZERO).invoAmountAfterTax(credit.getSmsPayAmount()).invoMonthName(getMonthNameFromDate(LocalDateTime.now())).invoMonthId(getMonthIdFromDate(LocalDateTime.now())).build();
            invoiceRepository.saveAndFlush(invoice);
        }

        response.setData("result", save(credit), response);
        return response;
    }

    private String getMonthNameFromDate(LocalDateTime dateTime) {
        return dateTime.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    private boolean validateResellerBalance(User user, BigDecimal incomingAMount) {

        if (user.getLayer().equals(Layers.RESELLER)) {
            Reseller res = resellerService.findById(user.getUsrResellerId());
            return res.getRsAllocatableUnit().compareTo(incomingAMount) >= 0;
        } else return user.getLayer().equals(Layers.TOP);

    }

    private boolean validateResellerBalance(UUID rsId, BigDecimal incomingAMount) {

        Reseller res = resellerService.findById(rsId);
        return res.getRsAllocatableUnit().compareTo(incomingAMount) >= 0;

    }

    private boolean validateResellerBalanceBcp(User user, BigDecimal incomingAMount) {

        if (user.getLayer().equals(Layers.RESELLER)) {
            Reseller res = resellerService.findById(user.getUsrResellerId());
            BigDecimal allocatedBalance = accountService.getAccountBalancesForReseller(user.getUsrResellerId());
            BigDecimal rsAllocatableMsgBal = res.getRsMsgBal().subtract(allocatedBalance);
            return rsAllocatableMsgBal.compareTo(incomingAMount) >= 0;
        }
        return true;
    }

    public StandardJsonResponse initiateCredit(Credit credit, User user) {
        StandardJsonResponse response = new StandardJsonResponse();
        credit.setSmsCreatedBy(user.getUsrId());
        credit.setSmsCreatedByName(user.getEmail());
        credit.setSmsCreatedDate(LocalDateTime.now());
        credit.setCrStatus(CrStatus.PENDING_APPROVAL);
        response.setMessage("message", "Top of amount " + credit.getSmsPayAmount() + " initiated successfully", response);
        response.setData("result", save(credit), response);
        return response;
    }

    @Transactional
    public void accountLoadCredit(Credit credit, StandardJsonResponse response) {

        Account account = accountService.findByAccId(credit.getSmsAccId());
        BigDecimal accMsgBal = account.getAccMsgBal();
        if (accMsgBal == null) {
            accMsgBal = BigDecimal.ZERO;
        }

        credit.setSmsPrevBal(accMsgBal);
        BigDecimal accSmsPrice = account.getAccSmsPrice();
        if (accSmsPrice == null) {
            accSmsPrice = new BigDecimal("0.5");
        }

        Reseller rs = resellerService.findById(account.getAccResellerId());
        credit.setSmsResellerId(rs.getRsId());
        credit.setSmsResellerName(rs.getRsCompanyName());

        credit.setSmsAccId(account.getAccId());
        credit.setSmsAccountName(account.getAccName());
        BigDecimal unitsLoaded = gu.getDivide(credit.getSmsPayAmount(), accSmsPrice);
        credit.setSmsLoaded(unitsLoaded.longValue());
        BigDecimal newBal = accMsgBal.add(credit.getSmsPayAmount());
        credit.setSmsNewBal(newBal);
        credit.setSmsRate(account.getAccSmsPrice());

        account.setAccMsgBal(newBal);
        account.setAccStatus(AcStatus.ACTIVE);
        accountService.save(account);

        //deduct Reseller Units
        rs.setRsAllocatableUnit(rs.getRsAllocatableUnit().subtract(unitsLoaded));
        resellerService.save(rs);

        // Real money paid by the account becomes the reseller's cash (units inventory left the reseller).
        creditResellerCashOnUnitSale(credit, rs);
        recordAccountPurchaseUnitLegs(credit, rs, unitsLoaded, rs.getRsAllocatableUnit(),
                gu.getDivide(newBal, accSmsPrice));

        queueMsgService.resendPendingSMSAccountCredit(account.getAccId());

        response.setMessage("message", "Top of amount " + credit.getSmsPayAmount() + "  done successfully. New Balance : " + newBal, response);

    }

    private void resellerLoadSelfCredit(Credit credit, StandardJsonResponse response) {

        Reseller rs = resellerService.findById(credit.getSmsResellerId());
        BigDecimal accMsgBal = rs.getRsMsgBal();
        if (accMsgBal == null) {
            accMsgBal = BigDecimal.ZERO;
        }

        credit.setSmsPrevBal(accMsgBal);
        BigDecimal accSmsPrice = rs.getRsSmsUnitPrice();
        if (accSmsPrice == null) {
            accSmsPrice = new BigDecimal("0.5");
        }
        BigDecimal rsAllocatableUnits = rs.getRsAllocatableUnit();
        if (rsAllocatableUnits == null) {
            rsAllocatableUnits = BigDecimal.ZERO;
        }

        credit.setSmsResellerId(rs.getRsId());
        credit.setSmsResellerName(rs.getRsCompanyName());

        BigDecimal divide = gu.getDivide(credit.getSmsPayAmount(), accSmsPrice);
        credit.setSmsLoaded(divide.longValue());
        rsAllocatableUnits = rsAllocatableUnits.add(divide);
        BigDecimal newBal = accMsgBal.add(credit.getSmsPayAmount());
        credit.setSmsNewBal(newBal);
        credit.setSmsRate(rs.getRsSmsUnitPrice());
        rs.setRsAllocatableUnit(rsAllocatableUnits);
        rs.setRsMsgBal(newBal);
        rs.setRsStatus(ReStatus.ACTIVE.name());
        resellerService.save(rs);

        // Reseller bought units from TOP with real money via STK — TOP receives the cash.
        creditTopCashOnUnitSale(credit);
        recordResellerFromTopUnitLegs(rs.getRsId(), rs.getRsCompanyName(), divide, rsAllocatableUnits,
                credit.getSmsPaymentRef(), credit.getSmsCreatedBy());

        queueMsgService.resendPendingSMSResellerCredit(rs.getRsId());

        response.setMessage("message", "Top of amount " + credit.getSmsPayAmount() + "  done successfully. New Balance : " + newBal, response);

    }

    /**
     * Allocates {@code units} of SMS credit to a reseller and records a Credit ledger row, WITHOUT any
     * cash-wallet movement. Used by the buy-units-from-wallet flow, where the caller has already
     * debited the reseller wallet and credited TOP within the same transaction.
     * Returns the persisted Credit.
     */
    @Transactional
    public Credit allocateResellerUnitsFromWallet(Reseller rs, BigDecimal units, BigDecimal cashSpent, User user) {
        BigDecimal rsMsgBal = rs.getRsMsgBal() == null ? BigDecimal.ZERO : rs.getRsMsgBal();
        BigDecimal rsAllocatable = rs.getRsAllocatableUnit() == null ? BigDecimal.ZERO : rs.getRsAllocatableUnit();

        Credit credit = Credit.builder()
                .smsResellerId(rs.getRsId())
                .smsResellerName(rs.getRsCompanyName())
                .smsPayAmount(cashSpent)
                .smsPrevBal(rsMsgBal)
                .smsLoaded(units.longValue())
                .smsRate(rs.getRsSmsUnitPrice())
                .smsLoadingMethod("WALLET")
                .build();

        rs.setRsAllocatableUnit(rsAllocatable.add(units));
        rs.setRsMsgBal(rsMsgBal.add(cashSpent));
        rs.setRsStatus(ReStatus.ACTIVE.name());
        resellerService.save(rs);

        credit.setSmsNewBal(rs.getRsMsgBal());
        credit.setSmsCreatedBy(user.getUsrId());
        credit.setSmsCreatedByName(user.getEmail());
        credit.setSmsCreatedDate(LocalDateTime.now());
        credit.setCrStatus(CrStatus.PROCESSED);
        Credit saved = save(credit);

        queueMsgService.resendPendingSMSResellerCredit(rs.getRsId());
        return saved;
    }

    private void resellerLoadCredit(Credit credit, StandardJsonResponse response) {

        Account account = accountService.findByAccId(credit.getSmsAccId());
        BigDecimal accMsgBal = account.getAccMsgBal();
        if (accMsgBal == null) {
            accMsgBal = BigDecimal.ZERO;
        }

        credit.setSmsPrevBal(accMsgBal);
        BigDecimal accSmsPrice = account.getAccSmsPrice();
        if (accSmsPrice == null) {
            accSmsPrice = new BigDecimal("0.5");
        }
        Reseller rs = resellerService.findById(account.getAccResellerId());
        credit.setSmsResellerId(rs.getRsId());
        credit.setSmsResellerName(rs.getRsCompanyName());
        credit.setSmsAccId(account.getAccId());
        credit.setSmsAccountName(account.getAccName());

        BigDecimal unitsLoaded;
        BigDecimal newBal;
        if (credit.getSmsLoadingMethod().equalsIgnoreCase("UNITS")) {
            unitsLoaded = credit.getSmsPayAmount();
            BigDecimal amountLoaded = credit.getSmsPayAmount().multiply(account.getAccSmsPrice());
            newBal = accMsgBal.add(amountLoaded);
            credit.setSmsPayAmount(amountLoaded);
        } else {
            unitsLoaded = gu.getDivide(credit.getSmsPayAmount(), accSmsPrice);
            newBal = accMsgBal.add(credit.getSmsPayAmount());
        }


        credit.setSmsLoaded(unitsLoaded.longValue());

        credit.setSmsNewBal(newBal);
        credit.setSmsRate(account.getAccSmsPrice());
        credit.setSmsNewResellerUnits(rs.getRsAllocatableUnit().subtract(unitsLoaded));
        account.setAccMsgBal(newBal);
        account.setAccStatus(AcStatus.ACTIVE);
        accountService.save(account);

        // update reseller allocatable units
        rs.setRsAllocatableUnit(rs.getRsAllocatableUnit().subtract(unitsLoaded));
        resellerService.save(rs);

        // Same economic event as accountLoadCredit: account units funded by real money → reseller gets the cash.
        creditResellerCashOnUnitSale(credit, rs);
        recordAccountPurchaseUnitLegs(credit, rs, unitsLoaded, rs.getRsAllocatableUnit(),
                gu.getDivide(newBal, accSmsPrice));

        queueMsgService.resendPendingSMSAccountCredit(account.getAccId());

        response.setMessage("message", "Top of amount " + credit.getSmsPayAmount() + "  done successfully. New Balance : " + newBal, response);


    }

    @Transactional
    public void topLevelLoadCredit(Credit credit, StandardJsonResponse response) {

        Reseller reseller = resellerService.findById(credit.getSmsResellerId());
        BigDecimal rsMsgBal = reseller.getRsMsgBal();
        if (rsMsgBal == null) {
            rsMsgBal = BigDecimal.ZERO;
        }
        credit.setSmsAccId(null);
        credit.setSmsPrevBal(rsMsgBal);
        credit.setSmsResellerId(reseller.getRsId());
        credit.setSmsResellerName(reseller.getRsCompanyName());
        BigDecimal rsSmsUnitPrice = reseller.getRsSmsUnitPrice();

        if (rsSmsUnitPrice == null) {
            rsSmsUnitPrice = new BigDecimal("0.5");
        }
        BigDecimal rsAllocatableUnits = reseller.getRsAllocatableUnit();
        if (rsAllocatableUnits == null) {
            rsAllocatableUnits = BigDecimal.ZERO;
        }

        BigDecimal unitsLoaded;
        BigDecimal newBal;
        if (credit.getSmsLoadingMethod().equalsIgnoreCase("UNITS")) {
            unitsLoaded = credit.getSmsPayAmount();
            BigDecimal amountLoaded = credit.getSmsPayAmount().multiply(rsSmsUnitPrice);
            newBal = rsMsgBal.add(amountLoaded);
            credit.setSmsPayAmount(amountLoaded);
        } else {
            unitsLoaded = gu.getDivide(credit.getSmsPayAmount(), rsSmsUnitPrice);
            newBal = rsMsgBal.add(credit.getSmsPayAmount());
        }

        credit.setSmsLoaded(unitsLoaded.longValue());
        rsAllocatableUnits = rsAllocatableUnits.add(unitsLoaded);

        credit.setSmsNewBal(newBal);
        credit.setSmsRate(reseller.getRsSmsUnitPrice());

        reseller.setRsMsgBal(newBal);
        reseller.setRsStatus("ACTIVE");
        reseller.setRsAllocatableUnit(rsAllocatableUnits);
        resellerService.save(reseller);

        recordResellerFromTopUnitLegs(reseller.getRsId(), reseller.getRsCompanyName(), unitsLoaded,
                rsAllocatableUnits, credit.getSmsPaymentRef(), credit.getSmsCreatedBy());

        queueMsgService.resendPendingSMSResellerCredit(reseller.getRsId());
        response.setMessage("message", "Top of amount " + credit.getSmsPayAmount() + "  done successfully. New Ballocatable Balance : " + rsAllocatableUnits, response);

    }

    public StandardJsonResponse getAllResellerCreditHistory(User user, CreditFilter filterDto) {
        if (user.getLayer().equals(Layers.ACCOUNT)) filterDto.setAccId(user.getUsrAccId());
        if (user.getLayer().equals(Layers.RESELLER)) filterDto.setResellerId(user.getUsrResellerId());
        if (user.getRole().equals(Role.SALE)) filterDto.setSaleUserId(user.getUsrId());
        if (user.getLayer().equals(Layers.TOP) && (user.getUsrId().equals(UUID.fromString("50b0ad9d-7471-4143-8f4b-57838360cb4a")))) { // sync TOP
            filterDto.setResellerId(UUID.fromString("c3a1822b-72f3-4176-9b64-093fbf0a8c0d")); // sync Reseller
        }

        if (filterDto.getLimit() == 0) filterDto.setLimit(10);
        filterDto.setSortColumn("sms_created_date");
        Pageable pageable = PageRequest.of(filterDto.getStart(), filterDto.getLimit(), Sort.by(filterDto.getSortColumn()).descending());

        if (filterDto.getCrStatus() != null) filterDto.setCrStatus("%" + filterDto.getCrStatus() + "%");
        if (filterDto.getSmsAccountName() != null)
            filterDto.setSmsAccountName("%" + filterDto.getSmsAccountName() + "%");

        Page<Credit> pagedData = creditRepository.getAllAccuntsCreditsByResellerId(filterDto.getResellerId(), filterDto.getSaleUserId(), filterDto.getAccId(), filterDto.getCrStatus(), filterDto.getSmsAccountName(), pageable);


        StandardJsonResponse response = new StandardJsonResponse();

        response.setData("result", pagedData.getContent(), response);
        response.setTotal((int) pagedData.getTotalElements());
        return response;
    }

    public StandardJsonResponse getResellerCreditHistory(UUID resellerId) {
        StandardJsonResponse response = new StandardJsonResponse();
        var list = creditRepository.findBySmsResellerIdOrderBySmsCreatedDateDesc(resellerId);
        response.setData("result", list, response);
        response.setTotal(list.size());
        return response;
    }

    public StandardJsonResponse getCreditHistoryAsAccount(UUID usrAccId) {
        StandardJsonResponse response = new StandardJsonResponse();
        List<Credit> list = creditRepository.findBySmsAccIdOrderBySmsCreatedDateDesc(usrAccId);
        response.setData("result", list, response);
        response.setTotal(list.size());
        return response;
    }

    public StandardJsonResponse approveCredit(UUID crId, User user) {
        StandardJsonResponse response = new StandardJsonResponse();
        Credit credit = findById(crId);
        credit.setSmsApprovedBy(user.getUsrId());
        credit.setSmsApprovedByName(user.getEmail());
        credit.setSmsApprovedDate(LocalDateTime.now());
        credit.setCrStatus(CrStatus.PROCESSED);
        resellerLoadCredit(credit, response);
        response.setData("result", save(credit), response);
        return response;
    }
  public StandardJsonResponse reverseCredit(UUID crId) {
        StandardJsonResponse response = new StandardJsonResponse();
        Credit credit = findById(crId);
        credit.setCrStatus(CrStatus.REVERSED);

        if(credit.getSmsAccId() != null) {
            accountReverseCredit(credit);
        }

        response.setData("result", save(credit), response);
        return response;
    }

    private void accountReverseCredit(Credit credit) {

        Account acc = accountService.findByAccId(credit.getSmsAccId());
        BigDecimal accMsgBal = acc.getAccMsgBal();
        if (accMsgBal == null) {
            accMsgBal = BigDecimal.ZERO;
        }

        acc.setAccMsgBal(accMsgBal.subtract(credit.getSmsPayAmount()));
        accountService.save(acc);


//        todo add back the units to the reseller

    }


    private Credit findById(UUID crId) {
        return creditRepository.findById(crId).orElseThrow(() -> new RuntimeException("credit not found with id: " + crId));
    }


    //    @PostConstruct
    public void updateMonthName() {
        List<Invoice> invoiceList = invoiceRepository.findAll();
        for (Invoice invoice : invoiceList) {
            invoice.setInvoMonthName(getMonthNameFromDate(invoice.getInvoCreatedDate()));
            invoice.setInvoMonthId(getMonthIdFromDate(invoice.getInvoCreatedDate()));
            invoiceRepository.saveAndFlush(invoice);
        }
    }

    private int getMonthIdFromDate(LocalDateTime dateTime) {
        return dateTime.getMonth().getValue();
    }

    public StandardJsonResponse getCreditLoadedToResellers(User user, CreditFilter filterDto) {
        if (filterDto.getLimit() == 0) filterDto.setLimit(10);
        filterDto.setSortColumn("sms_created_date");
        Pageable pageable = PageRequest.of(filterDto.getStart(), filterDto.getLimit(), Sort.by(filterDto.getSortColumn()).descending());
        Page<Credit> pagedData = creditRepository.getCreditLoadedToResellers(user.getUsrId(), pageable);
        StandardJsonResponse response = new StandardJsonResponse();

        response.setData("result", pagedData.getContent(), response);
        response.setTotal((int) pagedData.getTotalElements());
        return response;
    }

  /*
    public byte[] getAllResellerCreditHistoryDownload(User user, CreditFilter filterDto) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sms_list_Excell");
        if (user.getLayer().equals(Layers.ACCOUNT)) filterDto.setAccId(user.getUsrAccId());
        if (user.getLayer().equals(Layers.RESELLER)) filterDto.setResellerId(user.getUsrResellerId());
        if (user.getRole().equals(Role.SALE)) filterDto.setSaleUserId(user.getUsrId());
        if (user.getLayer().equals(Layers.TOP)
                && (user.getUsrId().equals(UUID.fromString("50b0ad9d-7471-4143-8f4b-57838360cb4a")))) { // sync TOP
            filterDto.setResellerId(UUID.fromString("c3a1822b-72f3-4176-9b64-093fbf0a8c0d")); // sync Reseller
        }

        if (filterDto.getLimit() == 0) filterDto.setLimit(10);
        filterDto.setSortColumn("sms_created_date");
        Pageable pageable = PageRequest.of(filterDto.getStart(), filterDto.getLimit(), Sort.by(filterDto.getSortColumn()).descending());

        if (filterDto.getCrStatus() != null) filterDto.setCrStatus("%" + filterDto.getCrStatus() + "%");
        if (filterDto.getSmsAccountName() != null)
            filterDto.setSmsAccountName("%" + filterDto.getSmsAccountName() + "%");


        createExcellHeader(sheet);

        Page<Credit> pagedData = creditRepository.getAllAccuntsCreditsByResellerId(filterDto.getResellerId(), filterDto.getSaleUserId(), filterDto.getAccId(), filterDto.getCrStatus(), filterDto.getSmsAccountName(), pageable);

        createExcellRows();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }


    private void createExcellRows() {
        List<MsgMessageQueueArc> list = pagedData.getContent();

        if (!list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                // Adding a sample row of data
                MsgMessageQueueArc m = list.get(i);
                Row row = sheet.createRow(i + 1); // Start from the second row
                row.createCell(0).setCellValue(m.getMsgCreatedDate().toString());
                row.createCell(1).setCellValue(m.getMsgSubMobileNo());
                row.createCell(2).setCellValue(m.getMsgStatus());
                row.createCell(3).setCellValue(m.getMsgAccName());
                row.createCell(4).setCellValue(m.getMsgSenderIdName());
                row.createCell(5).setCellValue(m.getMsgCostId().doubleValue());
                row.createCell(6).setCellValue(m.getMsgMessage());

            }
        } else {
            Row sampleRow = sheet.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                sampleRow.createCell(i).setCellValue("N/A");
            }
        }
    }

    private void createExcellHeader(Sheet sheet) {
        // Create header row and style it
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
//            headerStyle.setLocked(true);

        String[] headers = {"Date", "Mobile", "Msg Status", "Client", "Sender Id", "Cost", "Message"};
        int[] columnWidths = {30, 20, 20, 20, 20, 10, 200}; // Set column widths

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, columnWidths[i] * 256); // Set column width
        }

    }

    public byte[] getCreditHistoryAsAccountDownload(UUID usrAccId) {
        StandardJsonResponse response = new StandardJsonResponse();
        List<Credit> list = creditRepository.findBySmsAccIdOrderBySmsCreatedDateDesc(usrAccId);
        response.setData("result", list, response);
        response.setTotal(list.size());
        return response;
    }
*/
}
