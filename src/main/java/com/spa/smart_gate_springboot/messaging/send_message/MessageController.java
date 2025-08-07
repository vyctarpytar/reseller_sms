package com.spa.smart_gate_springboot.messaging.send_message;

import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.messaging.send_message.dtos.FilterDto;
import com.spa.smart_gate_springboot.messaging.send_message.dtos.GroupMessageDto;
import com.spa.smart_gate_springboot.messaging.send_message.dtos.SingleMessageDto;
import com.spa.smart_gate_springboot.messaging.shedules.ScheduleService;
import com.spa.smart_gate_springboot.user.Role;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.util.Arrays;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/sms")
@RequiredArgsConstructor
@Slf4j
public class MessageController {
    private final QueueMsgService queueMsgService;
    private final UserService userService;
    private final ScheduleService scheduleService;


    @PostMapping("/group/{grpId}")
    @PreAuthorize("hasAnyRole('CAMPAIGN_ADMIN','ADMIN')")
    public StandardJsonResponse sendSMStoGroup(@PathVariable("grpId") UUID grpId, HttpServletRequest request, @RequestBody @Valid GroupMessageDto grpMessageDto) {
        grpMessageDto.setSourceIpAdd(queueMsgService.getDomainOrIp(request));
        if (TextUtils.isEmpty(grpMessageDto.getGrpMessage())) {
            StandardJsonResponse jsonResponse = new StandardJsonResponse();
            jsonResponse.setMessage("message", "No Message", jsonResponse);
            jsonResponse.setSuccess(false);
            return jsonResponse;
        }

        if (TextUtils.isEmpty(grpMessageDto.getSenderId())) {
            StandardJsonResponse jsonResponse = new StandardJsonResponse();
            jsonResponse.setMessage("message", "No Sender Id", jsonResponse);
            jsonResponse.setSuccess(false);
            return jsonResponse;
        }


        User user = userService.getCurrentUser(request);

        if (!TextUtils.isEmpty(grpMessageDto.getGrpSendAt())) {
            return scheduleService.scheduleGroupMessage(grpId, grpMessageDto, user);
        }
        return queueMsgService.sendSmsToGroup(grpId, grpMessageDto, user);
    }

    @PostMapping("/multi-group")
    @PreAuthorize("hasAnyRole('CAMPAIGN_ADMIN','ADMIN')")
    public StandardJsonResponse sendSMStoMultipleGroup(HttpServletRequest request, @RequestBody @Valid GroupMessageDto grpMessageDto) {
        grpMessageDto.setSourceIpAdd(queueMsgService.getDomainOrIp(request));

        User user = userService.getCurrentUser(request);

        String[] grpIds = grpMessageDto.getGrpIds().split(",");
        for (String grpId : grpIds) {
            if (!TextUtils.isEmpty(grpMessageDto.getGrpSendAt())) {
                scheduleService.scheduleGroupMessage(UUID.fromString(grpId), grpMessageDto, user);
            }else {
                queueMsgService.sendSmsToGroup(UUID.fromString(grpId), grpMessageDto, user);
            }
        }
        StandardJsonResponse resp = new StandardJsonResponse();
        resp.setMessage("message", "Messages Sent to All Groups Successfully", resp);
        return resp;

    }

    @PostMapping("/single-sms")
    @PreAuthorize("hasAnyRole('CAMPAIGN_ADMIN','ADMIN')")
    public StandardJsonResponse sendSingleSms(HttpServletRequest request, @RequestBody SingleMessageDto singleMessageDto) {
        singleMessageDto.setSourceIpAdd(queueMsgService.getClientIp(request));
        if (TextUtils.isEmpty(singleMessageDto.getMessage()) || TextUtils.isEmpty(singleMessageDto.getMobile())) {
            StandardJsonResponse jsonResponse = new StandardJsonResponse();
            jsonResponse.setMessage("message", "No Message or Phone Number", jsonResponse);
            jsonResponse.setSuccess(false);
            return jsonResponse;
        }
        User user = userService.getCurrentUser(request);
        if (singleMessageDto.getMobile().contains(",")) {
            if (!TextUtils.isEmpty(singleMessageDto.getSendAt())) {
                return scheduleService.scheduleSingleSmsMultipleNumbers(singleMessageDto, user);
            } else {
                return queueMsgService.sendSingleSmsMultipleNumbers(singleMessageDto, user);
            }
        }
        return queueMsgService.sendSingleSms(singleMessageDto, user);
    }

    @PostMapping("/upload-csv")
    @PreAuthorize("hasAnyRole('CAMPAIGN_ADMIN','ADMIN')")
    public StandardJsonResponse uploadSmsCsvFile(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (file.isEmpty()) {
            log.info("File is empty !!!!");
            throw new RuntimeException("File is empty");
        }
        User user = userService.getCurrentUser(request);
        return queueMsgService.uploadSmsCsvFile(file, user);
    }

    @PostMapping
    public StandardJsonResponse getAllSmsForAccount(@RequestBody FilterDto filterDto, HttpServletRequest request) {
        User user = userService.getCurrentUser(request);
        if (user.getLayer().equals(Layers.ACCOUNT)) {
            filterDto.setMsgAccId(user.getUsrAccId());
        }
        if (user.getLayer().equals(Layers.RESELLER)) {
            filterDto.setMsgResellerId(user.getUsrResellerId());
        }
        if (user.getRole().equals(Role.SALE)) {
            filterDto.setMsgSaleUserId(user.getUsrId());
        }
        if (user.getLayer().equals(Layers.TOP) && (user.getUsrId().equals(UUID.fromString("50b0ad9d-7471-4143-8f4b-57838360cb4a")))) { // sync TOP
//            filterDto.setMsgResellerId(UUID.fromString("c3a1822b-72f3-4176-9b64-093fbf0a8c0d")); // sync Reseller
            filterDto.setMsgResellerId(user.getUsrId()); // sync Reseller
        }
        return queueMsgService.findByMessagesArcFilters(filterDto);
    }

    @GetMapping("/distinct-statuses")
    public StandardJsonResponse getDistinctMsgStatuses(HttpServletRequest request) {
        User user = userService.getCurrentUser(request);
        return queueMsgService.getDistinctMsgStatuses(user);
    }

    @GetMapping("/placeholders")
    @PreAuthorize("hasAnyRole('CAMPAIGN_ADMIN','ADMIN')")
    public StandardJsonResponse getPlaceholders() {
        StandardJsonResponse resp = new StandardJsonResponse();
        resp.setData("result", Arrays.asList("firstName ", "otherNames ", "mobileNumber ", "gender ", "dateOfBirth ", "option1 ", "option2 ", "option3 ", "option4 "), resp);
        return resp;
    }

    @PostMapping("/download-excel")
    public ResponseEntity<byte[]> downloadExcel(HttpServletRequest request, @RequestBody FilterDto filterDto) {
        User user = userService.getCurrentUser(request);
        if (user.getLayer().equals(Layers.ACCOUNT)) filterDto.setMsgAccId(user.getUsrAccId());
        if (user.getLayer().equals(Layers.RESELLER)) filterDto.setMsgResellerId(user.getUsrResellerId());
        if (user.getRole().equals(Role.SALE)) filterDto.setMsgSaleUserId(user.getUsrId());
        if (user.getLayer().equals(Layers.TOP) && (user.getUsrId().equals(UUID.fromString("50b0ad9d-7471-4143-8f4b-57838360cb4a")))) { // sync TOP
            filterDto.setMsgResellerId(UUID.fromString("c3a1822b-72f3-4176-9b64-093fbf0a8c0d")); // sync Reseller
        }

        byte[] excelBytes = queueMsgService.downloadMsgExcell(filterDto, user);
        if (excelBytes == null) {
            log.error("excel without data");
            return ResponseEntity.status(500).body(null);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Sms_list_Excel.xlsx");
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }
}

