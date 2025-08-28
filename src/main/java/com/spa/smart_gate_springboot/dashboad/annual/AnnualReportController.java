package com.spa.smart_gate_springboot.dashboad.annual;

import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/annual-reports")
@RequiredArgsConstructor
@Slf4j
public class AnnualReportController {
    
    private final AnnualReportService annualReportService;
    private final UserService userService;
    
    /**
     * Get quarterly reports with optional filtering
     */
    @PostMapping
    public ResponseEntity<StandardJsonResponse> getQuarterlyReports(
            HttpServletRequest request,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer quarter,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID resellerId) {
        
        try {
            User currentUser = userService.getCurrentUser(request);
            log.info("User {} requesting quarterly reports with filters - year: {}, quarter: {}, accountId: {}, resellerId: {}", 
                    currentUser.getEmail(), year, quarter, accountId, resellerId);
            
            List<AnnualReportDto> reports = annualReportService.getQuarterlyReports(year, quarter, accountId, resellerId);
            
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
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer quarter,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID resellerId) {
        
        try {
            User currentUser = userService.getCurrentUser(request);
            log.info("User {} requesting Excel export with filters - year: {}, quarter: {}, accountId: {}, resellerId: {}", 
                    currentUser.getEmail(), year, quarter, accountId, resellerId);
            
            byte[] excelData = annualReportService.generateExcelReport(year, quarter, accountId, resellerId);
            
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
     * Manually trigger report generation for specific year and quarter
     */
    @PostMapping("/generate")
    public ResponseEntity<StandardJsonResponse> generateReports(
            HttpServletRequest request,
            @RequestParam Integer year,
            @RequestParam Integer quarter) {
        
        try {
            User currentUser = userService.getCurrentUser(request);
            log.info("User {} manually triggering report generation for Q{} {}", 
                    currentUser.getEmail(), quarter, year);
            
            // Validate quarter
            if (quarter < 1 || quarter > 4) {
                StandardJsonResponse response = new StandardJsonResponse();
                response.setMessage("Invalid quarter. Must be between 1 and 4", "error", response);
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate year
            int currentYear = LocalDateTime.now().getYear();
            if (year < 2024 || year > currentYear + 1) {
                StandardJsonResponse response = new StandardJsonResponse();
                response.setMessage("Invalid year. Must be between 2024 and " + (currentYear + 1), "error", response);
                return ResponseEntity.badRequest().body(response);
            }
            
            annualReportService.generateReportsManually(year, quarter);
            
            StandardJsonResponse response = new StandardJsonResponse();
            response.setMessage(String.format("Report generation initiated for Q%d %d", quarter, year), "success", response);
            response.setData("year", year, response);
            response.setData("quarter", quarter, response);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error manually generating reports for Q{} {}", quarter, year, e);
            StandardJsonResponse response = new StandardJsonResponse();
            response.setMessage("Error generating reports: " + e.getMessage(), "error", response);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    

    
    /**
     * Get quarterly summary statistics
     */
    @PostMapping("/summary")
    public ResponseEntity<StandardJsonResponse> getQuarterlySummary(
            HttpServletRequest request,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer quarter) {
        
        try {
            User currentUser = userService.getCurrentUser(request);
            log.debug("User {} requesting quarterly summary for year: {}, quarter: {}", 
                    currentUser.getEmail(), year, quarter);
            
            List<AnnualReportDto> reports = annualReportService.getQuarterlyReports(year, quarter, null, null);
            
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



            SummaryDto dtototalAccounts = SummaryDto.builder().tittle("totalAccounts").value(String.valueOf(reports.size())).svg("account").build();
            SummaryDto dtototalMessages = SummaryDto.builder().tittle("totalMessages").value(String.valueOf(totalMessages)).svg("message").build();
            SummaryDto dtototalRevenue = SummaryDto.builder().tittle("totalRevenue").value(String.format("%.2f", totalRevenue)).svg("dollar").build();
            SummaryDto dtototalDelivered = SummaryDto.builder().tittle("totalDelivered").value(String.valueOf(totalDelivered)).svg("delivered").build();
            SummaryDto dtototalFailed = SummaryDto.builder().tittle("totalFailed").value(String.valueOf(totalFailed)).svg("failed").build();
            SummaryDto dtooverallDeliveryRate = SummaryDto.builder().tittle("overallDeliveryRate").value(String.valueOf(overallDeliveryRate)).svg("deliveryRate").build();

            List<SummaryDto> summaryList = List.of(dtototalAccounts, dtototalMessages, dtototalRevenue, dtototalDelivered, dtototalFailed, dtooverallDeliveryRate);

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
}
