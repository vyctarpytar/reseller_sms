package com.spa.smart_gate_springboot.report;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import jakarta.servlet.http.HttpServletResponse;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;

/**
 * Shared look-and-feel for every branded PDF export in the platform (Synq Africa).
 *
 * <p>This is the single source of truth for the brand palette, fonts, the page chrome
 * (letterhead header band, footer band, page number, faded Africa-spark watermark) and the
 * styled building blocks (stat cards, document headers, zebra tables, rounded status pills,
 * totals). Every report reuses these — never hand-roll fonts, colours, headers or tables in an
 * individual report. If a building block is missing, add it here so all reports inherit it.
 *
 * <p>Brand source of truth lives in {@code cooporate_assets/}; the chrome PNGs are mirrored into
 * {@code src/main/resources/branding/} (see the {@code branded-pdf} skill).
 */
public final class BrandedPdf {

    private BrandedPdf() {
    }

    // ---- Palette (define once here; no ad-hoc colours in reports) --------------------------
    static final Color BROWN = new Color(0x69, 0x47, 0x2E);   // primary — wordmark, table headers, footer rule
    static final Color TERRACOTTA = new Color(0xD9, 0x6C, 0x3B); // accent — hairline rule, card top accent
    static final Color TERRACOTTA_DK = new Color(0xB8, 0x56, 0x2A);
    static final Color DARK = new Color(0x13, 0x16, 0x1D);
    static final Color OFF_WHITE = new Color(0xFA, 0xFA, 0xF9);
    static final Color ZEBRA = new Color(0xFA, 0xF8, 0xF5);
    static final Color CREAM = new Color(0xF2, 0xEF, 0xEA);
    static final Color BORDER = new Color(0xEE, 0xEA, 0xE4);
    static final Color INK = new Color(0x33, 0x41, 0x55);
    static final Color MUTED = new Color(0x64, 0x74, 0x8B);

    static final Color GREEN_TXT = new Color(0x04, 0x78, 0x57);
    static final Color AMBER_TXT = new Color(0xB4, 0x53, 0x09);
    static final Color RED_TXT = new Color(0xB9, 0x1C, 0x1C);
    static final Color GREEN_BG = new Color(0xA7, 0xF3, 0xD0);
    static final Color AMBER_BG = new Color(0xFD, 0xE6, 0x8A);
    static final Color RED_BG = new Color(0xFE, 0xCA, 0xCA);
    static final Color GREY_BG = new Color(0xE2, 0xE8, 0xF0);

    // ---- Alignment shorthands --------------------------------------------------------------
    public static final int L = Element.ALIGN_LEFT;
    public static final int R = Element.ALIGN_RIGHT;
    public static final int C = Element.ALIGN_CENTER;

    /** Pass to an {@code open*} method to pin a recipient-only confidentiality notice. */
    public static final boolean CONFIDENTIAL = true;

    // ---- Chrome geometry / assets ----------------------------------------------------------
    private static final float HEADER_RATIO = 150f / 1000f; // header.svg viewBox h/w
    private static final float FOOTER_RATIO = 90f / 1000f;  // footer.svg viewBox h/w
    private static final float WATERMARK_OPACITY = 0.05f;

    private static final byte[] HEADER_PNG = load("/branding/header.png");
    private static final byte[] FOOTER_PNG = load("/branding/footer.png");
    private static final byte[] MARK_PNG = load("/branding/mark.png");

    private static final BaseFont BASE = baseFont();

    private static BaseFont baseFont() {
        try {
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new RuntimeException("Could not create base font for PDF", e);
        }
    }

    // ---- Fonts (the type scale — do not set fonts per cell in reports) ---------------------
    private static final Font H2 = new Font(Font.HELVETICA, 15, Font.BOLD, BROWN);
    private static final Font H_LABEL = new Font(Font.HELVETICA, 7.5f, Font.BOLD, MUTED);
    private static final Font H_NAME = new Font(Font.HELVETICA, 12, Font.BOLD, BROWN);
    private static final Font H_SUB = new Font(Font.HELVETICA, 9, Font.NORMAL, MUTED);
    private static final Font META_LABEL = new Font(Font.HELVETICA, 8, Font.NORMAL, MUTED);
    private static final Font META_VALUE = new Font(Font.HELVETICA, 9, Font.BOLD, INK);
    private static final Font CARD_LABEL = new Font(Font.HELVETICA, 7, Font.NORMAL, MUTED);
    private static final Font CARD_VALUE = new Font(Font.HELVETICA, 15, Font.BOLD, BROWN);
    private static final Font TH = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font TD = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, INK);
    private static final Font TOTAL_LABEL = new Font(Font.HELVETICA, 9, Font.NORMAL, MUTED);
    private static final Font TOTAL_VALUE = new Font(Font.HELVETICA, 9.5f, Font.NORMAL, INK);
    private static final Font TOTAL_GRAND = new Font(Font.HELVETICA, 11, Font.BOLD, BROWN);
    private static final Font NOTE = new Font(Font.HELVETICA, 8, Font.ITALIC, MUTED);

    // =======================================================================================
    //  Document lifecycle
    // =======================================================================================

    /** Set {@code Content-Disposition} (inline) to {@code <file-name>-<today>.pdf}. Call before opening. */
    public static void preparePdf(HttpServletResponse resp, String fileName) {
        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "-" + LocalDate.now() + ".pdf\"");
    }

    public static Document openPortrait(HttpServletResponse resp, String title) throws DocumentException, IOException {
        return open(resp, title, false, false);
    }

    public static Document openPortrait(HttpServletResponse resp, String title, boolean confidential)
            throws DocumentException, IOException {
        return open(resp, title, false, confidential);
    }

    public static Document openLandscape(HttpServletResponse resp, String title) throws DocumentException, IOException {
        return open(resp, title, true, false);
    }

    public static Document openLandscape(HttpServletResponse resp, String title, boolean confidential)
            throws DocumentException, IOException {
        return open(resp, title, true, confidential);
    }

    private static Document open(HttpServletResponse resp, String title, boolean landscape, boolean confidential)
            throws DocumentException, IOException {
        Rectangle size = landscape ? PageSize.A4.rotate() : PageSize.A4;
        float pw = size.getWidth();
        float headerH = pw * HEADER_RATIO;
        float footerH = pw * FOOTER_RATIO;
        float top = headerH + 18;
        float bottom = footerH + 20 + (confidential ? 14 : 0);

        Document doc = new Document(size, 40, 40, top, bottom);
        PdfWriter writer = PdfWriter.getInstance(doc, resp.getOutputStream());
        writer.setPageEvent(new Chrome(confidential));
        doc.open();

        if (title != null && !title.isEmpty()) {
            Paragraph p = new Paragraph(title, H2);
            p.setSpacingAfter(12);
            doc.add(p);
        }
        return doc;
    }

    // =======================================================================================
    //  Header blocks
    // =======================================================================================

    /** Reseller/account panel: name + identifiers (left, terracotta accent) + statement period (right). */
    public static void addStatementHeader(Document doc, String name, String account, String phone,
                                          Object from, Object to) throws DocumentException {
        PdfPTable t = headerShell();
        t.addCell(leftCell("STATEMENT FOR", s(name), labelled("Account", account), labelled("Mobile", phone)));
        t.addCell(metaCell(new String[]{"Statement Period"}, new String[]{period(from, to)}, null));
        doc.add(t);
    }

    /**
     * Generic document header for issued documents (invoices, receipts): a recipient block on the
     * left (terracotta accent) and a right-aligned grid of meta pairs, with an optional status.
     */
    public static void addDocHeader(Document doc, String recipientLabel, String recipientName, String contactLine,
                                    String[] metaLabels, String[] metaValues, String status) throws DocumentException {
        PdfPTable t = headerShell();
        t.addCell(leftCell(recipientLabel, blank(recipientName) ? "—" : recipientName, contactLine));
        t.addCell(metaCell(metaLabels, metaValues, status));
        doc.add(t);
    }

    private static PdfPTable headerShell() throws DocumentException {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1.4f, 1f});
        t.setSpacingAfter(18);
        return t;
    }

    private static PdfPCell leftCell(String heading, String name, String... subLines) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.LEFT);
        cell.setBorderColorLeft(TERRACOTTA);
        cell.setBorderWidthLeft(3f);
        cell.setPaddingLeft(12);
        cell.setPaddingTop(2);
        cell.setPaddingBottom(2);

        Paragraph h = new Paragraph(up(heading), H_LABEL);
        h.setSpacingAfter(4);
        cell.addElement(h);
        cell.addElement(new Paragraph(s(name), H_NAME));
        for (String line : subLines) {
            if (!blank(line)) {
                Paragraph sp = new Paragraph(line, H_SUB);
                sp.setSpacingBefore(2);
                cell.addElement(sp);
            }
        }
        return cell;
    }

    private static PdfPCell metaCell(String[] labels, String[] values, String status) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);

        PdfPTable grid = new PdfPTable(2);
        try {
            grid.setWidths(new float[]{1f, 1.1f});
        } catch (DocumentException ignored) {
        }
        grid.setWidthPercentage(100);
        grid.setHorizontalAlignment(Element.ALIGN_RIGHT);

        for (int i = 0; i < labels.length; i++) {
            grid.addCell(metaText(labels[i], META_LABEL));
            grid.addCell(metaText(values != null && i < values.length ? values[i] : "", META_VALUE));
        }
        if (!blank(status)) {
            grid.addCell(metaText("Status", META_LABEL));
            PdfPCell sc = new PdfPCell(new Phrase(up(status), new Font(Font.HELVETICA, 9, Font.BOLD, statusColor(status))));
            sc.setBorder(Rectangle.NO_BORDER);
            sc.setHorizontalAlignment(Element.ALIGN_RIGHT);
            sc.setPaddingTop(3);
            grid.addCell(sc);
        }
        cell.addElement(grid);
        return cell;
    }

    private static PdfPCell metaText(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(s(text), font));
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setPaddingTop(3);
        c.setPaddingBottom(3);
        return c;
    }

    /** Muted "Period: …" line for aggregate reports without a header block. */
    public static void addPeriodLine(Document doc, Object from, Object to) throws DocumentException {
        Paragraph p = new Paragraph("Period: " + period(from, to), H_SUB);
        p.setSpacingAfter(12);
        doc.add(p);
    }

    // =======================================================================================
    //  Stat cards
    // =======================================================================================

    /** Dashboard stat cards: terracotta top accent, uppercase muted label, large brown value. */
    public static void addSummary(Document doc, String[] labels, BigDecimal[] values) throws DocumentException {
        addSummaryStrings(doc, labels, moneyAll(values));
    }

    /** Stat cards with pre-formatted string values (use when a value is not money). */
    public static void addSummaryStrings(Document doc, String[] labels, String[] values) throws DocumentException {
        int n = labels.length;
        int cols = n * 2 - 1; // card, spacer, card, spacer, ...
        float[] widths = new float[cols];
        for (int i = 0; i < cols; i++) widths[i] = (i % 2 == 0) ? 1f : 0.08f;

        PdfPTable t = new PdfPTable(cols);
        t.setWidthPercentage(100);
        t.setWidths(widths);
        t.setSpacingAfter(20);

        for (int i = 0; i < n; i++) {
            PdfPCell card = new PdfPCell();
            card.setBorder(Rectangle.BOX);
            card.setBorderColor(BORDER);
            card.setBorderWidth(0.8f);
            card.setUseVariableBorders(true);
            card.setBorderColorTop(TERRACOTTA);
            card.setBorderWidthTop(3f);
            card.setPadding(13);
            card.setBackgroundColor(OFF_WHITE);

            Paragraph lbl = new Paragraph(up(labels[i]), CARD_LABEL);
            lbl.setSpacingAfter(6);
            card.addElement(lbl);
            card.addElement(new Paragraph(values[i], CARD_VALUE));
            t.addCell(card);

            if (i < n - 1) {
                PdfPCell spacer = new PdfPCell();
                spacer.setBorder(Rectangle.NO_BORDER);
                t.addCell(spacer);
            }
        }
        doc.add(t);
    }

    // =======================================================================================
    //  Tables
    // =======================================================================================

    public static PdfPTable table(float[] widths) throws DocumentException {
        PdfPTable t = new PdfPTable(widths.length);
        t.setWidthPercentage(100);
        t.setWidths(widths);
        t.setSpacingBefore(6);
        return t;
    }

    public static void headerRow(PdfPTable t, int[] aligns, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            PdfPCell c = new PdfPCell(new Phrase(s(cells[i]), TH));
            c.setBackgroundColor(BROWN);
            c.setBorder(Rectangle.NO_BORDER);
            c.setHorizontalAlignment(aligns[i]);
            c.setVerticalAlignment(Element.ALIGN_MIDDLE);
            c.setPadding(9);
            t.addCell(c);
        }
        t.setHeaderRows(1);
    }

    public static void bodyRow(PdfPTable t, int rowIndex, int[] aligns, String... cells) {
        bodyRow(t, rowIndex, aligns, -1, cells);
    }

    /** Zebra-striped row; {@code chipColumn} renders as a rounded status pill ({@code -1} for none). */
    public static void bodyRow(PdfPTable t, int rowIndex, int[] aligns, int chipColumn, String... cells) {
        Color bg = (rowIndex % 2 == 1) ? ZEBRA : Color.WHITE;
        for (int i = 0; i < cells.length; i++) {
            PdfPCell c;
            if (i == chipColumn) {
                String v = s(cells[i]);
                c = new PdfPCell(new Phrase(up(v), new Font(Font.HELVETICA, 8, Font.BOLD, statusColor(v))));
                c.setCellEvent(new PillEvent(up(v), statusChipBg(v)));
            } else {
                c = new PdfPCell(new Phrase(s(cells[i]), TD));
            }
            c.setBackgroundColor(bg);
            c.setBorder(Rectangle.BOTTOM);
            c.setBorderColorBottom(BORDER);
            c.setBorderWidthBottom(0.6f);
            c.setHorizontalAlignment(aligns[i]);
            c.setVerticalAlignment(Element.ALIGN_MIDDLE);
            c.setPadding(8);
            t.addCell(c);
        }
    }

    // =======================================================================================
    //  Totals
    // =======================================================================================

    /** Right-aligned totals stack; the last row is emphasised as the grand total. */
    public static void addTotals(Document doc, String[] labels, BigDecimal[] values) throws DocumentException {
        PdfPTable wrap = new PdfPTable(2);
        wrap.setWidthPercentage(100);
        wrap.setWidths(new float[]{1.2f, 1f});
        wrap.setSpacingBefore(14);

        PdfPCell spacer = new PdfPCell();
        spacer.setBorder(Rectangle.NO_BORDER);
        wrap.addCell(spacer);

        PdfPTable totals = new PdfPTable(2);
        totals.setWidths(new float[]{1.3f, 1f});
        totals.setWidthPercentage(100);

        int last = labels.length - 1;
        for (int i = 0; i < labels.length; i++) {
            boolean grand = (i == last);
            Font lf = grand ? TOTAL_GRAND : TOTAL_LABEL;
            Font vf = grand ? TOTAL_GRAND : TOTAL_VALUE;

            PdfPCell lc = new PdfPCell(new Phrase(labels[i], lf));
            PdfPCell vc = new PdfPCell(new Phrase(money(values[i]), vf));
            for (PdfPCell c : new PdfPCell[]{lc, vc}) {
                c.setBorder(grand ? Rectangle.TOP : Rectangle.NO_BORDER);
                c.setBorderColorTop(BORDER);
                c.setBorderWidthTop(1f);
                c.setHorizontalAlignment(Element.ALIGN_RIGHT);
                c.setPadding(6);
            }
            totals.addCell(lc);
            totals.addCell(vc);
        }

        PdfPCell holder = new PdfPCell(totals);
        holder.setBorder(Rectangle.NO_BORDER);
        wrap.addCell(holder);
        doc.add(wrap);
    }

    /** Small muted italic note paragraph (payment instructions, system-generated disclaimers, …). */
    public static void addNote(Document doc, String text) throws DocumentException {
        Paragraph p = new Paragraph(s(text), NOTE);
        p.setSpacingBefore(18);
        doc.add(p);
    }

    // =======================================================================================
    //  Formatting helpers
    // =======================================================================================

    public static String money(BigDecimal v) {
        if (v == null) v = BigDecimal.ZERO;
        v = v.setScale(2, RoundingMode.HALF_UP);
        boolean whole = v.stripTrailingZeros().scale() <= 0;
        DecimalFormat df = new DecimalFormat(whole ? "#,##0" : "#,##0.00");
        return df.format(v);
    }

    private static String[] moneyAll(BigDecimal[] values) {
        String[] out = new String[values.length];
        for (int i = 0; i < values.length; i++) out[i] = money(values[i]);
        return out;
    }

    public static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    public static String s(String s) {
        return s == null ? "" : s;
    }

    static Color statusColor(String status) {
        switch (bucket(status)) {
            case 1: return GREEN_TXT;
            case 2: return AMBER_TXT;
            case 3: return RED_TXT;
            default: return MUTED;
        }
    }

    static Color statusChipBg(String status) {
        switch (bucket(status)) {
            case 1: return GREEN_BG;
            case 2: return AMBER_BG;
            case 3: return RED_BG;
            default: return GREY_BG;
        }
    }

    /** 1 = green, 2 = amber, 3 = red, 0 = grey. */
    private static int bucket(String status) {
        if (blank(status)) return 0;
        String u = status.trim().toUpperCase();
        if (u.matches(".*(DELIVERED|SENT|ACTIVE|PAID|COMPLETED|APPROVED|PROCESSED|SUCCESS).*")
                && !u.contains("PARTIALLY") && !u.contains("PENDING") && !u.contains("UNDELIVERED")) {
            return 1;
        }
        if (u.matches(".*(QUEUED|PENDING|SUBMITTED|PROCESSING|PARTIALLY|AWAIT).*")) return 2;
        if (u.matches(".*(FAILED|REJECTED|BLOCKED|EXPIRED|UNDELIVERED|REVERSED|CANCELL?ED).*")) return 3;
        return 0;
    }

    // =======================================================================================
    //  Internals
    // =======================================================================================

    private static String period(Object from, Object to) {
        boolean a = from != null && !str(from).isEmpty();
        boolean b = to != null && !str(to).isEmpty();
        if (!a && !b) return "All dates";
        return (a ? str(from) : "…") + " – " + (b ? str(to) : "…");
    }

    private static String labelled(String label, String value) {
        if (blank(value)) return null;
        return label + ": " + value;
    }

    private static String up(String s) {
        return s == null ? "" : s.toUpperCase();
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static byte[] load(String path) {
        try (InputStream in = BrandedPdf.class.getResourceAsStream(path)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    /** Draws a rounded status pill on the cell background canvas, sized to the text. */
    private static final class PillEvent implements PdfPCellEvent {
        private final String text;
        private final Color bg;

        PillEvent(String text, Color bg) {
            this.text = text;
            this.bg = bg;
        }

        @Override
        public void cellLayout(PdfPCell cell, Rectangle pos, PdfContentByte[] canvases) {
            PdfContentByte cb = canvases[PdfPTable.BACKGROUNDCANVAS];
            float textW = BASE.getWidthPoint(text, 8);
            float pillW = Math.min(pos.getWidth() - 6, textW + 16);
            float pillH = 15;
            float cx = (pos.getLeft() + pos.getRight()) / 2f;
            float cy = (pos.getTop() + pos.getBottom()) / 2f;
            cb.saveState();
            cb.setColorFill(bg);
            cb.roundRectangle(cx - pillW / 2f, cy - pillH / 2f, pillW, pillH, pillH / 2f);
            cb.fill();
            cb.restoreState();
        }
    }

    /** Page chrome: header + footer bands, watermark, page number, optional confidentiality notice. */
    private static final class Chrome extends PdfPageEventHelper {
        private final boolean confidential;
        private PdfTemplate total;
        private BaseFont bf;

        Chrome(boolean confidential) {
            this.confidential = confidential;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            bf = BASE;
            total = writer.getDirectContent().createTemplate(40, 12);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Rectangle page = document.getPageSize();
            float pw = page.getWidth();
            float ph = page.getHeight();
            PdfContentByte cb = writer.getDirectContent();

            // header + footer bands (full page width)
            band(cb, HEADER_PNG, pw, ph, true);
            band(cb, FOOTER_PNG, pw, ph, false);

            // faded watermark behind body text
            watermark(writer, pw, ph);

            float footerH = pw * FOOTER_RATIO;

            // page number, bottom-right
            String txt = "Page " + writer.getPageNumber() + " of ";
            float w = bf.getWidthPoint(txt, 9);
            float x = pw - 40 - w - 12;
            float y = 6;
            cb.beginText();
            cb.setFontAndSize(bf, 9);
            cb.setColorFill(MUTED);
            cb.setTextMatrix(x, y);
            cb.showText(txt);
            cb.endText();
            cb.addTemplate(total, x + w, y);

            if (confidential) {
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                        new Phrase("Confidential — intended solely for the named recipient. Please do not redistribute.", NOTE),
                        pw / 2f, footerH + 5, 0);
            }
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            total.beginText();
            total.setFontAndSize(bf, 9);
            total.setColorFill(MUTED);
            total.setTextMatrix(0, 3);
            total.showText(String.valueOf(writer.getPageNumber()));
            total.endText();
        }

        private void band(PdfContentByte cb, byte[] png, float pw, float ph, boolean top) {
            if (png == null) return;
            try {
                Image im = Image.getInstance(png);
                float scale = pw / im.getWidth();
                float h = im.getHeight() * scale;
                im.scaleAbsolute(pw, h);
                im.setAbsolutePosition(0, top ? ph - h : 0);
                cb.addImage(im);
            } catch (Exception ignored) {
            }
        }

        private void watermark(PdfWriter writer, float pw, float ph) {
            if (MARK_PNG == null) return;
            try {
                Image mark = Image.getInstance(MARK_PNG);
                float mw = Math.min(pw, ph) * 0.62f;
                float scale = mw / mark.getWidth();
                float mh = mark.getHeight() * scale;
                mark.scaleAbsolute(mw, mh);
                mark.setAbsolutePosition((pw - mw) / 2f, (ph - mh) / 2f);

                PdfContentByte under = writer.getDirectContentUnder();
                PdfGState gs = new PdfGState();
                gs.setFillOpacity(WATERMARK_OPACITY);
                gs.setStrokeOpacity(WATERMARK_OPACITY);
                under.saveState();
                under.setGState(gs);
                under.addImage(mark);
                under.restoreState();
            } catch (Exception ignored) {
            }
        }
    }
}
