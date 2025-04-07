package com.spa.smart_gate_springboot.messaging.send_message;

import com.spa.smart_gate_springboot.MQRes.MQConfig;
import com.spa.smart_gate_springboot.MQRes.RMQPublisher;
import com.spa.smart_gate_springboot.account_setup.reseller.ResellerService;
import com.spa.smart_gate_springboot.messaging.send_message.dtos.SmsDlr;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/public")
@RequiredArgsConstructor
@Slf4j
public class IncomingController {
    private final QueueMsgService queueMsgService;
    private final ResellerService resellerService;
    private final RMQPublisher rmqPublisher;

    @PostMapping("/single-sms")
    public StandardJsonResponse sendSingleSms(@RequestBody MsgQueue msgQueue, HttpServletRequest request) {
        StandardJsonResponse resp = new StandardJsonResponse();
        String clientIp = queueMsgService.getClientIp(request);
        if (clientIp != null && (clientIp.equalsIgnoreCase("102.217.125.32")
                                 || clientIp.equalsIgnoreCase("102.217.125.3")
                                 || clientIp.equalsIgnoreCase("102.217.125.89")
                                 || clientIp.equalsIgnoreCase("102.217.125.10")
                                 || clientIp.equalsIgnoreCase("102.217.125.152")
        )) {
            log.info("Block sending SMS from Test Env : {}\n {}", clientIp, msgQueue.getMsgMessage());
            resp.setMessage("message", "Block sending SMS from Test Env", resp);
            resp.setStatus(HttpStatus.FORBIDDEN.value());
            resp.setSuccess(false);
            return resp;
        }
        String domainOrIp = queueMsgService.getDomainOrIp(request);
        msgQueue.setMsgSourceIpAddress(domainOrIp);
        msgQueue.setMsgCreatedDate(new Date());
        queueMsgService.publishNewMessage(msgQueue);

        resp.setData("result", domainOrIp, resp);
        resp.setMessage("message", "Messages Sent Successfully", resp);
        return resp;
    }

    @GetMapping("/view-logo/{rsId}")
    public ResponseEntity<byte[]> viewImage(@PathVariable @NotNull  UUID rsId) {
        try {
            String fileName = resellerService.findById(rsId).getRsLogo();
            File file = new File(fileName);
            if (!file.exists()) {
                return ResponseEntity.status(404).body(null);
            }

//            Path filePath = Paths.get(uploadDir, imageData.getFileName());
            byte[] image = Files.readAllBytes(file.toPath());
            return ResponseEntity.ok().header("Content-Type", MediaType.IMAGE_PNG_VALUE).body(image);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/health")
    public String health() {
        log.error("Health Check:   {}", new Date());
        return "OK";
    }

    @PostMapping("/dlr")
    public ResponseEntity<String> delivery(@RequestBody SmsDlr smsResponse) {
        log.info("RECEIVED DLR : {}", smsResponse);
        rmqPublisher.publishToOutQueue(smsResponse, MQConfig.INCOMING_SMS_DLR);

        return ResponseEntity.status(HttpStatus.OK).body("SMS DLR Received");
    }

}

