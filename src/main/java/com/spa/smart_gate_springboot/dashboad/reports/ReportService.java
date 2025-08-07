package com.spa.smart_gate_springboot.dashboad.reports;

import com.spa.smart_gate_springboot.dashboad.reports.models.*;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {
    private final ReportRepository arcRepo;

    private static void setReportRows(Row row, Object[] m) {
        row.createCell(0).setCellValue(m[0] + "");
        row.createCell(1).setCellValue((Integer) m[1]);
        row.createCell(2).setCellValue(Double.parseDouble(m[2] + ""));
    }

    public StandardJsonResponse getSmsSummaryPerAccount(FilterRptDto filterDto) {
        UUID msgAccId = filterDto.getMsgAccId();
        UUID msgSalesUserId = filterDto.getMsgSaleUserId();
        UUID msgResellerId = filterDto.getMsgResellerId();
        Date msgDate = filterDto.getMsgCreatedDate();
        Date msgDateFrom = filterDto.getMsgDateFrom();
        Date msgDateTo = filterDto.getMsgDateTo();
        if (msgDate == null) msgDate = new Date();

        if (msgDateTo == null) msgDateTo = new Date();
        if (msgDateFrom != null) msgDate = null;

        String msgStatus = filterDto.getMsgStatus();
        String msgSenderName = filterDto.getMsgSenderId();

        AccountDonought accountDonought = new AccountDonought();
        accountDonought.setAccStatList(getSmsSummaryPerAccount(msgAccId, msgDate, msgStatus, msgSalesUserId, msgResellerId, msgSenderName, msgDateFrom, msgDateTo));
        StandardJsonResponse response = new StandardJsonResponse();
        response.setData("result", accountDonought, response);
        return response;
    }

    private List<MsgAccStat> getSmsSummaryPerAccount(UUID msgAccId, Date msgDate, String msgStatus, UUID msgSalesUserId, UUID msgResellerId, String msgSenderName, Date msgDateFrom, Date msgDateTo) {
        List<Object[]> rawResults = arcRepo.getSmsSummaryPerAccount(msgAccId, msgDate, msgStatus, msgSalesUserId, msgResellerId, msgSenderName, msgDateFrom, msgDateTo);
        int totalSms = rawResults.stream().mapToInt(rawResult -> (Integer) rawResult[1]).sum();
        return rawResults.stream().map(result -> MsgAccStat.builder().msgAccName((String) result[0]).msgCount((Integer) result[1]).path("sent-sms").msgPerCent(((Integer) result[1] * 100) / totalSms).build()).collect(Collectors.toList());
    }

    public StandardJsonResponse getDailySmsUsage(FilterRptDto filterDto) {
        if (filterDto.getLimit() == 0) filterDto.setLimit(10);
        UUID msgAccId = filterDto.getMsgAccId();
        UUID msgSaleId = filterDto.getMsgSaleUserId();
        UUID msgResellerId = filterDto.getMsgResellerId();
        Date msgDate = filterDto.getMsgCreatedDate();
        Date msgDateFrom = filterDto.getMsgDateFrom();
        Date msgDateTo = filterDto.getMsgDateTo();
        if (msgDate == null) msgDate = new Date();
        if (msgDateTo == null) msgDateTo = new Date();
        if (msgDateFrom != null) msgDate = null;

        filterDto.setSortColumn("msg_created_date");
        Pageable pageable = PageRequest.of(filterDto.getStart(), filterDto.getLimit(), Sort.by(filterDto.getSortColumn()).descending());
        Page<Object[]> results = arcRepo.getDailySmsSummary(msgAccId, msgDate, msgDateFrom, msgDateTo, msgSaleId, msgResellerId, pageable);

        List<DailyMessageSumDto> statusSummaryDtos = results.getContent().stream().map(result -> DailyMessageSumDto.builder().createdDate((Date) result[0]).noOfMessages((Integer) result[1]).credit((BigDecimal) result[2]).build()).toList();

        StandardJsonResponse response = new StandardJsonResponse();
        response.setData("result", statusSummaryDtos, response);
        response.setTotal((int) results.getTotalElements());
        return response;
    }

    public StandardJsonResponse getStatusSmsUsage(FilterRptDto filterDto) {
        if (filterDto.getLimit() == 0) filterDto.setLimit(30);
        UUID msgAccId = filterDto.getMsgAccId();
        UUID msgSaleId = filterDto.getMsgSaleUserId();
        UUID msgResellerId = filterDto.getMsgResellerId();
        Date msgDate = filterDto.getMsgCreatedDate();
        Date msgDateFrom = filterDto.getMsgDateFrom();
        Date msgDateTo = filterDto.getMsgDateTo();
        if (msgDate == null) msgDate = new Date();
        if (msgDateTo == null) msgDateTo = new Date();
        if (msgDateFrom != null) msgDate = null;

        filterDto.setSortColumn("msg_status");
        Pageable pageable = PageRequest.of(filterDto.getStart(), filterDto.getLimit(), Sort.by(filterDto.getSortColumn()).descending());

        Page<Object[]> results = arcRepo.getStatusSmsSummary(msgAccId, msgDate, msgDateFrom, msgDateTo, msgSaleId, msgResellerId, pageable);
        List<StatusSummaryDto> statusSummaryDtos = results.getContent().stream().map(result -> StatusSummaryDto.builder().msgStatus((String) result[0]).noOfMessages((Integer) result[1]).credit((BigDecimal) result[2]).build()).toList();
        StandardJsonResponse response = new StandardJsonResponse();
        response.setData("result", statusSummaryDtos, response);
        response.setTotal((int) results.getTotalElements());
        return response;
    }

    public byte[] getDailySmsUsageDownloadExcell(FilterRptDto filterDto) {

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sms_Daily_Usage");

            if (filterDto.getLimit() == 0) filterDto.setLimit(10000000);
            UUID msgAccId = filterDto.getMsgAccId();
            UUID msgSaleId = filterDto.getMsgSaleUserId();
            UUID msgResellerId = filterDto.getMsgResellerId();
            Date msgDate = filterDto.getMsgCreatedDate();
            Date msgDateFrom = filterDto.getMsgDateFrom();
            Date msgDateTo = filterDto.getMsgDateTo();
            if (msgDate == null) msgDate = new Date();
            if (msgDateTo == null) msgDateTo = new Date();
            if (msgDateFrom != null) msgDate = null;

            filterDto.setSortColumn("msg_created_date");
            Pageable pageable = PageRequest.of(filterDto.getStart(), filterDto.getLimit(), Sort.by(filterDto.getSortColumn()).descending());
            Page<Object[]> results = arcRepo.getDailySmsSummary(msgAccId, msgDate, msgDateFrom, msgDateTo, msgSaleId, msgResellerId, pageable);

            // Create header row and style it
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setLocked(true);

            String[] headers = {"Date", "No Of Messages", "Cost"};
            int[] columnWidths = {30, 30, 30}; // Set column widths

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, columnWidths[i] * 256); // Set column width
            }

            if (results.getTotalElements() > 0) {
                for (int i = 0; i < results.getTotalElements(); i++) {
                    // Adding a sample row of data
                    var m = results.getContent().get(i);
                    Row row = sheet.createRow(i + 1); // Start from the second row
                    setReportRows(row, m);


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
            log.error("Error downloading report daily : " + e);
            return null;
        }
    }

    public byte[] getStatusSmsUsageDownloadExcell(FilterRptDto filterDto) {

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sms_Daily_Usage");

            if (filterDto.getLimit() == 0) filterDto.setLimit(10000000);
            UUID msgAccId = filterDto.getMsgAccId();
            UUID msgSaleId = filterDto.getMsgSaleUserId();
            UUID msgResellerId = filterDto.getMsgResellerId();
            Date msgDate = filterDto.getMsgCreatedDate();
            Date msgDateFrom = filterDto.getMsgDateFrom();
            Date msgDateTo = filterDto.getMsgDateTo();
            if (msgDate == null) msgDate = new Date();
            if (msgDateTo == null) msgDateTo = new Date();
            if (msgDateFrom != null) msgDate = null;

            filterDto.setSortColumn("msg_status");
            Pageable pageable = PageRequest.of(filterDto.getStart(), filterDto.getLimit(), Sort.by(filterDto.getSortColumn()).descending());
            Page<Object[]> results = arcRepo.getStatusSmsSummary(msgAccId, msgDate, msgDateFrom, msgDateTo, msgSaleId, msgResellerId, pageable);

            // Create header row and style it
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setLocked(true);

            String[] headers = {"Status", "No Of Messages", "Cost"};
            int[] columnWidths = {30, 30, 30}; // Set column widths

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, columnWidths[i] * 256); // Set column width
            }

            if (results.getTotalElements() > 0) {
                for (int i = 0; i < results.getTotalElements(); i++) {
                    // Adding a sample row of data
                    var m = results.getContent().get(i);
                    Row row = sheet.createRow(i + 1); // Start from the second row
                    setReportRows(row, m);


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
            log.error("Error downloading report : " + e);
            return null;
        }
    }
}
