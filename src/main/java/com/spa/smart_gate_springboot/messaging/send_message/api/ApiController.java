package com.spa.smart_gate_springboot.messaging.send_message.api;

import com.fasterxml.jackson.databind.util.BeanUtil;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/sandbox")
@RequiredArgsConstructor
@Slf4j
public class ApiController {

    private final ApiKeyService apiKeyService;


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


}

