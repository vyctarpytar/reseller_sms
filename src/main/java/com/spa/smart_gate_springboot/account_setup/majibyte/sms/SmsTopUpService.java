package com.spa.smart_gate_springboot.account_setup.majibyte.sms;

import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.account_setup.credit.CrStatus;
import com.spa.smart_gate_springboot.account_setup.credit.Credit;
import com.spa.smart_gate_springboot.account_setup.invoice.InvoStatus;
import com.spa.smart_gate_springboot.account_setup.invoice.Invoice;
import com.spa.smart_gate_springboot.account_setup.invoice.InvoiceRepository;
import com.spa.smart_gate_springboot.account_setup.invoice.InvoiceService;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.utils.GlobalUtils;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import com.spa.smart_gate_springboot.utils.UniqueCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class SmsTopUpService{

    private final AccountService accountService;
    private final GlobalUtils gu;
    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;

    public StandardJsonResponse majiByteLoadAccount(TopUpDto credit) {


        Account account = accountService.findByAccId(UUID.fromString(credit.getSmsAccId()));
        BigDecimal accSmsPrice = account.getAccSmsPrice();
        BigDecimal unitsToBuy = gu.getDivide(credit.getSmsPayAmount(), accSmsPrice);


        boolean canBuySms = invoiceService.checkIfResellerHasAvailableUnits(account.getAccResellerId(),unitsToBuy);
        StandardJsonResponse response = new StandardJsonResponse();
        if (!canBuySms) {
            log.info("Failed !! Reseller toload allocatable Units for : {} ", account.getAccResellerId());
            response.setMessage("message", "Failed !! Contact Service Provider to Load Credit", response);
            response.setStatus(HttpStatus.SC_EXPECTATION_FAILED);
            response.setSuccess(false);
            return response;
        }
        UniqueCodeGenerator ug = new UniqueCodeGenerator();
        String xPlainCode = ug.getUniqueCode();
        Invoice invoice = Invoice.builder().invoCode("SMS" + xPlainCode).invoAccId(account.getAccId()).invoResellerId(account.getAccResellerId()).invoPayerMobileNumber(credit.getSmsPayerMobileNumber())
                .invoLayer(Layers.ACCOUNT).invoCreatedByEmail("MAJIBYTE_LOGGED_IN_USER").invoCreatedDate(LocalDateTime.now()).invoDueDate(LocalDateTime.now().plusDays(1))
                .invoCreatedBy(null).invoStatus(InvoStatus.PENDING_PAYMENT).invoAmount(credit.getSmsPayAmount()).invoTaxRate(BigDecimal.valueOf(0.16))
                .invoAmountAfterTax(new BigDecimal("1.16").multiply(credit.getSmsPayAmount())).build();

        BigDecimal amountToLaunch  = credit.getSmsPayAmount();

        response.setMessage("message", "STK pop for amount " + amountToLaunch + " to code " + invoice.getInvoCode(), response);
        invoiceService.launchSDkMpesa(invoice);
        response.setData("result", invoiceRepository.saveAndFlush(invoice), response);
        return response;
    }



}
