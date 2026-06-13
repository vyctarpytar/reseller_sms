package com.spa.smart_gate_springboot.messaging.send_message.api;

import com.spa.smart_gate_springboot.account_setup.invoice.CreditInvoDto;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v2/sandbox")
@RequiredArgsConstructor
@Slf4j
public class ApiController {

    private final ApiKeyService apiKeyService;
    private final SandboxAccountService sandboxAccountService;

    @PostMapping("/single-sms")
    public Map<String,Object> apiSms(@RequestBody @Valid MsgApiDto msgQueue, HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-KEY");
        if(apiKey == null) apiKey = msgQueue.getApiKey();
        if(apiKey == null) throw new RuntimeException("API KEY missing!!!");
        return apiKeyService.sendMessage(msgQueue, apiKey);
    }

    @PostMapping("/bulk-sms")
    public StandardJsonResponse apiBulkSms(@RequestBody @Valid MsgApiBulkDto msgQueueBulk, HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-KEY");
        if(apiKey == null) apiKey = msgQueueBulk.getApiKey();
        if(apiKey == null) throw new RuntimeException("API KEY missing!!!");
        for (String phone : msgQueueBulk.getMsgMobileNos()) {
            MsgApiDto msgApiDto =  new MsgApiDto();
            BeanUtils.copyProperties(msgQueueBulk, msgApiDto);
            msgApiDto.setMsgMobileNo(phone);
            apiKeyService.sendMessage(msgApiDto, apiKey);
        }
        StandardJsonResponse map = new StandardJsonResponse();
        map.setMessage("message", "Bulk SMS sent Successfully", map);
        map.setSuccess(Boolean.TRUE);
        map.setTotal(msgQueueBulk.getMsgMobileNos().length);
        return map;
    }

    /** Current SMS balance (KSh + derived units) for the account this API key belongs to. */
    @GetMapping("/balance")
    public StandardJsonResponse balance(HttpServletRequest request) {
        return sandboxAccountService.getBalance(request.getHeader("X-API-KEY"));
    }

    /**
     * Load SMS credit (in KSh) for this API key's account. Creates a PENDING_PAYMENT invoice and
     * fires the M-Pesa STK push via the existing account-load flow; the returned invoice's invoCode
     * is then polled with {@code GET /invoice-status/{invoiceCode}} until it settles.
     */
    @PostMapping("/load")
    public StandardJsonResponse load(@RequestBody @Valid CreditInvoDto credit, HttpServletRequest request) {
        return sandboxAccountService.loadCredit(request.getHeader("X-API-KEY"), credit);
    }

    /** Poll the status of a load invoice (scoped to this API key's own account). */
    @GetMapping("/invoice-status/{invoiceCode}")
    public StandardJsonResponse invoiceStatus(@PathVariable String invoiceCode, HttpServletRequest request) {
        return sandboxAccountService.invoiceStatus(request.getHeader("X-API-KEY"), invoiceCode);
    }


}

