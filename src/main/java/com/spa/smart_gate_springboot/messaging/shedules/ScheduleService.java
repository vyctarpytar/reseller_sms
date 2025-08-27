package com.spa.smart_gate_springboot.messaging.shedules;

import com.spa.smart_gate_springboot.account_setup.group.ChGroup;
import com.spa.smart_gate_springboot.account_setup.group.ChGroupService;
import com.spa.smart_gate_springboot.account_setup.member.ChMember;
import com.spa.smart_gate_springboot.account_setup.member.MemberService;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.messaging.send_message.MsgQueue;
import com.spa.smart_gate_springboot.messaging.send_message.QueueMsgService;
import com.spa.smart_gate_springboot.messaging.send_message.dtos.GroupMessageDto;
import com.spa.smart_gate_springboot.messaging.send_message.dtos.SingleMessageDto;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@RequiredArgsConstructor
@EnableScheduling
@Slf4j
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final QueueMsgService queueMsgService;
    private final MemberService memberService;
    private final ChGroupService chGroupService;




    public StandardJsonResponse scheduleGroupMessage(UUID grpId, GroupMessageDto grpMessageDto, User user) {
        ChGroup chGroup = chGroupService.getChGroup(grpId);
        Schedule sche = Schedule.builder().schCreatedById(user.getUsrId()).schCreatedByName(user.getEmail()).schAccId(user.getUsrAccId()).schGrpId(grpId).schCreatedOn(LocalDateTime.now()).schMessage(grpMessageDto.getGrpMessage()).schReleaseTime(grpMessageDto.getGrpSendAt()).schSenderid(grpMessageDto.getSenderId())
                .schGroupName(chGroup.getGroupName()).build();
        scheduleRepository.saveAndFlush(sche);

        SingleMessageDto sendSingleSmsDto = SingleMessageDto.builder().mobile(user.getPhoneNumber()).message(grpMessageDto.getGrpMessage()).senderId(grpMessageDto.getSenderId()).build();
        queueMsgService.sendSingleSms(sendSingleSmsDto, user);
        StandardJsonResponse resp = new StandardJsonResponse();
        resp.setMessage("result", sche, resp);
        resp.setMessage("message", "Messages Scheduled For release at (" + grpMessageDto.getGrpSendAt() + "}. Sample Sent to you.", resp);
        return resp;
    }

    public StandardJsonResponse scheduleSingleSmsMultipleNumbers(SingleMessageDto singleMessageDto, User user) {
        String[] phoneNumbers = singleMessageDto.getMobile().split(",");

        for (String phoneNumber : phoneNumbers) {
            Schedule sche = Schedule.builder().schCreatedById(user.getUsrId()).schCreatedByName(user.getEmail()).schPhoneNumber(phoneNumber).schAccId(user.getUsrAccId()).schCreatedOn(LocalDateTime.now()).schMessage(singleMessageDto.getMessage()).schReleaseTime(singleMessageDto.getSendAt()).schSenderid(singleMessageDto.getSenderId()).build();
            scheduleRepository.saveAndFlush(sche);

            SingleMessageDto sendSingleSmsDto = SingleMessageDto.builder().mobile(user.getPhoneNumber()).message(singleMessageDto.getMessage()).senderId(singleMessageDto.getSenderId()).build();
            queueMsgService.sendSingleSms(sendSingleSmsDto, user);
        }

        StandardJsonResponse resp = new StandardJsonResponse();

        resp.setMessage("message", "Messages Scheduled For release at (" + singleMessageDto.getSendAt() + "}. Sample Sent to you.", resp);
        return resp;
    }

    @Scheduled(fixedRate = 5000)
    public void runScheduledMessages() {

            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String formattedDate = now.format(formatter);

            List<Schedule> scheduleList = scheduleRepository.findAllBySchReleaseTimeEqualsAndSchStatus(formattedDate, "PENDING");

            for (Schedule schedule : scheduleList) {
                log.info("running schedule for time {}", schedule.getSchReleaseTime());

                if (schedule.getSchGrpId() != null) {
                    List<ChMember> memberList = memberService.getChMembersByGroupId(schedule.getSchGrpId());
                    for (ChMember m : memberList) {
                        String message = reformatMessage(schedule.getSchMessage(), m);
                        MsgQueue msgQueue = MsgQueue.builder().msgAccId(schedule.getSchAccId()).msgStatus("PENDING_PROCESSING").msgSenderId(schedule.getSchSenderid()).msgMessage(message).msgCreatedDate(new Date()).msgCreatedTime(String.valueOf(LocalDateTime.now())).msgSubMobileNo(m.getChTelephone()).msgCreatedBy(schedule.getSchCreatedById()).msgGroupId(schedule.getSchGrpId()).msgSourceIpAddress(schedule.getSchSourceIp()).build();
                        queueMsgService.publishNewMessage(msgQueue);
                    }

                } else {
                    // send to one mobile number
                    String message = schedule.getSchMessage();
                    MsgQueue msgQueue = MsgQueue.builder().msgAccId(schedule.getSchAccId()).msgStatus("PENDING_PROCESSING").msgSenderId(schedule.getSchSenderid()).msgMessage(message).msgCreatedDate(new Date()).msgCreatedTime(String.valueOf(LocalDateTime.now())).msgSubMobileNo(schedule.getSchPhoneNumber()).msgCreatedBy(schedule.getSchCreatedById()).msgGroupId(schedule.getSchGrpId()).msgSourceIpAddress(schedule.getSchSourceIp()).build();
                    queueMsgService.publishNewMessage(msgQueue);
                }
                schedule.setSchStatus("SENT");
                scheduleRepository.save(schedule);


            }


    }

    public StandardJsonResponse getFilteredSchedules(ScheduleFilterDto filterDto, User user) {

        StandardJsonResponse resp = new StandardJsonResponse();


        if (user.getLayer().equals(Layers.ACCOUNT)) {
            List<Schedule> all = scheduleRepository.findAllBySchAccIdOrderBySchCreatedOn(filterDto.getSchAccId());
            resp.setData("result", all, resp);
            resp.setTotal(all.size());
        }


        return resp;
    }

    @NotNull
    private static String reformatMessage(String message, ChMember m) {
        if (message.contains("@")) {
            // Use a mutable map to handle null values safely
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("@firstName", m.getChFirstName() != null ? m.getChFirstName() : "");
            placeholders.put("@otherNames", m.getChOtherName() != null ? m.getChOtherName().split(" ")[0] : "");
            placeholders.put("@gender", m.getChGenderCode() != null ? m.getChGenderCode() : "");
            placeholders.put("@mobileNumber", m.getChTelephone() != null ? m.getChTelephone() : "");
            placeholders.put("@dateOfBirth", m.getChDob() != null ? String.valueOf(m.getChDob()) : "");
            placeholders.put("@option1", m.getChOption1() != null ? m.getChOption1() : "");
            placeholders.put("@option2", m.getChOption2() != null ? m.getChOption2() : "");
            placeholders.put("@option3", m.getChOption3() != null ? m.getChOption3() : "");
            placeholders.put("@option4", m.getChOption4() != null ? m.getChOption4() : "");

            // Replace placeholders in the message
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace(entry.getKey(), entry.getValue());
            }
        }
        return message;
    }


    public StandardJsonResponse updateSchedule(Schedule schedule, User user) {
        schedule.setSchUpdatedOn(LocalDateTime.now());
        schedule.setSchUpdatedById(user.getUsrId());
        schedule.setSchUpdatedByName(user.getEmail());
        StandardJsonResponse resp = new StandardJsonResponse();
        resp.setMessage("result", scheduleRepository.save(schedule), resp);
        resp.setMessage("message", "Messages Schedule Updated Successfully", resp);
        return resp;
    }

    public StandardJsonResponse disaleSchedule(UUID scheduleId, User user) {
        Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
        if (schedule == null) throw new RuntimeException("Schedule Not Found");
        schedule.setSchStatus("DISABLED");
        schedule.setSchUpdatedOn(LocalDateTime.now());
        schedule.setSchUpdatedById(user.getUsrId());
        schedule.setSchUpdatedByName(user.getEmail());
        StandardJsonResponse resp = new StandardJsonResponse();
        resp.setMessage("result", scheduleRepository.save(schedule), resp);
        resp.setMessage("message", "Messages Schedule Disabled Successfully", resp);
        return resp;
    }
}
