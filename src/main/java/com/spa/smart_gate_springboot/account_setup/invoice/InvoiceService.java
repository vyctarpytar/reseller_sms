package com.spa.smart_gate_springboot.account_setup.invoice;

import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.account_setup.credit.Credit;
import com.spa.smart_gate_springboot.account_setup.credit.CreditService;
import com.spa.smart_gate_springboot.account_setup.reseller.ResellerRepo;
import com.spa.smart_gate_springboot.payment.Payment;
import com.spa.smart_gate_springboot.payment.PaymentDto;
import com.spa.smart_gate_springboot.payment.PaymentService;
import com.spa.smart_gate_springboot.payment.StkCallbackDto;
import com.spa.smart_gate_springboot.pushSDK.PushSDKConfigService;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.GlobalExceptionHandler;
import com.spa.smart_gate_springboot.utils.GlobalUtils;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import com.spa.smart_gate_springboot.utils.UniqueCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.http.util.TextUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final CreditService creditService;
    private final PaymentService paymentService;
    private final UserService userService;
    private final PushSDKConfigService pushSDKConfigService;
    private final GlobalUtils gu;
    private final ResellerRepo resellerRepo;
    private final AccountService accountService;


    public StandardJsonResponse accountLoadCredit(CreditInvoDto credit, User user) {

        BigDecimal unitsToBuy =credit.getSmsPayAmount();

        if(credit.getSmsLoadingMethod().equalsIgnoreCase("MONEY")){
            BigDecimal accSmsPrice = accountService.findByAccId(user.getUsrAccId()).getAccSmsPrice();
            unitsToBuy = gu.getDivide(credit.getSmsPayAmount(), accSmsPrice);
        }

        boolean canBuySms = checkIfResellerHasAvailableUnits(user.getUsrResellerId(),unitsToBuy);
        StandardJsonResponse response = new StandardJsonResponse();
        if (!canBuySms) {
            log.info("Failed !! Reseller toload allocatable Units for {}", user.getUsrResellerId());
            response.setMessage("message", "Failed !! Contact Service Provider to Load Credit", response);
            response.setStatus(HttpStatus.SC_EXPECTATION_FAILED);
            response.setSuccess(false);
            return response;
        }
        UniqueCodeGenerator ug = new UniqueCodeGenerator();
        String xPlainCode = ug.getUniqueCode();
        Invoice invoice = Invoice.builder().invoCode("SMS" + xPlainCode).invoAccId(user.getUsrAccId()).invoResellerId(user.getUsrResellerId()).invoPayerMobileNumber(credit.getSmsPayerMobileNumber()).invoLayer(user.getLayer()).invoCreatedByEmail(user.getEmail()).invoCreatedDate(LocalDateTime.now()).invoDueDate(LocalDateTime.now().plusDays(1)).invoCreatedBy(user.getUsrId()).invoStatus(InvoStatus.PENDING_PAYMENT).invoAmount(credit.getSmsPayAmount()).invoTaxRate(BigDecimal.ZERO).invoAmountAfterTax(credit.getSmsPayAmount()).build();

        BigDecimal amountToLaunch  = credit.getSmsPayAmount();
        if(credit.getSmsLoadingMethod().equalsIgnoreCase("UNITS")){
            Account account = accountService.findByAccId(user.getUsrAccId());
            amountToLaunch = credit.getSmsPayAmount().multiply(account.getAccSmsPrice());
            invoice.setInvoAmount(amountToLaunch);
            invoice.setInvoAmountAfterTax(amountToLaunch); // no tax — after-tax tracks the money amount
        }

        response.setMessage("message", "STK pop for amount " + amountToLaunch + " to code " + invoice.getInvoCode(), response);
        launchSDkMpesa(invoice);
        response.setData("result", invoiceRepository.saveAndFlush(invoice), response);
        return response;
    }

    public boolean checkIfResellerHasAvailableUnits(UUID usrResellerId, BigDecimal unitsToBuy) {
        BigDecimal currentUnits = resellerRepo.findById(usrResellerId).orElseThrow(() -> new RuntimeException("Reseller Does Not Exist")).getRsAllocatableUnit();
        return currentUnits.compareTo(unitsToBuy) >= 0;
    }

    public StandardJsonResponse resellerLoadCredit(CreditInvoDto credit, User user) {
        StandardJsonResponse response = new StandardJsonResponse();
        UniqueCodeGenerator ug = new UniqueCodeGenerator();
        String xPlainCode = ug.getUniqueCode();
        //load collection to weiser
        Invoice invoice = Invoice.builder().invoCode("SMS" + xPlainCode).invoResellerId(user.getUsrResellerId()).invoPayerMobileNumber(credit.getSmsPayerMobileNumber())
                .invoLayer(user.getLayer()).invoCreatedByEmail(user.getEmail())
                .invoCreatedDate(LocalDateTime.now()).invoDueDate(LocalDateTime.now().plusDays(1)).invoCreatedBy(user.getUsrId())
                .invoStatus(InvoStatus.PENDING_PAYMENT).invoAmount(credit.getSmsPayAmount()).invoTaxRate(BigDecimal.ZERO).invoAmountAfterTax(credit.getSmsPayAmount()).build();
        response.setMessage("message", "STK pop for amount " + credit.getSmsPayAmount() + " to code " + invoice.getInvoCode(), response);
        launchSDkResellerSelf(invoice);
        response.setData("result", invoiceRepository.saveAndFlush(invoice), response);
        return response;
    }

    public void launchSDkMpesa(Invoice invoice) {
        StandardJsonResponse response = new StandardJsonResponse();
        try {
            String checkoutRequestId = pushSDKConfigService.popSDkMpesa(invoice.getInvoPayerMobileNumber(), String.valueOf(invoice.getInvoAmount()), invoice.getInvoCode());
            invoice.setInvoCheckoutRequestId(checkoutRequestId);
        } catch (Exception e) {
            invoice.setInvoStatus(InvoStatus.FAILED_TO_POP_SDK);
            invoiceRepository.saveAndFlush(invoice);
            response.setMessage("message", "Failed !!! Launch STK Failed", response);
           log.error("Error Launching STK : {}", e.getMessage());

        }

    }

    public void launchSDkResellerSelf(Invoice invoice) {
        StandardJsonResponse response = new StandardJsonResponse();
        try {
            String checkoutRequestId = pushSDKConfigService.popSDkMpesa(invoice.getInvoPayerMobileNumber(), String.valueOf(invoice.getInvoAmount()),  invoice.getInvoCode());
            invoice.setInvoCheckoutRequestId(checkoutRequestId);
            response.setMessage("message", "Launch STK", response);
        } catch (Exception e) {
            invoice.setInvoStatus(InvoStatus.FAILED_TO_POP_SDK);
            invoiceRepository.saveAndFlush(invoice);
            response.setMessage("message", "Failed !!! Launch STK Failed", response);
            log.error("Error Launching STK Self : {}", e.getMessage());

        }

    }

    public void receivePayment(PaymentDto paymentDto)  throws Exception {

            Payment payment = new Payment();
            BeanUtils.copyProperties(paymentDto, payment);
            payment.setTransAmount(paymentDto.getTransAmount());
            gu.printToJson(payment, "success");
            Invoice invoice = findByInvoCode(payment.getBillRefNumber());

            // Idempotency guard — M-Pesa C2B confirmations are at-least-once. Re-processing a duplicate
            // would re-allocate units (and the historical double-credit bug). Skip if already settled.
            if (invoice.getInvoStatus() == InvoStatus.PAID || paymentService.existsByTransId(paymentDto.getTransId())) {
                log.warn("Duplicate C2B payment ignored — invoCode={} transId={}",
                        payment.getBillRefNumber(), paymentDto.getTransId());
                return;
            }

            invoice.setInvoStatus(InvoStatus.PAID);
            if (paymentDto.getFirstName() != null) {
                invoice.setInvoPayerName(paymentDto.getFirstName().replaceAll(" null", ""));
            }
            invoiceRepository.saveAndFlush(invoice);
            payment.setTransResellerId(invoice.getInvoResellerId());
            paymentService.save(payment);

            Credit credit = Credit.builder().smsAccId(invoice.getInvoAccId()).smsPayAmount(payment.getTransAmount()).smsResellerId(invoice.getInvoResellerId())
                    .smsPaymentRef(paymentDto.getTransId()).smsPaymentId(payment.getId()).build();

        User user = userService.findById(invoice.getInvoCreatedBy());
//            user.setLayer(Layers.ACCOUNT);
        creditService.saveCredit(credit, user);


    }

    /**
     * Process a Safaricom STK push result callback. This is the only signal we get when a top-up is
     * cancelled on the SIM prompt, fails (wrong PIN / insufficient funds / unreachable) or times out,
     * so without it the invoice would sit in PENDING_PAYMENT forever.
     *
     * <p>Matching is by CheckoutRequestID (captured at launch). On success we record the receipt but
     * leave settlement to the C2B confirmation as a fallback we also settle here idempotently, so a
     * successful top-up credits units even if no separate C2B confirmation arrives. The
     * {@code receivePayment} idempotency guard (status==PAID / existsByTransId) prevents double-credit.
     */
    public void handleStkCallback(StkCallbackDto dto) {
        StkCallbackDto.StkCallback cb = dto.callback();
        if (cb == null || cb.getCheckoutRequestID() == null) {
            log.warn("STK callback without CheckoutRequestID — ignored");
            return;
        }
        Invoice invoice = invoiceRepository.findByInvoCheckoutRequestId(cb.getCheckoutRequestID()).orElse(null);
        if (invoice == null) {
            log.warn("STK callback for unknown CheckoutRequestID={} (resultCode={})",
                    cb.getCheckoutRequestID(), cb.getResultCode());
            return;
        }
        if (invoice.getInvoStatus() == InvoStatus.PAID) {
            log.info("STK callback for already-settled invoice {} — no-op", invoice.getInvoCode());
            return;
        }

        Integer resultCode = cb.getResultCode();
        if (resultCode != null && resultCode == 0) {
            Object receipt = dto.metadata("MpesaReceiptNumber");
            Object amount = dto.metadata("Amount");
            Object phone = dto.metadata("PhoneNumber");
            if (receipt != null) invoice.setInvoMpesaReceipt(receipt.toString());
            invoiceRepository.saveAndFlush(invoice);
            log.info("STK success invoice={} receipt={} — settling", invoice.getInvoCode(), receipt);

            // Settle idempotently from the callback so a paid top-up always allocates units.
            try {
                PaymentDto pay = new PaymentDto();
                pay.setBillRefNumber(invoice.getInvoCode());
                pay.setTransId(receipt != null ? receipt.toString() : "STK-" + cb.getCheckoutRequestID());
                pay.setTransAmount(amount != null ? new BigDecimal(amount.toString()) : invoice.getInvoAmount());
                pay.setMsisdn(phone != null ? phone.toString() : invoice.getInvoPayerMobileNumber());
                receivePayment(pay);
            } catch (Exception e) {
                log.error("STK settlement failed for invoice {} : {}", invoice.getInvoCode(), e.getMessage());
            }
            return;
        }

        // ResultCode != 0 — the top-up did not go through.
        // 1032 = "Request cancelled by user"; everything else is a generic failure.
        InvoStatus terminal = (resultCode != null && resultCode == 1032) ? InvoStatus.CANCELLED : InvoStatus.FAILED;
        invoice.setInvoStatus(terminal);
        invoice.setInvoFailureReason(cb.getResultDesc());
        invoiceRepository.saveAndFlush(invoice);
        log.warn("STK {} invoice={} code={} reason={}", terminal, invoice.getInvoCode(),
                resultCode, cb.getResultDesc());
    }

    /**
     * Safety net for STK pushes that never produce any callback (the customer ignores the prompt and
     * it silently lapses). Flips PENDING_PAYMENT invoices past their due date to EXPIRED. Driven by
     * {@link com.spa.smart_gate_springboot.account_setup.invoice.InvoiceExpiryCron}.
     */
    @Transactional
    public int expireStalePending() {
        return invoiceRepository.expireStalePending(LocalDateTime.now());
    }

    private Invoice findByInvoCode(String billRefNumber) {
        return invoiceRepository.findByInvoCode(billRefNumber).orElseThrow(() -> new GlobalExceptionHandler.ResourceNotFoundException("Invo code not found :" + billRefNumber));
    }

    public Invoice findById(UUID id) {
        return invoiceRepository.findById(id).orElseThrow(() -> new RuntimeException("Invoice not found with id :" + id));
    }

    public StandardJsonResponse getAllInvoices(InvoiceFilter filterDto) {
        if (filterDto.getLimit() == 0) filterDto.setLimit(10);
        filterDto.setSortColumn("invo_created_date");
        Pageable pageable = PageRequest.of(filterDto.getStart(), filterDto.getLimit(), Sort.by(filterDto.getSortColumn()).descending());
        if (!TextUtils.isEmpty(filterDto.getInvoCode())) {
            filterDto.setInvoCode("%" + filterDto.getInvoCode() + "%");
        }
        if (!TextUtils.isEmpty(filterDto.getInvoPayerMobileNumber())) {
            filterDto.setInvoPayerMobileNumber("%" + filterDto.getInvoPayerMobileNumber() + "%");
        }
        StandardJsonResponse response = new StandardJsonResponse();
        Page<Invoice> pagedData = invoiceRepository.findInvoiceFiltered(filterDto.getInvoCode(), filterDto.getInvoAccId(), filterDto.getInvoResellerId(), filterDto.getInvoPayerMobileNumber(), filterDto.getInvoStatus(), filterDto.getInvoDate(), pageable);
        List<Invoice> invoices = pagedData.getContent();
        response.setData("result", invoices, response);
        response.setTotal((int) pagedData.getTotalElements());
        return response;

    }

    public StandardJsonResponse getResellerInvoicesPerYearSummary(UUID rsId) {
        StandardJsonResponse response = new StandardJsonResponse();
        List<Object[]> objectList = invoiceRepository.getResellerInvoicesPerYearSummary(rsId);
        List<InvoiceResellerSummary> list = objectList.stream().map(result -> {
            log.info("see result = : {}", Arrays.toString(result));
            gu.printToJson(result, "success");
            return InvoiceResellerSummary.builder().amount((BigDecimal) result[0]).
                    monthName(result[1] + "").monthId((result[2] == null ? 0 : (int) result[2])).build();
        }).toList();


        response.setData("result", list, response);
        response.setTotal(list.size());
        response.setMessage("message", "Ok", response);
        return response;
    }

}




