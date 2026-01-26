package com.spa.smart_gate_springboot.dashboad.annual;

import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.GlobalUtils;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/annual-reports")
@RequiredArgsConstructor
@Slf4j
public class AnnualReportController {
    
    private final AnnualReportService annualReportService;
    private final UserService userService;
    private final GlobalUtils globalUtils;
    
    /**
     * Get quarterly reports with optional filtering
     */
    @PostMapping
    public ResponseEntity<StandardJsonResponse> getQuarterlyReports(
            HttpServletRequest request,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String quarter,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String resellerId,
            @RequestParam(required = false) String reseller_id
    ) {
        
        try {

            if (reseller_id != null) {
                resellerId = reseller_id;
            }

            year = GlobalUtils.CheckNullValues(year);
            quarter = GlobalUtils.CheckNullValues(quarter);
            accountId = GlobalUtils.CheckNullValues(accountId);
            resellerId = GlobalUtils.CheckNullValues(resellerId);

            User currentUser = userService.getCurrentUser(request);
            log.info("User {} requesting quarterly reports with filters - year: {}, quarter: {}, accountId: {}, resellerId: {}", 
                    currentUser.getEmail(), year, quarter, accountId, resellerId);
            
            List<AnnualReportDto> reports = annualReportService.getQuarterlyReports(  StringUtils.isEmpty(year) ? null : Integer.parseInt(year),
                    StringUtils.isEmpty(quarter) ? null : Integer.parseInt(quarter),
                    StringUtils.isEmpty(accountId) ? null: UUID.fromString(accountId),
                    StringUtils.isEmpty(resellerId) ? null : UUID.fromString(resellerId));
            
            StandardJsonResponse response = new StandardJsonResponse();
            response.setData("result", reports, response);
            response.setTotal(reports.size());
            response.setMessage("Quarterly reports retrieved successfully", "success", response);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving quarterly reports", e);
            StandardJsonResponse response = new StandardJsonResponse();
            response.setMessage("Error retrieving quarterly reports: " + e.getMessage(), "error", response);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Export quarterly reports to Excel
     */
    @PostMapping("/export/excel")
    public ResponseEntity<byte[]> exportToExcel(
            HttpServletRequest request,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String quarter,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String resellerId,
            @RequestParam(required = false) String reseller_id)
    {
        
        try {

            if (reseller_id != null) {
                resellerId = reseller_id;
            }

            year = GlobalUtils.CheckNullValues(year);
            quarter = GlobalUtils.CheckNullValues(quarter);
            accountId = GlobalUtils.CheckNullValues(accountId);
            resellerId = GlobalUtils.CheckNullValues(resellerId);

            User currentUser = userService.getCurrentUser(request);
            log.info("User {} requesting Excel export with filters - year: {}, quarter: {}, accountId: {}, resellerId: {}", 
                    currentUser.getEmail(), year, quarter, accountId, resellerId);
            
            byte[] excelData = annualReportService.generateExcelReport(
                    StringUtils.isEmpty(year) ? null : Integer.parseInt(year),
                    StringUtils.isEmpty(quarter) ? null : Integer.parseInt(quarter),
                    StringUtils.isEmpty(accountId) ? null: UUID.fromString(accountId),
                    StringUtils.isEmpty(resellerId) ? null : UUID.fromString(resellerId));
            
            // Generate filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String filename = String.format("quarterly_report_%s.xlsx", timestamp);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            
            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
            
        } catch (IOException e) {
            log.error("Error generating Excel report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Error exporting quarterly reports to Excel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get available years for filtering
     */
    @PostMapping("/years")
    public ResponseEntity<StandardJsonResponse> getAvailableYears(HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            log.debug("User {} requesting available years", currentUser.getEmail());
            
            List<Integer> years = annualReportService.getAvailableYears();
            
            StandardJsonResponse response = new StandardJsonResponse();
            response.setData("result", years, response);
            response.setMessage("Available years retrieved successfully", "success", response);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving available years", e);
            StandardJsonResponse response = new StandardJsonResponse();
            response.setMessage("Error retrieving available years: " + e.getMessage(), "error", response);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    

    /**
     * Get quarterly summary statistics
     */
    @PostMapping("/summary")
    public ResponseEntity<StandardJsonResponse> getQuarterlySummary(
            HttpServletRequest request,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String quarter,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String resellerId,
            @RequestParam(required = false) String reseller_id
    ) {
        
        try {

            if (reseller_id != null) {
                resellerId = reseller_id;
            }

            year = GlobalUtils.CheckNullValues(year);
            quarter = GlobalUtils.CheckNullValues(quarter);
            accountId = GlobalUtils.CheckNullValues(accountId);
            resellerId = GlobalUtils.CheckNullValues(resellerId);


            User currentUser = userService.getCurrentUser(request);
            log.info("User {} requesting quarterly summary for year: {}, quarter: {} , accountId: {} , resellerId: {}",
                    currentUser.getEmail(), year, quarter, accountId, resellerId);


            List<AnnualReportDto> reports = annualReportService.getQuarterlyReports(StringUtils.isEmpty(year) ? null : Integer.parseInt(year), StringUtils.isEmpty(quarter) ? null : Integer.parseInt(quarter),
                    StringUtils.isEmpty(accountId) ? null: UUID.fromString(accountId), StringUtils.isEmpty(resellerId) ? null : UUID.fromString(resellerId));
            
            // Calculate summary statistics
            long totalMessages = reports.stream()
                    .mapToLong(r -> r.getQuarterTotalMessages() != null ? r.getQuarterTotalMessages() : 0L)
                    .sum();
            
            double totalRevenue = reports.stream()
                    .mapToDouble(r -> r.getQuarterTotalRevenue() != null ? r.getQuarterTotalRevenue().doubleValue() : 0.0)
                    .sum();
            
            long totalDelivered = reports.stream()
                    .mapToLong(r -> r.getQuarterDeliveredCount() != null ? r.getQuarterDeliveredCount() : 0L)
                    .sum();
            
            long totalFailed = reports.stream()
                    .mapToLong(r -> r.getQuarterFailedCount() != null ? r.getQuarterFailedCount() : 0L)
                    .sum();
            
            double overallDeliveryRate = totalMessages > 0 ? (double) totalDelivered / totalMessages * 100 : 0.0;



            SummaryDto dtoTotalAccounts = SummaryDto.builder().title("Accounts").value(getFormat(reports.size())).svg("svg53").build();
            SummaryDto dtoTotalMessages = SummaryDto.builder().title("Messages").value(getFormat(totalMessages)).svg("svg54").build();
            SummaryDto dtoTotalRevenue = SummaryDto.builder().title("Revenue").value(getFormat(totalRevenue)).svg("svg55").build();
            SummaryDto dtoTotalDelivered = SummaryDto.builder().title("Delivered").value(getFormat(totalDelivered)).svg("svg56").build();
            SummaryDto dtoTotalFailed = SummaryDto.builder().title("Failed").value(String.valueOf(totalFailed)).svg("svg57").build();
            SummaryDto dtoOverallDeliveryRate = SummaryDto.builder().title("Delivery Rate").value(getFormat(overallDeliveryRate)).svg("svg54").build();

            List<SummaryDto> summaryList = List.of(dtoTotalAccounts, dtoTotalMessages, dtoTotalRevenue, dtoTotalDelivered, dtoTotalFailed, dtoOverallDeliveryRate);

            StandardJsonResponse response = new StandardJsonResponse();
            response.setData("result",summaryList, response);
            response.setMessage("Quarterly summary retrieved successfully", "success", response);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving quarterly summary", e);
            StandardJsonResponse response = new StandardJsonResponse();
            response.setMessage("Error retrieving quarterly summary: " + e.getMessage(), "error", response);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @NotNull
    private static String getFormat(double totalRevenue) {
        BigDecimal bd = new BigDecimal(totalRevenue).setScale(2, RoundingMode.HALF_UP);
      return bd.toString();
    }
}
