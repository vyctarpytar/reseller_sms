package com.spa.smart_gate_springboot.messaging.send_message;

import com.spa.smart_gate_springboot.MQRes.MQConfig;
import com.spa.smart_gate_springboot.MQRes.RMQPublisher;
import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.account_setup.member.ChMember;
import com.spa.smart_gate_springboot.account_setup.member.MemberService;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.messaging.delivery.MsgDelivery;
import com.spa.smart_gate_springboot.messaging.delivery.MsgDeliveryRepository;
import com.spa.smart_gate_springboot.messaging.send_message.dtos.FilterDto;
import com.spa.smart_gate_springboot.messaging.send_message.dtos.GroupMessageDto;
import com.spa.smart_gate_springboot.messaging.send_message.dtos.SingleMessageDto;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.utils.GlobalUtils;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
@EnableScheduling
public class QueueMsgService {
    private final MemberService chMemberService;
    private final RMQPublisher rmqPublisher;
    private final MsgMessageQueueArcRepository arcRepository;
    private final GlobalUtils globalUtils;
    private final AccountService accountService;
    private final MsgDeliveryRepository msgDeliveryRepository;

    public StandardJsonResponse sendSmsToGroup(UUID grpId, GroupMessageDto groupMessageDto, User user) {
        String grpMessage = groupMessageDto.getGrpMessage();
        List<ChMember> membersList = chMemberService.getChMembersByGroupId(grpId);
//        hello @firstname your balance is @accMsgBal
        //"firstName ", "otherNames ", "mobileNumber ","gender  "dateOfBirth ", "option1 ", "option2 ", "option3 ", "option4 "
        membersList.forEach(m -> {
            String chTelephone = validateMobileNo(m.getChTelephone());

            String message = grpMessage;
            if (message.contains("@")) {
                message = message.replace("@firstName", m.getChFirstName() != null ? m.getChFirstName() : "");
                message = message.replace("@otherNames", m.getChOtherName() != null ? m.getChOtherName().split(" ")[0] : "");
                message = message.replace("@gender", m.getChGenderCode() != null ? m.getChGenderCode() : "");
                message = message.replace("@mobileNumber", m.getChTelephone() != null ? m.getChTelephone() : "");
                message = message.replace("@dateOfBirth", m.getChDob() != null ? String.valueOf(m.getChDob()) : "");
                message = message.replace("@option1", m.getChOption1() != null ? m.getChOption1() : "");
                message = message.replace("@option2", m.getChOption2() != null ? m.getChOption2() : "");
                message = message.replace("@option3", m.getChOption3() != null ? m.getChOption3() : "");
                message = message.replace("@option4", m.getChOption4() != null ? m.getChOption4() : "");
            }

            MsgQueue msgQueue = MsgQueue.builder().msgAccId(user.getUsrAccId()).msgStatus("PENDING_PROCESSING").msgSenderId(groupMessageDto.getSenderId()).msgMessage(message).msgCreatedDate(new Date()).msgCreatedTime(String.valueOf(LocalDateTime.now())).msgSubMobileNo(chTelephone).msgCreatedBy(user.getUsrId()).msgGroupId(grpId).build();

            publishNewMessage(msgQueue);
        });

        StandardJsonResponse resp = new StandardJsonResponse();
        resp.setMessage("result", membersList, resp);
        resp.setMessage("message", "Messages Sent Successfully", resp);
        return resp;
    }

    private String validateMobileNo(String chTelephone) {
        return chTelephone.replace("+", "").replace("-", "");
    }

    public StandardJsonResponse sendSingleSms(SingleMessageDto singleMessageDto, User user) {
        MsgQueue msgQueue = MsgQueue.builder().msgAccId(user.getUsrAccId()).msgStatus("PENDING_PROCESSING").msgSenderId(singleMessageDto.getSenderId()).msgMessage(singleMessageDto.getMessage()).msgCreatedDate(new Date()).msgCreatedTime(String.valueOf(LocalDateTime.now())).msgSubMobileNo(singleMessageDto.getMobile()).msgCreatedBy(user.getUsrId()).build();


        Account acc = accountService.findByAccId(msgQueue.getMsgAccId());
        if (acc.getAccMsgBal().compareTo(BigDecimal.TEN) < 1) {
            msgQueue.setMsgStatus("PENDING_CREDIT");
            msgQueue.setMsgClientDeliveryStatus("PENDING");

            MsgMessageQueueArc arcQueue = new MsgMessageQueueArc();
            BeanUtils.copyProperties(msgQueue, arcQueue);
            arcQueue.setMsgExternalId(msgQueue.getMsgExternalId());
            if (arcQueue.getMsgId() != null) {
                log.warn(" msg id should be null here ---{}", arcQueue.getMsgId());
                arcQueue.setMsgId(null);
            }
            arcQueue.setMsgClientDeliveryStatus("PENDING");
            arcRepository.save(arcQueue);
        } else {
            if (user.getUsrResellerId().equals(UUID.fromString("c3a1822b-72f3-4176-9b64-093fbf0a8c0d"))) {
                log.error(" synq sending sms --> ");
                publishNewMessageSynq(msgQueue);
            } else {
                publishNewMessage(msgQueue);
            }
        }
        StandardJsonResponse resp = new StandardJsonResponse();
        resp.setData("result", msgQueue, resp);
        resp.setMessage("message", "Messages Sent Successfully", resp);
        return resp;
    }


    public StandardJsonResponse sendSingleSmsMultipleNumbers(SingleMessageDto singleMessageDto, User user) {

        String[] phoneNumbers = singleMessageDto.getMobile().split(",");
        for (String phoneNumber : phoneNumbers) {
            MsgQueue msgQueue = MsgQueue.builder().msgAccId(user.getUsrAccId()).msgStatus("PENDING_PROCESSING").msgSenderId(singleMessageDto.getSenderId()).msgMessage(singleMessageDto.getMessage()).msgCreatedDate(new Date()).msgCreatedTime(String.valueOf(LocalDateTime.now())).msgSubMobileNo(phoneNumber).msgCreatedBy(user.getUsrId()).build();

            Account acc = accountService.findByAccId(msgQueue.getMsgAccId());
            if (acc.getAccMsgBal().compareTo(BigDecimal.TEN) < 1) {
                msgQueue.setMsgStatus("PENDING_CREDIT");
                msgQueue.setMsgClientDeliveryStatus("PENDING");

                MsgMessageQueueArc arcQueue = new MsgMessageQueueArc();
                BeanUtils.copyProperties(msgQueue, arcQueue);
                arcQueue.setMsgExternalId(msgQueue.getMsgExternalId());
                if (arcQueue.getMsgId() != null) {
                    log.warn(" msg id should be null here ---{}", arcQueue.getMsgId());
                    arcQueue.setMsgId(null);
                }
                arcQueue.setMsgClientDeliveryStatus("PENDING");
                arcRepository.save(arcQueue);
            } else {
                if (user.getUsrResellerId().equals(UUID.fromString("c3a1822b-72f3-4176-9b64-093fbf0a8c0d"))) {
                    log.error(" synq sending sms --> ");
                    publishNewMessageSynq(msgQueue);
                } else {
                    publishNewMessage(msgQueue);
                }
            }
        }


        StandardJsonResponse resp = new StandardJsonResponse();
        resp.setMessage("message", "Messages Sent Successfully", resp);
        return resp;
    }

    public void publishNewMessageSynq(MsgQueue msgQueue) {
        try {
            rmqPublisher.publishToOutQueue(msgQueue, MQConfig.SYNQ_QUEUE);
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public void publishNewMessage(MsgQueue msgQueue) {
        try {
            rmqPublisher.publishToOutQueue(msgQueue, MQConfig.QUEUE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^\\d{10,15}$");


    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return false; // Reject null or empty numbers
        }
        return PHONE_NUMBER_PATTERN.matcher(phoneNumber).matches();
    }

    public StandardJsonResponse uploadSmsCsvFile(MultipartFile file, User user) {
        StandardJsonResponse response = new StandardJsonResponse();
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                String input = globalUtils.getCellValueAsString(row.getCell(0));
                if (row.getRowNum() == 0) { // Skip header row is is not number

                    boolean isNumber = input.matches("\\d+");
                    if (!isNumber) {
                        log.error("Ignore First Line is Header 1 : {}", input);
                        continue;
                    }
                }

                if (input != null && !TextUtils.isEmpty(input)) {
                boolean isvalid = isValidPhoneNumber(input);
                log.error("Mobile : {} Is Valid  : {}", input, isvalid);
                if(!isvalid) {
                    response.setMessage("message","Invalid Phone Number: " + input,  response);
                    response.setSuccess(false);
                    return response;
                }}


            }

            for (Row row : sheet) {
                String input = globalUtils.getCellValueAsString(row.getCell(0));
                if (row.getRowNum() == 0) { // Skip header row is is not number

                    boolean isNumber = input.matches("\\d+");
                    if (!isNumber) {
                        log.error("Ignore First Line is Header : {}", input);
                        continue;
                    }
                }



                SingleMessageDto sendSingleSmsDto = SingleMessageDto.builder().mobile(input).message(globalUtils.getCellValue(row.getCell(1))).build();
                sendSingleSms(sendSingleSmsDto, user);
            }
            response.setMessage("message", "Sent Successfully", response);
            return response;
        } catch (Exception e) {
            log.error(e.getMessage());
            response.setMessage("message", e.getMessage(), response);
            response.setSuccess(false);
            response.setStatus(500);
            return response;
        }

    }

    public StandardJsonResponse findByMessagesArcFilters(FilterDto filterDto) {
        if (filterDto.getLimit() == 0) filterDto.setLimit(10);
        filterDto.setSortColumn("msg_created_date");
        Pageable pageable = PageRequest.of(filterDto.getStart(), filterDto.getLimit(), Sort.by(filterDto.getSortColumn()).descending());
        if (!TextUtils.isEmpty(filterDto.getMsgSubmobileNo())) {
            filterDto.setMsgSubmobileNo("%" + filterDto.getMsgSubmobileNo() + "%");
        }
        if (!TextUtils.isEmpty(filterDto.getMsgMessage())) {
            filterDto.setMsgMessage("%" + filterDto.getMsgMessage() + "%");
        }

        Page<MsgMessageQueueArc> list = arcRepository.findByMessagesArcFilters(filterDto.getMsgAccId(), filterDto.getMsgResellerId(), filterDto.getMsgGrpId(), filterDto.getMsgCreatedDate(), filterDto.getMsgStatus(), filterDto.getMsgSubmobileNo(), filterDto.getMsgMessage(), filterDto.getMsgSenderId(), pageable);

        StandardJsonResponse resp = new StandardJsonResponse();
        resp.setData("result", list.getContent(), resp);
        resp.setTotal((int) list.getTotalElements());
        return resp;
    }

    public StandardJsonResponse getDistinctMsgStatuses(User user) {
        StandardJsonResponse response = new StandardJsonResponse();
        List<String> msgQueues = new ArrayList<>();
        if (user.getLayer().equals(Layers.ACCOUNT)) {
            msgQueues = arcRepository.findDistinctMsgStatus(user.getUsrAccId(), null);
        } else if (user.getLayer().equals(Layers.RESELLER)) {
            msgQueues = arcRepository.findDistinctMsgStatus(null, user.getUsrResellerId());
        } else if (user.getLayer().equals(Layers.TOP)) {
            msgQueues = arcRepository.findDistinctMsgStatus(null, null);
        }
        response.setData("result", msgQueues, response);
        response.setTotal(msgQueues.size());
        return response;
    }
@Transactional
    public void updateArcDnR(MsgDelivery msgDelivery) {

        MsgMessageQueueArc msgMessageQueueArc = arcRepository.findById(msgDelivery.getMsgdMsgId()).orElse(null);
        if (msgMessageQueueArc != null) {
            msgMessageQueueArc.setMsgStatus(msgDelivery.getMsgdStatus().trim());
            msgMessageQueueArc.setMsgDeliveredDate(new Date());
            msgMessageQueueArc.setMsgClientDeliveryStatus("PENDING");
            msgMessageQueueArc.setMsgRetryCount(0);
            MsgMessageQueueArc msgMessageQueueArc2 =  arcRepository.save(msgMessageQueueArc);
           if(msgMessageQueueArc2.getMsgStatus().equalsIgnoreCase(msgDelivery.getMsgdStatus().trim())) msgDeliveryRepository.delete(msgDelivery);
        }
    }

    public void resendPendingSMSResellerCredit(UUID rsId) {
        Set<UUID> accountList = accountService.findAccountByResellerId(rsId).stream().map(Account::getAccId).collect(Collectors.toSet());
        List<MsgMessageQueueArc> pdRsCredit = arcRepository.getMsgPendingCreditForReseller(accountList, "RS_CREDIT_ISSUE");
        pdRsCredit.forEach(m -> {
            MsgQueue msgQueue = new MsgQueue();
            BeanUtils.copyProperties(m, msgQueue);
            arcRepository.delete(m);
            publishNewMessage(msgQueue);
        });
    }

    public void resendPendingSMSAccountCredit(UUID accId) {
        List<MsgMessageQueueArc> pdRsCredit = arcRepository.getMsgPendingCreditForAccount(accId, "PENDING_CREDIT");
        pdRsCredit.forEach(m -> {
            MsgQueue msgQueue = new MsgQueue();
            BeanUtils.copyProperties(m, msgQueue);
            arcRepository.delete(m);
            publishNewMessage(msgQueue);
        });
    }


    public byte[] downloadMsgExcell(FilterDto filterDto, User user) {
//        var usr = user.getEmail();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sms_list_Excell");

            if (filterDto.getMsgCreatedDate() == null) {
                filterDto.setMsgCreatedDate(new Date());
            }
            if (filterDto.getLimit() == 0) filterDto.setLimit(5000);
            filterDto.setSortColumn("msg_created_date");
            Pageable pageable = PageRequest.of(filterDto.getStart(), filterDto.getLimit(), Sort.by(filterDto.getSortColumn()).descending());
            if (!TextUtils.isEmpty(filterDto.getMsgSubmobileNo())) {
                filterDto.setMsgSubmobileNo("%" + filterDto.getMsgSubmobileNo() + "%");
            }
            if (!TextUtils.isEmpty(filterDto.getMsgMessage())) {
                filterDto.setMsgMessage("%" + filterDto.getMsgMessage() + "%");
            }


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


            Page<MsgMessageQueueArc> pagedData = arcRepository.findByMessagesArcFilters(filterDto.getMsgAccId(), filterDto.getMsgResellerId(), filterDto.getMsgGrpId(), filterDto.getMsgCreatedDate(), filterDto.getMsgStatus(), filterDto.getMsgSubmobileNo(), filterDto.getMsgMessage(), filterDto.getMsgSenderId(), pageable);
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

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public String getDomainOrIp(HttpServletRequest request) {
        // First, try to get the domain (server name)
        String domain = request.getServerName();

        // If the domain is an IP address (IPv4 or IPv6), check if it's a valid domain or fallback to client IP
        if (domain.equals("localhost") || isIpAddress(domain)) {
            // If no valid domain is found, fallback to client IP
            return getClientIp(request);
        }

        return domain;
    }

    public String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            // X-Forwarded-For can contain a comma-separated list of IPs, with the first one being the client's original IP
            ipAddress = ipAddress.split(",")[0].trim();
        } else {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    // Helper method to check if the server name is an IP address
    public boolean isIpAddress(String domain) {
        try {
            // Check if the domain is a valid IP (IPv4 or IPv6)
            InetAddress.getByName(domain);
            return true;
        } catch (UnknownHostException e) {
            return false; // Not an IP, so it's a domain name
        }
    }


//    @Scheduled(fixedRate = 10000)
//    public void updateSenderId() {
//        PageRequest pagesble = PageRequest.of(0, 2000);
//        Page<MsgMessageQueueArc> pagedData = arcRepository.findByMsgSenderIdNameIsNull(pagesble);
//        List<MsgMessageQueueArc> list = pagedData.getContent();
//        log.info(pagedData.getTotalElements()+"------------updating ---");
//        for (int i = 0; i < list.size() ; i++) {
//            MsgMessageQueueArc m = list.get(i);
//            try {
//                if(TextUtils.isEmpty(m.getMsgAccName())) {
//                    Account acc = accountService.findByAccId(m.getMsgAccId());
//                    m.setMsgAccName(acc.getAccName());
//                    MsgShortcodeSetup set = msgShortcodeSetupService.findByShAccId(m.getMsgAccId());
//                    m.setMsgSenderIdName(set.getShCode());
//                    arcRepository.saveAndFlush(m);
//                }
//            } catch (Exception e) {
//            }
//        }
//    }
}

