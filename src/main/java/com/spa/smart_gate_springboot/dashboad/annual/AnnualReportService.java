package com.spa.smart_gate_springboot.dashboad.annual;

import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountRepository;
import com.spa.smart_gate_springboot.account_setup.reseller.Reseller;
import com.spa.smart_gate_springboot.account_setup.reseller.ResellerRepo;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnualReportService {

    private final AnnualReportRepository annualReportRepository;
    private final AccountRepository accountRepository;
    private final ResellerRepo resellerRepo;
    private final EntityManager entityManager;

    /**
     * Scheduled method to generate quarterly reports every hour
     */
    @Scheduled(fixedRate = 3600000) // Run every hour (3600000 ms)
    @PostConstruct
    @Transactional
    public void generateQuarterlyReportsScheduled() {
        log.info("Starting scheduled quarterly report generation at: {}", LocalDateTime.now());

        try {
            int currentYear = LocalDateTime.now().getYear();
            int currentQuarter = getCurrentQuarter();

            generateQuarterlyReports(currentYear, currentQuarter);

            log.info("Completed scheduled quarterly report generation for Q{} {}", currentQuarter, currentYear);
        } catch (Exception e) {
            log.error("Error during scheduled quarterly report generation", e);
        }
    }

    /**
     * Generate quarterly reports for all accounts
     */
    @Transactional
    public void generateQuarterlyReports(int year, int quarter) {
        List<Account> allAccounts = accountRepository.findAll();

        // Process reports in parallel using CompletableFuture
        List<CompletableFuture<Void>> reportFutures = allAccounts.stream().map(account -> CompletableFuture.runAsync(() -> {
            try {
                generateReportForAccount(account, year, quarter);
            } catch (Exception e) {
                log.error("Error generating report for account: {}", account.getAccId(), e);
            }
        })).toList();

        // Wait for all reports to complete
        CompletableFuture<Void> allReports = CompletableFuture.allOf(reportFutures.toArray(new CompletableFuture[0]));

        try {
            allReports.join();
            log.info("Generated quarterly reports for {} accounts", allAccounts.size());
        } catch (Exception e) {
            log.error("Error during parallel report generation", e);
            throw new RuntimeException("Failed to generate some quarterly reports", e);
        }
    }

    /**
     * Generate report for a specific account
     */
    private void generateReportForAccount(Account account, int year, int quarter) {
        // Check if report already exists
        Optional<AnnualReport> existingReport = annualReportRepository.findByYearAndQuarterAndAccountId(year, quarter, account.getAccId());

        AnnualReport report;
        if (existingReport.isPresent()) {
            report = existingReport.get();
            log.debug("Updating existing report for account: {}", account.getAccName());
        } else {
            // Get reseller name if reseller ID exists
            String resellerName = null;
            if (account.getAccResellerId() != null) {
                Optional<Reseller> reseller = resellerRepo.findById(account.getAccResellerId());
                resellerName = reseller.map(Reseller::getRsCompanyName).orElse(null);
            }

            report = AnnualReport.builder()
                    .validityPeriod(account.getAccLicenceValidity())
                    .year(year).quarter(quarter).accountId(account.getAccId()).accountName(account.getAccName()).resellerId(account.getAccResellerId()).resellerName(resellerName).status("PROCESSING").build();
            log.debug("Creating new report for account: {}", account.getAccName());
        }

        try {
            // Get quarter months
            int[] months = getQuarterMonths(quarter);

            // Calculate monthly data
            calculateMonthlyData(report, account.getAccId(), year, months);

            // Calculate quarter totals
            calculateQuarterTotals(report);

            // Calculate additional metrics
            calculateAdditionalMetrics(report, account.getAccId(), year, quarter);

            // Get sender ID information
            setSenderIdInformation(report, account.getAccId(), year, getQuarterMonths(quarter));

            report.setStatus("COMPLETED");
            annualReportRepository.save(report);

        } catch (Exception e) {
            report.setStatus("FAILED");
            annualReportRepository.save(report);
            throw new RuntimeException("Failed to generate report for account: " + account.getAccName(), e);
        }
    }

    /**
     * Calculate monthly data for the quarter
     */
    private void calculateMonthlyData(AnnualReport report, UUID accountId, int year, int[] months) {
        for (int i = 0; i < months.length; i++) {
            int month = months[i];

            // Query message data for the month
            String sql = """
                    SELECT 
                        COUNT(*) as message_count,
                        SUM(CASE WHEN msg_status in ('DeliveredToTerminal','PENDING_DELIVERY','SENT') THEN 1 ELSE 0 END) as delivered_count,
                        SUM(CASE WHEN msg_status  not in ('DeliveredToTerminal','PENDING_DELIVERY','SENT') THEN 1 ELSE 0 END) as failed_count
                    FROM msg.message_queue_arc 
                    WHERE msg_acc_id = ?1 
                    AND EXTRACT(YEAR FROM msg_created_date) = ?2 
                    AND EXTRACT(MONTH FROM msg_created_date) = ?3
                    """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, accountId);
            query.setParameter(2, year);
            query.setParameter(3, month);

            Object[] result = (Object[]) query.getSingleResult();

            Long messageCount = result[0] != null ? ((Number) result[0]).longValue() : 0L;
            Long deliveredCount = result[1] != null ? ((Number) result[1]).longValue() : 0L;
            Long failedCount = result[2] != null ? ((Number) result[2]).longValue() : 0L;

            // Calculate revenue (assuming account has SMS price)
            BigDecimal revenue = calculateMonthlyRevenue(accountId, messageCount);

            // Set monthly data based on position in quarter
            switch (i) {
                case 0:
                    report.setMonth1MessageCount(messageCount);
                    report.setMonth1Revenue(revenue);
                    report.setMonth1DeliveredCount(deliveredCount);
                    report.setMonth1FailedCount(failedCount);
                    break;
                case 1:
                    report.setMonth2MessageCount(messageCount);
                    report.setMonth2Revenue(revenue);
                    report.setMonth2DeliveredCount(deliveredCount);
                    report.setMonth2FailedCount(failedCount);
                    break;
                case 2:
                    report.setMonth3MessageCount(messageCount);
                    report.setMonth3Revenue(revenue);
                    report.setMonth3DeliveredCount(deliveredCount);
                    report.setMonth3FailedCount(failedCount);
                    break;
            }

        }
    }

    /**
     * Calculate quarter totals from monthly data
     */
    private void calculateQuarterTotals(AnnualReport report) {
        // Sum up monthly totals
        long totalMessages = (report.getMonth1MessageCount() != null ? report.getMonth1MessageCount() : 0L) + (report.getMonth2MessageCount() != null ? report.getMonth2MessageCount() : 0L) + (report.getMonth3MessageCount() != null ? report.getMonth3MessageCount() : 0L);

        BigDecimal totalRevenue = (report.getMonth1Revenue() != null ? report.getMonth1Revenue() : BigDecimal.ZERO).add(report.getMonth2Revenue() != null ? report.getMonth2Revenue() : BigDecimal.ZERO).add(report.getMonth3Revenue() != null ? report.getMonth3Revenue() : BigDecimal.ZERO);

        long totalDelivered = (report.getMonth1DeliveredCount() != null ? report.getMonth1DeliveredCount() : 0L) + (report.getMonth2DeliveredCount() != null ? report.getMonth2DeliveredCount() : 0L) + (report.getMonth3DeliveredCount() != null ? report.getMonth3DeliveredCount() : 0L);

        long totalFailed = (report.getMonth1FailedCount() != null ? report.getMonth1FailedCount() : 0L) + (report.getMonth2FailedCount() != null ? report.getMonth2FailedCount() : 0L) + (report.getMonth3FailedCount() != null ? report.getMonth3FailedCount() : 0L);

        // Calculate delivery rate
        BigDecimal deliveryRate = BigDecimal.ZERO;
        if (totalMessages > 0) {
            deliveryRate = BigDecimal.valueOf(totalDelivered).divide(BigDecimal.valueOf(totalMessages), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }

        report.setQuarterTotalMessages(totalMessages);
        report.setQuarterTotalRevenue(totalRevenue);
        report.setQuarterDeliveredCount(totalDelivered);
        report.setQuarterFailedCount(totalFailed);
        report.setQuarterDeliveryRate(deliveryRate

        );
    }

    /**
     * Calculate additional metrics
     */
    private void calculateAdditionalMetrics(AnnualReport report, UUID accountId, int year, int quarter) {
        // Calculate average message cost
        if (report.getQuarterTotalMessages() != null && report.getQuarterTotalMessages() > 0) {
            BigDecimal avgCost = report.getQuarterTotalRevenue().divide(BigDecimal.valueOf(report.getQuarterTotalMessages()), 4, RoundingMode.HALF_UP);
            report.setAverageMessageCost(avgCost);
        }

        // Determine top performing month
        String topMonth = determineTopPerformingMonth(report);
        report.setTopPerformingMonth(topMonth);

        // Calculate unique customer count for the quarter
        Long uniqueCustomers = calculateQuarterlyUniqueCustomers(accountId, year, quarter);
        report.setUniqueCustomerCount(uniqueCustomers);
    }

    /**
     * Calculate monthly revenue based on account's SMS price
     */
    private BigDecimal calculateMonthlyRevenue(UUID accountId, Long messageCount) {
        if (messageCount == null || messageCount == 0) {
            return BigDecimal.ZERO;
        }

        Optional<Account> account = accountRepository.findById(accountId);
        if (account.isPresent() && account.get().getAccSmsPrice() != null) {
            return account.get().getAccSmsPrice().multiply(BigDecimal.valueOf(messageCount));
        }

        return BigDecimal.ZERO;
    }

    /**
     * Get months for a specific quarter
     */
    private int[] getQuarterMonths(int quarter) {
        return switch (quarter) {
            case 3 -> new int[]{1, 2, 3};    // Jan, Feb, Mar
            case 4 -> new int[]{4, 5, 6};    // Apr, May, Jun
            case 1 -> new int[]{7, 8, 9};    // Jul, Aug, Sep
            case 2 -> new int[]{10, 11, 12}; // Oct, Nov, Dec
            default -> throw new IllegalArgumentException("Invalid quarter: " + quarter);
        };
    }

    /**
     * Get current quarter based on current month
     */
    private int getCurrentQuarter() {
        int currentMonth = LocalDateTime.now().getMonthValue();
        return switch (currentMonth) {
            case 1, 2, 3 -> 3;      // Jan, Feb, Mar = Q3
            case 4, 5, 6 -> 4;      // Apr, May, Jun = Q4
            case 7, 8, 9 -> 1;      // Jul, Aug, Sep = Q1
            case 10, 11, 12 -> 2;   // Oct, Nov, Dec = Q2
            default -> throw new IllegalArgumentException("Invalid month: " + currentMonth);
        };
    }

    /**
     * Determine the top performing month based on message count
     */
    private String determineTopPerformingMonth(AnnualReport report) {
        long month1Count = report.getMonth1MessageCount() != null ? report.getMonth1MessageCount() : 0L;
        long month2Count = report.getMonth2MessageCount() != null ? report.getMonth2MessageCount() : 0L;
        long month3Count = report.

                getMonth3MessageCount() != null ? report.getMonth3MessageCount() : 0L;

        if (month1Count >= month2Count && month1Count >= month3Count) {
            return getMonthName(report.getQuarter(), 1);
        } else if (month2Count >= month3Count) {
            return getMonthName(report.getQuarter(), 2

            );
        } else {
            return getMonthName(report.getQuarter(), 3);
        }
    }

    /**
     * Get month name based on quarter and position
     */
    private String getMonthName(int quarter, int position) {
        int[] months = getQuarterMonths(quarter);
        return Month.of(months[position - 1]).name();
    }

    /**
     * Set sender ID information for the report
     */
    private void setSenderIdInformation(AnnualReport report, UUID accountId, int year, int[] months) {
        // Get the most frequently used sender ID for this account during the quarter
        String sql = """
                SELECT 
                    mq.msg_sender_id_name,
                    COUNT(*) as usage_count
                FROM msg.message_queue_arc mq
                WHERE mq.msg_acc_id = ?1 
                AND EXTRACT(YEAR FROM mq.msg_created_date) = ?2 
                AND (EXTRACT(MONTH FROM mq.msg_created_date) = ?3 
                     OR EXTRACT(MONTH FROM mq.msg_created_date) = ?4 
                     OR EXTRACT(MONTH FROM mq.msg_created_date) = ?5)
                AND mq.msg_sender_id_name IS NOT NULL
                GROUP BY mq.msg_sender_id_name
                ORDER BY usage_count DESC
                LIMIT 1
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, accountId);
        query.setParameter(2, year);
        query.setParameter(3, months[0]);
        query.setParameter(4, months[1]);
        query.setParameter(5, months[2]);

        try {
            Object[] result = (Object[]) query.getSingleResult();
            String senderId = (String) result[0];
            report.setSenderId(senderId);

            // Get sender ID provider from shortcode setup
            if (senderId != null) {
                String providerSql = """
                        SELECT scs.sh_channel
                        FROM msg.shortcode_setup scs
                        WHERE UPPER(scs.sh_code) = UPPER(?1)
                        LIMIT 1
                        """;

                Query providerQuery = entityManager.createNativeQuery(providerSql);
                providerQuery.setParameter(1, senderId);

                try {
                    String provider = (String) providerQuery.getSingleResult();
                    report.setSenderIdProvider(provider);
                } catch (Exception e) {
                    // If no provider found, use default
                    report.setSenderIdProvider(null);
                }
            } else {
                report.setSenderIdProvider(null);
            }
        } catch (Exception e) {
            // If no sender ID found, leave as null and use default provider
            report.setSenderId(null);
            report.setSenderIdProvider(null);
        }
    }

    /**
     * Calculate unique customers for the entire quarter
     */
    private Long calculateQuarterlyUniqueCustomers(UUID accountId, int year, int quarter) {
        int[] months = getQuarterMonths(quarter);

        String sql = """
                SELECT COUNT(DISTINCT msg_sub_mobile_no) 
                FROM msg.message_queue_arc 
                WHERE msg_acc_id = ?1 
                AND EXTRACT(YEAR FROM msg_created_date) = ?2 
                AND (EXTRACT(MONTH FROM msg_created_date) = ?3 
                     OR EXTRACT(MONTH FROM msg_created_date) = ?4 
                     OR EXTRACT(MONTH FROM msg_created_date) = ?5)
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, accountId);
        query.setParameter(2, year);
        query.setParameter(3, months[0]);
        query.setParameter(4, months[1]);
        query.setParameter(5, months[2]);

        return ((Number) query.getSingleResult()).longValue();
    }

    /**
     * Get quarterly reports with pagination and filtering
     */
    public List<AnnualReportDto> getQuarterlyReports(Integer year, Integer quarter, UUID accountId, UUID resellerId) {
        List<AnnualReport> reports;

        if (accountId != null) {
            reports = annualReportRepository.findByAccountId(accountId);
        } else if (resellerId != null) {
            reports = annualReportRepository.findByResellerId(resellerId);
        } else if (year != null && quarter != null) {
            reports = annualReportRepository.findByYearAndQuarter(year, quarter);
        } else if (year != null) {

            reports = annualReportRepository.findByYearOrderedByQuarterAndAccount(year);
        } else {
            reports = annualReportRepository.findAll();
        }

        return reports.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     *
     *
     * Convert AnnualReport entity to DTO
     */
    private AnnualReportDto convertToDto(AnnualReport report) {
        int[] months = getQuarterMonths(report.getQuarter());

        return AnnualReportDto.builder().id(report.getId())
                .validityPeriod(report.getValidityPeriod())
                .senderId(report.getSenderId())
                .senderIdProvider(report.getSenderIdProvider())
                .year(report.getYear()).quarter(report.getQuarter()).accountId(report.getAccountId()).accountName(report.getAccountName()).resellerName(report.getResellerName()).resellerId(report.getResellerId()).month1(AnnualReportDto.MonthlyData.builder().monthName(Month.of(months[0]).name()).messageCount(report.getMonth1MessageCount()).revenue(report.getMonth1Revenue()).deliveredCount(report.getMonth1DeliveredCount()).failedCount(report.getMonth1FailedCount()).deliveryRate(calculateMonthlyDeliveryRate(report.getMonth1DeliveredCount(), report.getMonth1MessageCount())).build()).month2(AnnualReportDto.MonthlyData.builder().monthName(Month.of(months[1]).name()).messageCount(report.getMonth2MessageCount()).revenue(report.getMonth2Revenue()).deliveredCount(report.getMonth2DeliveredCount()).failedCount(report.getMonth2FailedCount()).deliveryRate(calculateMonthlyDeliveryRate(report.getMonth2DeliveredCount(), report.getMonth2MessageCount())).build()).month3(AnnualReportDto.MonthlyData.builder().monthName(Month.of(months[2]).name()).messageCount(report.getMonth3MessageCount()).revenue(report.getMonth3Revenue()).deliveredCount(report.getMonth3DeliveredCount()).failedCount(report.getMonth3FailedCount()).deliveryRate(calculateMonthlyDeliveryRate(report.getMonth3DeliveredCount(), report.getMonth3MessageCount())).build()).quarterTotalMessages(report.getQuarterTotalMessages()).quarterTotalRevenue(report.getQuarterTotalRevenue()).quarterDeliveredCount(report.getQuarterDeliveredCount()).quarterFailedCount(report.getQuarterFailedCount()).quarterDeliveryRate(report.getQuarterDeliveryRate()).averageMessageCost(report.getAverageMessageCost()).uniqueCustomerCount(report.getUniqueCustomerCount()).topPerformingMonth(report.getTopPerformingMonth()).status(report.getStatus()).createdAt(report.getCreatedAt()).updatedAt(report.getUpdatedAt()).build();
    }

    /**
     * Calculate monthly delivery rate
     */
    private BigDecimal calculateMonthlyDeliveryRate(Long delivered, Long total) {
        if (total == null || total == 0 || delivered == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(delivered).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    /**
     * Generate Excel report for quarterly data
     */
    public byte[] generateExcelReport(Integer year, Integer quarter, UUID accountId, UUID resellerId) throws IOException {
        List<AnnualReportDto>  reports = getQuarterlyReports(year, quarter, accountId, resellerId);

        try (

                Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Create main sheet
            Sheet sheet = workbook.createSheet("Quarterly Report");

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.

                    setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Create data style

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderLeft(

                    BorderStyle.THIN);

            // Create currency style
            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.cloneStyleFrom(dataStyle);
            currencyStyle.setDataFormat(

                    workbook.createDataFormat().getFormat("KSH #,##0.00"));

            CellStyle percentStyle = workbook.createCellStyle();
            percentStyle.cloneStyleFrom(dataStyle);
            percentStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
            // Create headers
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Account Name", "Reseller","Validity Period", "Sender ID", "Provider", "Year", "Quarter", "Month 1", "M1 Messages", "M1 Revenue", "M1 Delivered", "M1 Failed", "M1 Delivery Rate", "Month 2", "M2 Messages", "M2 Revenue",

                    "M2 Delivered", "M2 Failed", "M2 Delivery Rate", "Month 3", "M3 Messages", "M3 Revenue", "M3 Delivered", "M3 Failed", "M3 Delivery Rate", "Quarter Total Messages",

                    "Quarter Total Revenue", "Quarter Delivered", "Quarter Failed", "Quarter Delivery Rate", "Avg Message Cost", "Unique Customers", "Top Month", "Status"};

            for (

                    int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Fill data rows
            int rowNum = 1;
            for (AnnualReportDto report : reports) {
                Row row = sheet.createRow(rowNum++);
                int colNum = 0;

                // Basic info
                row.createCell(colNum++).setCellValue(report.getAccountName() != null ? report.getAccountName() : "");
                row.createCell(colNum++).setCellValue(report.getResellerName() != null ? report.getResellerName() : "");
                row.createCell(colNum++).setCellValue(report.getValidityPeriod() != null ? report.getValidityPeriod() : 0);
                row.createCell(colNum++).setCellValue(report.getSenderId() != null ? report.getSenderId() : "");
                row.createCell(colNum++).setCellValue(report.getSenderIdProvider() != null ? report.getSenderIdProvider() : "");
                row.createCell(colNum++).setCellValue(report.getYear() != null ? report.getYear() : 0);
                row.createCell(colNum++).setCellValue(report.getQuarter() != null ? report.getQuarter() : 0);

                // Month 1 data
                row.createCell(colNum++).setCellValue(report.getMonth1().

                        getMonthName());
                row.createCell(colNum++).setCellValue(report.getMonth1().getMessageCount() != null ? report.getMonth1().getMessageCount() : 0);

                Cell m1RevenueCell = row.createCell(colNum++);
                if (report.

                        getMonth1().getRevenue() != null) {
                    m1RevenueCell.setCellValue(report.getMonth1().getRevenue().doubleValue());
                    m1RevenueCell.setCellStyle(currencyStyle);
                }

                row.createCell(colNum++).setCellValue(report.getMonth1().getDeliveredCount(

                ) != null ? report.getMonth1().getDeliveredCount() : 0);
                row.createCell(colNum++).setCellValue(report.getMonth1().getFailedCount() != null ? report.getMonth1().getFailedCount() : 0);

                Cell

                        m1DeliveryRateCell = row.createCell(colNum++);
                if (report.getMonth1().getDeliveryRate() != null) {
                    m1DeliveryRateCell.setCellValue(report.getMonth1().getDeliveryRate().doubleValue() / 100);
                    m1DeliveryRateCell.setCellStyle(percentStyle);
                }

                // Month 2 data
                row.createCell(colNum++).setCellValue(report.getMonth2().getMonthName());
                row.createCell(colNum++).setCellValue(report.getMonth2().getMessageCount() != null ? report.getMonth2().getMessageCount() : 0);

                Cell m2RevenueCell = row.createCell(colNum++);
                if (report.getMonth2().getRevenue() != null) {
                    m2RevenueCell.setCellValue(report.getMonth2().getRevenue().doubleValue());
                    m2RevenueCell.setCellStyle(currencyStyle);
                }

                row.

                        createCell(colNum++).setCellValue(report.getMonth2().getDeliveredCount() != null ? report.getMonth2().getDeliveredCount() : 0);
                row.createCell(colNum++).setCellValue(report.getMonth2().getFailedCount() != null ? report.getMonth2().

                        getFailedCount() : 0);

                Cell m2DeliveryRateCell = row.createCell(colNum++);
                if (report.getMonth2().getDeliveryRate() != null) {
                    m2DeliveryRateCell.setCellValue(report.getMonth2().getDeliveryRate().doubleValue() / 100);
                    m2DeliveryRateCell.setCellStyle(percentStyle);
                }

                // Month 3 data
                row.createCell(colNum++).setCellValue(report.getMonth3().getMonthName());
                row.createCell(colNum++).setCellValue(report.getMonth3().

                        getMessageCount() != null ? report.getMonth3().getMessageCount() : 0);

                Cell m3RevenueCell = row.createCell(colNum++);
                if (report.getMonth3().getRevenue() != null) {
                    m3RevenueCell.setCellValue(report.getMonth3().getRevenue().doubleValue());

                    m3RevenueCell.setCellStyle(currencyStyle);
                }

                row.createCell(colNum++).setCellValue(report.getMonth3().getDeliveredCount() !=

                        null ? report.getMonth3().getDeliveredCount() : 0);
                row.createCell(colNum++).setCellValue(report.getMonth3().getFailedCount() != null ? report.getMonth3().getFailedCount() : 0);

                Cell m3DeliveryRateCell = row.createCell(colNum++);
                if (

                        report.getMonth3().getDeliveryRate() != null) {
                    m3DeliveryRateCell.setCellValue(report.getMonth3().getDeliveryRate().doubleValue() / 100);
                    m3DeliveryRateCell.setCellStyle(percentStyle);
                }

                // Quarter totals
                row.createCell(colNum++).setCellValue(report.getQuarterTotalMessages() != null ? report.getQuarterTotalMessages() : 0);

                Cell quarterRevenueCell = row.createCell(colNum++);
                if (report.getQuarterTotalRevenue() != null) {

                    quarterRevenueCell.setCellValue(report.getQuarterTotalRevenue().doubleValue());
                    quarterRevenueCell.setCellStyle(currencyStyle);
                }

                row.createCell(colNum++).setCellValue(report.getQuarterDeliveredCount() != null ? report.

                        getQuarterDeliveredCount() : 0);
                row.createCell(colNum++).setCellValue(report.getQuarterFailedCount() != null ? report.getQuarterFailedCount() : 0);

                Cell quarterDeliveryRateCell = row.createCell(colNum++);
                if (report.getQuarterDeliveryRate() != null) {
                    quarterDeliveryRateCell.setCellValue(report.

                            getQuarterDeliveryRate().doubleValue() / 100);
                    quarterDeliveryRateCell.setCellStyle(percentStyle);
                }

                Cell avgCostCell = row.createCell(colNum++);
                if (report.getAverageMessageCost() != null) {
                    avgCostCell.setCellValue(report.getAverageMessageCost().doubleValue());

                    avgCostCell.setCellStyle(currencyStyle);
                }

                row.createCell(colNum++).setCellValue(report.getUniqueCustomerCount() != null ? report.getUniqueCustomerCount() : 0);
                row.createCell(colNum++).setCellValue(report.getTopPerformingMonth() != null ? report.getTopPerformingMonth() : "");
                row.createCell(colNum++).setCellValue(report.getStatus() != null ? report.getStatus() : "");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Get available years for reports
     */
    public List<Integer> getAvailableYears() {
        return annualReportRepository.findDistinctYears();
    }

    /**
     * Manually trigger report generation for specific year and quarter
     */
    public void generateReportsManually(int year, int quarter) {
        log.info("Manually triggering report generation for Q{} {}", quarter, year);
        generateQuarterlyReports(year, quarter);
    }
}