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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
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

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
@EnableScheduling
public class QueueMsgService {
    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^\\d{10,15}$");
    private final MemberService chMemberService;
    private final RMQPublisher rmqPublisher;
    private final MsgMessageQueueArcRepository arcRepository;
    private final GlobalUtils globalUtils;
    private final AccountService accountService;
    private final MsgDeliveryRepository msgDeliveryRepository;

    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return false; // Reject null or empty numbers
        }
        return PHONE_NUMBER_PATTERN.matcher(phoneNumber).matches();
    }

    public StandardJsonResponse sendSmsToGroup(UUID grpId, GroupMessageDto groupMessageDto, User user) {
        String grpMessage = groupMessageDto.getGrpMessage();
        List<ChMember> membersList = chMemberService.getChMembersByGroupId(grpId);
//        hello @firstname your balance is @accMsgBal
        //"firstName ", "otherNames ", "mobileNumber ","gender  "dateOfBirth ", "option1 ", "option2 ", "option3 ", "option4 "

        log.info("sending message to members : {}", membersList.size());

        for (ChMember m : membersList) {
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
        }

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
                    if (!isvalid) {
                        response.setMessage("message", "Invalid Phone Number: " + input, response);
                        response.setSuccess(false);
                        return response;
                    }
                }


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

        log.info("msg date from: {} to: {} ", filterDto.getMsgCreatedFrom(), filterDto.getMsgCreatedTo());

        Page<MsgMessageQueueArc> list = arcRepository.findByMessagesArcFilters(filterDto.getMsgAccId(), filterDto.getMsgResellerId(), filterDto.getMsgGrpId(), filterDto.getMsgCreatedDate(), filterDto.getMsgStatus(), filterDto.getMsgSubmobileNo(), filterDto.getMsgMessage(), filterDto.getMsgSenderId(), filterDto.getMsgCreatedFrom(), filterDto.getMsgCreatedTo(), pageable);

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
            msgMessageQueueArc.setMsgDeliveredDate(LocalDateTime.now());
            msgMessageQueueArc.setMsgClientDeliveryStatus("PENDING");
            msgMessageQueueArc.setMsgRetryCount(0);
            MsgMessageQueueArc msgMessageQueueArc2 = arcRepository.save(msgMessageQueueArc);
            if (msgMessageQueueArc2.getMsgStatus().equalsIgnoreCase(msgDelivery.getMsgdStatus().trim()))
                msgDeliveryRepository.delete(msgDelivery);
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
        // Use SXSSFWorkbook for better memory management with large datasets
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) { // keep 100 rows in memory, exceeding rows will be flushed to disk
            Sheet sheet = workbook.createSheet("Sms_list_Excell");

            // Set default values
            if (filterDto.getMsgCreatedDate() == null && filterDto.getMsgCreatedFrom() == null) {
                filterDto.setMsgCreatedDate(new Date());
            }
            if (filterDto.getLimit() == 0) filterDto.setLimit(5_000_000); // Use underscore for better readability
            filterDto.setSortColumn("msg_created_date");

            // Optimize query by setting appropriate pagination
            int pageSize = 10_000; // Process in batches of 10,000
            int totalProcessed = 0;
            boolean hasMore = true;
            int currentRow = 1; // Start from row 1 (0 is header)

            // Create header row
            String[] headers = {"Date", "Mobile", "Msg Status", "Client", "Sender Id", "Cost", "Message"};
            int[] columnWidths = {30, 20, 20, 20, 20, 10, 200};
            createHeaderRow(sheet, headers, columnWidths, workbook);

            // Reusable cell styles
            CellStyle dateCellStyle = workbook.createCellStyle();
            dateCellStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss"));

            // Process data in batches
            while (hasMore && totalProcessed < filterDto.getLimit()) {
                Pageable pageable = PageRequest.of(
                        totalProcessed / pageSize,
                        Math.min(pageSize, filterDto.getLimit() - totalProcessed),
                        Sort.by(filterDto.getSortColumn()).descending()
                );

                Page<MsgMessageQueueArc> pagedData = arcRepository.findByMessagesArcFilters(
                        filterDto.getMsgAccId(),
                        filterDto.getMsgResellerId(),
                        filterDto.getMsgGrpId(),
                        filterDto.getMsgCreatedDate(),
                        filterDto.getMsgStatus(),
                        filterDto.getMsgSubmobileNo(),
                        filterDto.getMsgMessage(),
                        filterDto.getMsgSenderId(),
                        filterDto.getMsgCreatedFrom(),
                        filterDto.getMsgCreatedTo(),
                        pageable
                );

//                List<MsgMessageQueueArc> batch = pagedData.getContent();
                List<MsgMessageQueueArc> batch = new ArrayList<>(pagedData.getContent());
                if (batch.isEmpty()) {
                    break;
                }

                // Process current batch
                for (MsgMessageQueueArc m : batch) {
                    Row row = sheet.createRow(currentRow++);
                    addDataRow(row, m, dateCellStyle);
                }


                totalProcessed += batch.size();
                hasMore = pagedData.hasNext();

                // Clear the batch to free memory
                batch.clear();
            }

            if (totalProcessed == 0) {
                createEmptyDataRow(sheet, headers.length);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error generating Excel file", e);
            throw new RuntimeException("Failed to generate Excel file", e);
        }
    }

    private void createHeaderRow(Sheet sheet, String[] headers, int[] columnWidths, Workbook workbook) {
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, columnWidths[i] * 256);
        }
    }

    private void addDataRow(Row row, MsgMessageQueueArc message, CellStyle dateCellStyle) {
        int col = 0;
        Cell dateCell = row.createCell(col++);
        dateCell.setCellValue(message.getMsgCreatedDate());
        dateCell.setCellStyle(dateCellStyle);

        row.createCell(col++).setCellValue(message.getMsgSubMobileNo());
        row.createCell(col++).setCellValue(message.getMsgStatus());
        row.createCell(col++).setCellValue(message.getMsgAccName());
        row.createCell(col++).setCellValue(message.getMsgSenderIdName());
        row.createCell(col++).setCellValue(message.getMsgCostId().doubleValue());
        row.createCell(col).setCellValue(message.getMsgMessage());
    }

    private void createEmptyDataRow(Sheet sheet, int columns) {
        Row row = sheet.createRow(1);
        for (int i = 0; i < columns; i++) {
            row.createCell(i).setCellValue("N/A");
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

