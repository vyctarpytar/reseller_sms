package com.spa.smart_gate_springboot.report;

import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfPTable;
import com.spa.smart_gate_springboot.account_setup.invoice.Invoice;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Branded PDF exports. One method per document; all styling/chrome comes from {@link BrandedPdf}.
 */
@Service
@Slf4j
public class ReportExportService {

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private static final int[] ITEM_ALIGNS = {BrandedPdf.L, BrandedPdf.R};

    // =======================================================================================
    //  Invoice (the bill)
    // =======================================================================================

    public void pdfInvoice(Invoice inv, HttpServletResponse resp) throws Exception {
        BrandedPdf.preparePdf(resp, "invoice-" + safeCode(inv));
        Document doc = BrandedPdf.openPortrait(resp, "Invoice", BrandedPdf.CONFIDENTIAL,
                "Payable via M-Pesa using invoice reference " + BrandedPdf.s(inv.getInvoCode())
                        + ". This invoice was generated electronically and is valid without a signature."
                        + " All prices are inclusive of any applicable taxes.");

        BrandedPdf.addDocHeader(doc, "Billed To", payer(inv), mobileLine(inv),
                new String[]{"Invoice No.", "Issue Date", "Due Date"},
                new String[]{BrandedPdf.s(inv.getInvoCode()), fmtDate(inv.getInvoCreatedDate()), fmtDate(inv.getInvoDueDate())},
                statusText(inv));

        BigDecimal amount = nz(inv.getInvoAmount());

        BrandedPdf.addSummary(doc,
                new String[]{"Total Due (KES)"},
                new BigDecimal[]{amount});

        addLineItems(doc, inv, amount);

        BrandedPdf.addTotals(doc,
                new String[]{"Total Due"},
                new BigDecimal[]{amount});

        doc.close();
    }

    // =======================================================================================
    //  Receipt (proof of payment)
    // =======================================================================================

    public void pdfReceipt(Invoice inv, HttpServletResponse resp) throws Exception {
        BrandedPdf.preparePdf(resp, "receipt-" + safeCode(inv));
        Document doc = BrandedPdf.openPortrait(resp, "Payment Receipt", BrandedPdf.CONFIDENTIAL,
                "This is a system-generated payment receipt for invoice "
                        + BrandedPdf.s(inv.getInvoCode()) + ". Thank you for your payment.");

        BigDecimal amount = nz(inv.getInvoAmount());
        BigDecimal paid = inv.getInvoMarkedPaidAmount() != null ? inv.getInvoMarkedPaidAmount() : amount;

        BrandedPdf.addDocHeader(doc, "Received From", payer(inv), mobileLine(inv),
                new String[]{"Receipt No.", "Payment Date", "Reference"},
                new String[]{BrandedPdf.s(inv.getInvoCode()), paymentDate(inv), reference(inv)},
                statusText(inv));

        BrandedPdf.addSummary(doc,
                new String[]{"Amount Paid (KES)", "Total Invoiced (KES)"},
                new BigDecimal[]{paid, amount});

        addLineItems(doc, inv, amount);

        BrandedPdf.addTotals(doc,
                new String[]{"Total Invoiced", "Amount Paid"},
                new BigDecimal[]{amount, paid});

        doc.close();
    }

    // =======================================================================================
    //  Shared
    // =======================================================================================

    private void addLineItems(Document doc, Invoice inv, BigDecimal amount) throws Exception {
        String desc = "SMS credit purchase — " + BrandedPdf.s(inv.getInvoCode());
        PdfPTable table = BrandedPdf.table(new float[]{4f, 2f});
        BrandedPdf.headerRow(table, ITEM_ALIGNS, "Description", "Amount (KES)");
        BrandedPdf.bodyRow(table, 0, ITEM_ALIGNS, desc, BrandedPdf.money(amount));
        doc.add(table);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String payer(Invoice inv) {
        if (inv.getInvoPayerName() != null && !inv.getInvoPayerName().trim().isEmpty()) {
            return inv.getInvoPayerName().trim();
        }
        if (inv.getInvoPayerMobileNumber() != null && !inv.getInvoPayerMobileNumber().trim().isEmpty()) {
            return inv.getInvoPayerMobileNumber().trim();
        }
        return "Customer";
    }

    private static String mobileLine(Invoice inv) {
        String m = inv.getInvoPayerMobileNumber();
        return (m == null || m.trim().isEmpty()) ? null : "Mobile: " + m.trim();
    }

    private static String statusText(Invoice inv) {
        return inv.getInvoStatus() == null ? null : inv.getInvoStatus().name();
    }

    private static String reference(Invoice inv) {
        String r = inv.getInvoMarkedPaidReference();
        return (r == null || r.trim().isEmpty()) ? "—" : r.trim();
    }

    private static String paymentDate(Invoice inv) {
        if (inv.getInvoMarkedPaidDate() != null) return fmtDateTime(inv.getInvoMarkedPaidDate());
        if (inv.getInvoMarkedPaidValueDate() != null) return fmt(inv.getInvoMarkedPaidValueDate());
        return fmtDateTime(inv.getInvoCreatedDate());
    }

    private static String safeCode(Invoice inv) {
        String c = inv.getInvoCode();
        return (c == null || c.trim().isEmpty()) ? "document" : c.trim();
    }

    private static String fmtDate(LocalDateTime t) {
        return t == null ? "—" : t.format(D);
    }

    private static String fmtDateTime(LocalDateTime t) {
        return t == null ? "—" : t.format(DT);
    }

    private static String fmt(Date d) {
        return d == null ? "—" : new SimpleDateFormat("dd MMM yyyy").format(d);
    }
}
