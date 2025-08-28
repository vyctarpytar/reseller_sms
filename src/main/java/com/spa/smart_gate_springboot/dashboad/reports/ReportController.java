package com.spa.smart_gate_springboot.dashboad.reports;

import com.spa.smart_gate_springboot.dashboad.reports.models.FilterRptDto;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.user.Role;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v2/rpt")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;
    private final UserService userService;

    private static void setFilters(FilterRptDto filterDto, User user) {
        if (user.getLayer().equals(Layers.ACCOUNT)) {
            filterDto.setMsgAccId(user.getUsrAccId());
        } else if (user.getRole().equals(Role.SALE)) {
            filterDto.setMsgSaleUserId(user.getUsrId());

        } else if (user.getLayer().equals(Layers.RESELLER)) {
            filterDto.setMsgResellerId(user.getUsrResellerId());
        } else if (user.getLayer().equals(Layers.TOP)) {
            if (user.getUsrId().equals(UUID.fromString("50b0ad9d-7471-4143-8f4b-57838360cb4a"))) { // top synq-Africa
                filterDto.setMsgResellerId(UUID.fromString("c3a1822b-72f3-4176-9b64-093fbf0a8c0d")); // show synq tel
            }
        }
    }

    @PostMapping("account-donut")
    public StandardJsonResponse getSmsSummaryPerAccount(HttpServletRequest request, @RequestBody FilterRptDto filterDto) {
        User user = userService.getCurrentUser(request);
        setFilters(filterDto, user);
        return reportService.getSmsSummaryPerAccount(filterDto);
    }

    @PostMapping("daily-sms-usage")
    public StandardJsonResponse getDailySmsUsage(HttpServletRequest request, @RequestBody FilterRptDto filterDto,@RequestParam(required = false) String reseller_id) {
        if (reseller_id != null) {
            filterDto.setMsgResellerId(UUID.fromString(reseller_id));
        }
        User user = userService.getCurrentUser(request);
        setFilters(filterDto, user);
        return reportService.getDailySmsUsage(filterDto);
    }

    @PostMapping("status-sms-usage")
    public StandardJsonResponse getStatusSmsUsage(HttpServletRequest request, @RequestBody FilterRptDto filterDto,@RequestParam(required = false) String reseller_id) {
        if (reseller_id != null) {
            filterDto.setMsgResellerId(UUID.fromString(reseller_id));
        }
        User user = userService.getCurrentUser(request);
        setFilters(filterDto, user);
        return reportService.getStatusSmsUsage(filterDto);
    }

    @PostMapping("/daily-sms-usage-download-excel")
    public ResponseEntity<byte[]> dailySMSUsageSummary(HttpServletRequest request, @RequestBody FilterRptDto filterDto) {
        User user = userService.getCurrentUser(request);
        setFilters(filterDto, user);

        byte[] excelBytes = reportService.getDailySmsUsageDownloadExcell(filterDto);
        if (excelBytes == null) {
            return ResponseEntity.status(500).body(null);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Sms_list_Excel.xlsx");
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    @PostMapping("/status-sms-summary-download-excel")
    public ResponseEntity<byte[]> statusSmsSummary(HttpServletRequest request, @RequestBody FilterRptDto filterDto) {
        User user = userService.getCurrentUser(request);
        setFilters(filterDto, user);

        byte[] excelBytes = reportService.getStatusSmsUsageDownloadExcell(filterDto);
        if (excelBytes == null) {
            return ResponseEntity.status(500).body(null);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Sms_list_Excel.xlsx");
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }
}
