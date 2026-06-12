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

    private static final int[] ITEM_ALIGNS = {BrandedPdf.L, BrandedPdf.R, BrandedPdf.R, BrandedPdf.R};

    // =======================================================================================
    //  Invoice (the bill)
    // =======================================================================================

    public void pdfInvoice(Invoice inv, HttpServletResponse resp) throws Exception {
        BrandedPdf.preparePdf(resp, "invoice-" + safeCode(inv));
        Document doc = BrandedPdf.openPortrait(resp, "Invoice", BrandedPdf.CONFIDENTIAL);

        BrandedPdf.addDocHeader(doc, "Billed To", payer(inv), mobileLine(inv),
                new String[]{"Invoice No.", "Issue Date", "Due Date"},
                new String[]{BrandedPdf.s(inv.getInvoCode()), fmtDate(inv.getInvoCreatedDate()), fmtDate(inv.getInvoDueDate())},
                statusText(inv));

        BigDecimal base = nz(inv.getInvoAmount());
        BigDecimal afterTax = afterTax(inv);
        BigDecimal vat = afterTax.subtract(base);

        BrandedPdf.addSummary(doc,
                new String[]{"Subtotal (KES)", vatLabel(inv), "Total Due (KES)"},
                new BigDecimal[]{base, vat, afterTax});

        addLineItems(doc, inv, base, vat, afterTax);

        BrandedPdf.addTotals(doc,
                new String[]{"Subtotal", vatLabel(inv), "Total Due"},
                new BigDecimal[]{base, vat, afterTax});

        BrandedPdf.addNote(doc, "Payable via M-Pesa using invoice reference " + BrandedPdf.s(inv.getInvoCode())
                + ". This invoice was generated electronically and is valid without a signature.");
        doc.close();
    }

    // =======================================================================================
    //  Receipt (proof of payment)
    // =======================================================================================

    public void pdfReceipt(Invoice inv, HttpServletResponse resp) throws Exception {
        BrandedPdf.preparePdf(resp, "receipt-" + safeCode(inv));
        Document doc = BrandedPdf.openPortrait(resp, "Payment Receipt", BrandedPdf.CONFIDENTIAL);

        BigDecimal base = nz(inv.getInvoAmount());
        BigDecimal afterTax = afterTax(inv);
        BigDecimal vat = afterTax.subtract(base);
        BigDecimal paid = inv.getInvoMarkedPaidAmount() != null ? inv.getInvoMarkedPaidAmount() : afterTax;

        BrandedPdf.addDocHeader(doc, "Received From", payer(inv), mobileLine(inv),
                new String[]{"Receipt No.", "Payment Date", "Reference"},
                new String[]{BrandedPdf.s(inv.getInvoCode()), paymentDate(inv), reference(inv)},
                statusText(inv));

        BrandedPdf.addSummary(doc,
                new String[]{"Amount Paid (KES)", vatLabel(inv), "Total Invoiced (KES)"},
                new BigDecimal[]{paid, vat, afterTax});

        addLineItems(doc, inv, base, vat, afterTax);

        BrandedPdf.addTotals(doc,
                new String[]{"Subtotal", vatLabel(inv), "Total Invoiced", "Amount Paid"},
                new BigDecimal[]{base, vat, afterTax, paid});

        BrandedPdf.addNote(doc, "This is a system-generated payment receipt for invoice "
                + BrandedPdf.s(inv.getInvoCode()) + ". Thank you for your payment.");
        doc.close();
    }

    // =======================================================================================
    //  Shared
    // =======================================================================================

    private void addLineItems(Document doc, Invoice inv, BigDecimal base, BigDecimal vat, BigDecimal afterTax)
            throws Exception {
        PdfPTable table = BrandedPdf.table(new float[]{4f, 1.8f, 1.8f, 2f});
        BrandedPdf.headerRow(table, ITEM_ALIGNS, "Description", "Subtotal", vatLabel(inv), "Amount (KES)");
        BrandedPdf.bodyRow(table, 0, ITEM_ALIGNS,
                "SMS credit purchase — " + BrandedPdf.s(inv.getInvoCode()),
                BrandedPdf.money(base), BrandedPdf.money(vat), BrandedPdf.money(afterTax));
        doc.add(table);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal afterTax(Invoice inv) {
        if (inv.getInvoAmountAfterTax() != null) return inv.getInvoAmountAfterTax();
        BigDecimal base = nz(inv.getInvoAmount());
        BigDecimal rate = inv.getInvoTaxRate() != null ? inv.getInvoTaxRate() : BigDecimal.ZERO;
        return base.add(base.multiply(rate));
    }

    private static String vatLabel(Invoice inv) {
        BigDecimal rate = inv.getInvoTaxRate() != null ? inv.getInvoTaxRate() : BigDecimal.ZERO;
        int pct = rate.multiply(BigDecimal.valueOf(100)).setScale(0, java.math.RoundingMode.HALF_UP).intValue();
        return "VAT (" + pct + "%)";
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
