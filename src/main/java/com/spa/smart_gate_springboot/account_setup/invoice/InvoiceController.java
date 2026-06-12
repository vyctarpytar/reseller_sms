package com.spa.smart_gate_springboot.account_setup.invoice;

import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.report.ReportExportService;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/invoice")
public class InvoiceController {
    private final InvoiceService invoiceService;
    private final UserService userService;
    private final ReportExportService reportExportService;

    /** Statuses that have a recorded payment and therefore expose a receipt. */
    private static final Set<InvoStatus> RECEIPTABLE = EnumSet.of(InvoStatus.PAID, InvoStatus.PARTIALLY_PAID);

    @GetMapping("/distinct-statuses")
    public StandardJsonResponse getDistinctInvoiceStatuses() {
        StandardJsonResponse resp = new StandardJsonResponse();
        List<String> invoStatus = Arrays.stream(InvoStatus.values()).map(Enum::name).collect(Collectors.toList());
        resp.setTotal(invoStatus.size());
        resp.setData("result", invoStatus, resp);
        return resp;

    }

    @PreAuthorize("hasAnyRole('ACCOUNTANT','SUPER_ADMIN', 'ADMIN')")
    @GetMapping("reseller-summary")
    public StandardJsonResponse getAllInvoices(HttpServletRequest request) {
        var user = userService.getCurrentUser(request);
        if (user.getLayer().equals(Layers.ACCOUNT))
            throw new RuntimeException("User is not a reseller / Top / Wallet Not Mapped");


        UUID usrResellerId = null;
        if (user.getLayer().equals(Layers.RESELLER)) {
            usrResellerId = user.getUsrResellerId();
        } else if (user.getLayer().equals(Layers.TOP)) {
            usrResellerId = user.getUsrId();
        }

        if (usrResellerId == null) throw new RuntimeException("User is not a reseller / Top / Wallet Not Mapped");

        return invoiceService.getResellerInvoicesPerYearSummary(usrResellerId);
    }

    /** Branded invoice PDF (the bill). Streamed inline so the frontend can preview or download it. */
    @GetMapping("/{invoId}/invoice-pdf")
    public void downloadInvoicePdf(@PathVariable UUID invoId, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Invoice invoice = invoiceService.findById(invoId);
        assertCanAccess(invoice, request);
        reportExportService.pdfInvoice(invoice, response);
    }

    /** Branded receipt PDF (proof of payment). Only available once a payment has been recorded. */
    @GetMapping("/{invoId}/receipt-pdf")
    public void downloadReceiptPdf(@PathVariable UUID invoId, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Invoice invoice = invoiceService.findById(invoId);
        assertCanAccess(invoice, request);
        if (!RECEIPTABLE.contains(invoice.getInvoStatus())) {
            throw new RuntimeException("No receipt available — invoice " + invoice.getInvoCode() + " has no recorded payment");
        }
        reportExportService.pdfReceipt(invoice, response);
    }

    /**
     * Tenant scoping for invoice/receipt PDFs: a reseller may only read their own invoices, an account
     * only its own; TOP may read any. Without this any logged-in user could pull another tenant's
     * invoice/receipt by guessing the UUID.
     */
    private void assertCanAccess(Invoice invoice, HttpServletRequest request) {
        var user = userService.getCurrentUser(request);
        Layers layer = user.getLayer();
        if (layer == Layers.TOP) return;
        if (layer == Layers.RESELLER
                && user.getUsrResellerId() != null
                && user.getUsrResellerId().equals(invoice.getInvoResellerId())) return;
        if (layer == Layers.ACCOUNT
                && user.getUsrAccId() != null
                && user.getUsrAccId().equals(invoice.getInvoAccId())) return;
        throw new RuntimeException("Not authorized to access invoice " + invoice.getInvoCode());
    }

}
