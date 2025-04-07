package com.spa.smart_gate_springboot.account_setup.invoice;

import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.account_setup.credit.CrStatus;
import com.spa.smart_gate_springboot.account_setup.credit.Credit;
import com.spa.smart_gate_springboot.account_setup.credit.CreditService;
import com.spa.smart_gate_springboot.account_setup.reseller.ResellerRepo;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.ndovuPay.NdovupayService;
import com.spa.smart_gate_springboot.payment.Payment;
import com.spa.smart_gate_springboot.payment.PaymentDto;
import com.spa.smart_gate_springboot.payment.PaymentService;
import com.spa.smart_gate_springboot.payment.ThirdPartyResponse;
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
    private final NdovupayService ndovupayService;
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
            log.info("Failed !! Reseller toload allocatable Units for " + user.getUsrResellerId());
            response.setMessage("message", "Failed !! Contact Service Provider to Load Credit", response);
            response.setStatus(HttpStatus.SC_EXPECTATION_FAILED);
            response.setSuccess(false);
            return response;
        }
        UniqueCodeGenerator ug = new UniqueCodeGenerator();
        String xPlainCode = ug.getUniqueCode();
        Invoice invoice = Invoice.builder().invoCode("SMS" + xPlainCode).invoAccId(user.getUsrAccId()).invoResellerId(user.getUsrResellerId()).invoPayerMobileNumber(credit.getSmsPayerMobileNumber()).invoLayer(user.getLayer()).invoCreatedByEmail(user.getEmail()).invoCreatedDate(LocalDateTime.now()).invoDueDate(LocalDateTime.now().plusDays(1)).invoCreatedBy(user.getUsrId()).invoStatus(InvoStatus.PENDING_PAYMENT).invoAmount(credit.getSmsPayAmount()).invoTaxRate(BigDecimal.valueOf(0.16)).invoAmountAfterTax(new BigDecimal("1.16").multiply(credit.getSmsPayAmount())).build();

        BigDecimal amountToLaunch  = credit.getSmsPayAmount();
        if(credit.getSmsLoadingMethod().equalsIgnoreCase("UNITS")){
            Account account = accountService.findByAccId(user.getUsrAccId());
            amountToLaunch = credit.getSmsPayAmount().multiply(account.getAccSmsPrice());
            invoice.setInvoAmount(amountToLaunch);
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
                .invoStatus(InvoStatus.PENDING_PAYMENT).invoAmount(credit.getSmsPayAmount()).invoTaxRate(BigDecimal.valueOf(0.16)).invoAmountAfterTax(new BigDecimal("1.16").multiply(credit.getSmsPayAmount())).build();
        response.setMessage("message", "STK pop for amount " + credit.getSmsPayAmount() + " to code " + invoice.getInvoCode(), response);
        launchSDkResellerSelf(invoice);
        response.setData("result", invoiceRepository.saveAndFlush(invoice), response);
        return response;
    }

    public void launchSDkMpesa(Invoice invoice) {
        StandardJsonResponse response = new StandardJsonResponse();
        try {
            String collectWalletCde = ndovupayService.getCollectWalletCode(invoice.getInvoResellerId());
            var res = pushSDKConfigService.popSDkMpesa(invoice.getInvoPayerMobileNumber(), String.valueOf(invoice.getInvoAmount()), collectWalletCde + "-" + invoice.getInvoCode());
           log.info("pop sdk response : {}", res);
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

            UUID topId = resellerRepo.findById(invoice.getInvoResellerId()).get().getRsCreatedBy();

            String collectWalletCde = ndovupayService.getCollectWalletCode(topId);
            pushSDKConfigService.popSDkMpesa(invoice.getInvoPayerMobileNumber(), String.valueOf(invoice.getInvoAmount()), collectWalletCde + "-" + invoice.getInvoCode());
            response.setMessage("message", "Launch STK", response);
        } catch (Exception e) {
            invoice.setInvoStatus(InvoStatus.FAILED_TO_POP_SDK);
            invoiceRepository.saveAndFlush(invoice);
            response.setMessage("message", "Failed !!! Launch STK Failed", response);
            log.error("Error Launching STK Self : {}", e.getMessage());

        }

    }

    public ThirdPartyResponse receivePayment(PaymentDto paymentDto) {

        try {
            Payment payment = new Payment();
            BeanUtils.copyProperties(paymentDto, payment);
            gu.printToJson(payment, "success");
            Invoice invoice = findByInvoCode(payment.getInvoiceNumber());
            invoice.setInvoStatus(InvoStatus.PAID);
            invoice.setInvoPayerName(paymentDto.getKycName().replaceAll(" null", ""));
            invoiceRepository.saveAndFlush(invoice);
            payment.setTransResellerId(invoice.getInvoResellerId());
            paymentService.save(payment);

            Credit credit = Credit.builder().smsAccId(invoice.getInvoAccId()).smsPayAmount(payment.getTransAmount()).smsResellerId(invoice.getInvoResellerId())
                    .smsPaymentRef(paymentDto.getTransId()).smsPaymentId(payment.getId()).build();


            if (invoice.getInvoCreatedByEmail().equalsIgnoreCase("MAJIBYTE_LOGGED_IN_USER")) {
                creditService.saveCreditMajiByte(credit);
            } else {

                User user = userService.findById(invoice.getInvoCreatedBy());
//            user.setLayer(Layers.ACCOUNT);
                creditService.saveCredit(credit, user);
            }
            return ThirdPartyResponse.builder().resultCode(String.valueOf(HttpStatus.SC_OK)).resultDesc("Payment Received").build();
        } catch (Exception e) {
            e.printStackTrace();
            return ThirdPartyResponse.builder().resultCode(String.valueOf(HttpStatus.SC_INTERNAL_SERVER_ERROR)).resultDesc("Failed!!!! Backend Error").build();
        }

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

    public StandardJsonResponse markAsPaidCredit(UUID invoId, User user, InvoPaidDto invoPaidDto) {
        StandardJsonResponse response = new StandardJsonResponse();
        Invoice invoice = findById(invoId);
        invoice.setInvoStatus(InvoStatus.valueOf(invoPaidDto.getInvoStatus()));
        invoice.setInvoMarkedPaidByEmail(user.getEmail());
        invoice.setInvoMarkedPaidById(user.getUsrId());
        invoice.setInvoMarkedPaidDate(LocalDateTime.now());
        invoice.setInvoMarkedPaidReference(invoPaidDto.getInvoMarkedPaidReference());
        invoice.setInvoMarkedPaidValueDate(invoPaidDto.getInvoMarkedPaidValueDate());
        invoice.setInvoMarkedPaidAmount(invoPaidDto.getInvoMarkedPaidAmount());
        response.setData("result", invoiceRepository.saveAndFlush(invoice), response);
        response.setMessage("message", "Marked as Paid", response);
        return response;
    }

    public StandardJsonResponse getResellerInvoicesPerYearSummary(UUID rsId) {
        StandardJsonResponse response = new StandardJsonResponse();
        List<Object[]> objectList = invoiceRepository.getResellerInvoicesPerYearSummary(rsId);
        List<InvoiceResellerSummary> list = objectList.stream().map(result -> {
            log.info("see result = : " + Arrays.toString(result));
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




