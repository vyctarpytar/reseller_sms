package com.spa.smart_gate_springboot.dashboad;

import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArcRepository;
import com.spa.smart_gate_springboot.messaging.send_message.dtos.FilterDto;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashBoardService {
    private final MsgMessageQueueArcRepository arcRepo;


    public StandardJsonResponse getMainDashboard(FilterDto filterDto) {
        UUID msgAccId = filterDto.getMsgAccId();
        UUID msgSalesUserId = filterDto.getMsgSaleUserId();
        UUID msgResellerId = filterDto.getMsgResellerId();
        Date msgDate = filterDto.getMsgCreatedDate();
        Date msgDateFrom = filterDto.getMsgCreatedFrom();
        Date msgDateTo = filterDto.getMsgCreatedTo();
        if (msgDate == null) msgDate = new Date();

        if (msgDateTo == null) msgDateTo = new Date();
        if (msgDateFrom != null) msgDate = null;


        String msgStatus = filterDto.getMsgStatus();
        String msgSenderName = filterDto.getMsgSenderId();


        DashBoard dash = new DashBoard();
        dash.setMsgTimeSeries(getMsgTimeSeries(msgAccId, msgDate, msgStatus, msgSalesUserId, msgResellerId, msgSenderName, msgDateFrom, msgDateTo));
        dash.setStstusSummary(getMsgStatusStats(msgAccId, msgDate, msgStatus, msgSalesUserId, msgResellerId, msgSenderName, msgDateFrom, msgDateTo));
        StandardJsonResponse response = new StandardJsonResponse();
        response.setData("result", dash, response);
        return response;
    }


    private List<MsgTimeSeries> getMsgTimeSeries(UUID msgAccId, Date msgDate, String msgStatus, UUID msgSalesUserId, UUID msgResellerId, String msgSenderName, Date msgDateFrom, Date msgDateTo) {
        List<Object[]> rawResults = arcRepo.getTimeSeriesDataForToday(msgAccId, msgDate, msgStatus, msgSalesUserId, msgResellerId, msgSenderName, msgDateFrom, msgDateTo);
        return rawResults.stream().map(result -> MsgTimeSeries.builder().msgCreatedDate((String) result[0]).msgStatus((String) result[1]).msgCount((Integer) result[2]).build()).collect(Collectors.toList());
    }

    private List<MsgStatusStat> getMsgStatusStats(UUID msgAccId, Date msgDate, String msgStatus, UUID msgSalesUserId, UUID msgResellerId, String msgSenderName, Date msgDateFrom, Date msgDateTo) {
        List<Object[]> rawResults = arcRepo.getMessageStatusStatForToday(msgAccId, msgDate, msgStatus, msgSalesUserId, msgResellerId, msgSenderName, msgDateFrom, msgDateTo);
        int totalSms = rawResults.stream().mapToInt(rawResult -> (Integer) rawResult[1]).sum();
        return rawResults.stream().map(result -> MsgStatusStat.builder().msgStatus((String) result[0]).msgCount((Integer) result[1]).path("sent-sms").msgPerCent(((Integer) result[1] * 100) / totalSms).build()).collect(Collectors.toList());
    }

}

