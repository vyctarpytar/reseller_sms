---
name: branded-pdf
description: >-
  Build branded PDF exports for the Synq Africa SMS reseller backend using a shared BrandedPdf
  chrome (official Synq logo letterhead, terracotta/brown palette, footer, page numbers, faded
  Africa-spark watermark) plus styled building blocks: dashboard stat cards, account/reseller
  header blocks, zebra tables with right-aligned numeric columns, and rounded status pills.
  Trigger when adding or restyling any PDF report/statement/invoice/export in
  src/main/java/com/spa/smart_gate_springboot/, or when asked to make a PDF "branded", "neat",
  "cleaner", or consistent with the others. Brand source of truth: cooporate_assets/.
---

# Branded PDF generation (Synq Africa)

Brand fundamentals, the official logo, palette, and the letterhead/footer chrome live in
[`cooporate_assets/`](../../../cooporate_assets/) — read [`cooporate_assets/README.md`](../../../cooporate_assets/README.md)
first. **The logo is fixed art: never redraw, recolour, or trace it.**

PDF exports are generated server-side with **OpenPDF** (LibrePDF fork of iText 5, package
`com.lowagie.text`). The shared look-and-feel lives in a single `BrandedPdf` helper class in
package `com.spa.smart_gate_springboot.report`; each report is a method on a `ReportExportService`
in that package.

> **Status of the infra:** as of this skill's install the project has **no** `report` package and
> **no OpenPDF dependency yet** — the first PDF export must bootstrap them (see *Bootstrap* below).
> Once `BrandedPdf` exists, every later report reuses it. **Never hand-roll fonts, colours, headers,
> or tables in a new PDF.** If a building block is missing, add it to `BrandedPdf` so all reports
> inherit it, rather than styling inline in one report.

## Bootstrap (first PDF export only)

1. **Add OpenPDF to [`pom.xml`](../../../pom.xml):**
   ```xml
   <dependency>
     <groupId>com.github.librepdf</groupId>
     <artifactId>openpdf</artifactId>
     <version>1.3.30</version>
   </dependency>
   ```
2. **Mirror the brand chrome into the classpath.** Export the source-of-truth SVGs to PNG and drop
   them in `src/main/resources/branding/` (create it):
   ```bash
   rsvg-convert -w 2000 cooporate_assets/pdf/header.svg > src/main/resources/branding/header.png
   rsvg-convert -w 2000 cooporate_assets/pdf/footer.svg > src/main/resources/branding/footer.png
   ```
   Also copy the stacked mark for the watermark source:
   `cp cooporate_assets/logos/synq-africa-mark.png src/main/resources/branding/mark.png`.
   Per project convention brand-asset changes **originate in `cooporate_assets/`** and are mirrored
   here — never hand-edit the PNGs in `resources/branding/`.
3. **Create `com.spa.smart_gate_springboot.report.BrandedPdf`** (a `final class`, package-private
   members) encoding the palette, fonts, page event (header/footer/page-number/watermark), and the
   building blocks below. Keep it the *only* place fonts/colours are defined.
4. **Create `ReportExportService`** (`@Service`) in the same package with one method per report.

> Build note: this project builds on **JDK 17** (JDK 21 breaks Lombok) — set `JAVA_HOME` to 17
> before `mvn`.

## Palette (define once in BrandedPdf)

From [`cooporate_assets/README.md`](../../../cooporate_assets/README.md) / the app Tailwind config —
warm earth tones, **not** navy/gold:

`BROWN #69472E` (primary — wordmark, table headers, footer rule) · `TERRACOTTA #D96C3B` (accent —
hairline rule, card top accent) · `TERRACOTTA_DK #b8562a` · `DARK #13161D` · `OFF_WHITE #fafaf9` ·
`ZEBRA #FAF8F5` · `CREAM #F2EFEA` · `BORDER #EEEAE4` · `INK #334155` · `MUTED #64748b`.
Status text: green `#047857`, amber `#b45309`, red `#b91c1c`. Status pill fills: green `#a7f3d0`,
amber `#fde68a`, red `#fecaca`, grey `#e2e8f0`. Use these constants — no ad-hoc colours.

## Chrome (the page event)

- **Header band:** the official `header.png` (embeds `SA-Logo.png` + contact + terracotta rule),
  drawn full-width at the top of every page.
- **Footer band:** `footer.png` (brown rule + registered name + services line + contact), plus the
  **page number** drawn by the engine to the right of the band — *not* baked into the image.
- **Watermark:** the faded **Africa-spark mark** centred behind body text. Draw it from `mark.png`
  at a single low-opacity constant `WATERMARK_OPACITY` (≈`0.05f`) — tune it there, not per report.
- Always `doc.close()` at the end — the footer/page-count is written on document close.
- If a chrome image is absent the document still renders (chrome degrades gracefully).

## Building blocks (add to BrandedPdf)

Document lifecycle:
- `openLandscape(resp, "Title")` / `openPortrait(resp, "Title")` → `Document`. Landscape for wide
  tables (≥6 columns); portrait for narrow/summary-heavy reports.
- `openLandscape(resp, "Title", BrandedPdf.CONFIDENTIAL)` / portrait variant → pins a recipient-only
  confidentiality notice to the footer band of every page (widens the bottom margin to fit). Use for
  any document issued to a named reseller/recipient (statements, invoices).
- `preparePdf(resp, "file-name")` → sets `Content-Disposition` to `file-name-<today>.pdf`. Call
  **before** opening the document.

Content blocks:
- `addSummary(doc, String[] labels, BigDecimal[] values)` → dashboard **stat cards** (terracotta top
  accent, uppercase muted label, large brown value). Use for the figures band at the top.
- `addStatementHeader(doc, name, account, phone, from, to)` → reseller/account panel (name +
  identifiers on the left with terracotta accent, "Statement Period" on the right).
- `addPeriodLine(doc, from, to)` → muted "Period: …" line (renders "All dates" when both null), for
  aggregate reports without a header block.

Tables:
- `table(float[] widths)` → `PdfPTable` (full width, 1 header row; relative-weight widths).
- `headerRow(t, int[] aligns, String... cells)` → brown header row. `aligns` per-column
  `BrandedPdf.L / R / C`.
- `bodyRow(t, rowIndex, int[] aligns, String... cells)` → zebra-striped row (pass a 0-based running
  index for striping).
- `bodyRow(t, rowIndex, int[] aligns, int chipColumn, String... cells)` → same, but `chipColumn`
  renders as a rounded **status pill** (`-1` for none).

Formatting helpers:
- `money(BigDecimal)` → `"10,000"` / `"10,000.50"` (HALF_UP, US grouping).
- `str(Object)` / `s(String)` → null-safe.
- `statusColor(status)` / `statusChipBg(status)` → map status → text colour / pill fill. Suggested
  SMS-domain vocab: `DELIVERED/SENT/ACTIVE/PAID/COMPLETED/APPROVED` → green; `QUEUED/PENDING/SUBMITTED/PROCESSING`
  → amber; `FAILED/REJECTED/BLOCKED/EXPIRED/UNDELIVERED` → red; else grey. Extend the switch as
  status vocab grows.

## Conventions (the "clean" look)

1. **Right-align every numeric/money column** (`R`); left-align text/IDs/dates (`L`); centre status
   pills (`C`). Phone numbers / sender IDs that look numeric stay left-aligned.
2. **Status columns are pills**, not plain text.
3. **Summary figures are stat cards** via `addSummary`, not inline paragraphs.
4. Keep column count sane for the orientation; go landscape once a table gets wide.
5. Don't set fonts/padding/borders per cell — the helpers encode the type scale (header 9pt bold,
   body 8.5pt, card value 15pt brown, labels 7pt uppercase muted).

## Recipe — add a new PDF export

Add a method to `ReportExportService`:

```java
public void pdfSmsDelivery(SmsDeliveryReport r, HttpServletResponse resp) throws Exception {
    BrandedPdf.preparePdf(resp, "sms-delivery");
    Document doc = BrandedPdf.openLandscape(resp, "SMS Delivery Statement");
    // recipient-specific? open with the confidentiality footer instead:
    // Document doc = BrandedPdf.openLandscape(resp, "SMS Delivery Statement", BrandedPdf.CONFIDENTIAL);

    BrandedPdf.addStatementHeader(doc, r.getResellerName(), r.getAccountNo(), r.getPhone(),
            r.getFrom(), r.getTo());
    BrandedPdf.addSummary(doc,
            new String[]{"Messages Sent", "Delivered", "Spend (KES)"},
            new BigDecimal[]{BigDecimal.valueOf(r.getSent()), BigDecimal.valueOf(r.getDelivered()),
                             r.getSpend()});

    int[] aligns = {BrandedPdf.L, BrandedPdf.L, BrandedPdf.R, BrandedPdf.R, BrandedPdf.R, BrandedPdf.C};
    PdfPTable table = BrandedPdf.table(new float[]{1.5f, 3f, 2f, 2f, 2f, 1.8f});
    BrandedPdf.headerRow(table, aligns, "Date", "Campaign", "Sent", "Delivered", "Cost", "Status");

    int i = 0;
    for (SmsDeliveryReport.Row row : r.getRows()) {
        BrandedPdf.bodyRow(table, i++, aligns, /* chipColumn */ 5,
                row.getDate(), row.getCampaign(),
                BrandedPdf.money(BigDecimal.valueOf(row.getSent())),
                BrandedPdf.money(BigDecimal.valueOf(row.getDelivered())),
                BrandedPdf.money(row.getCost()), row.getStatus());
    }
    doc.add(table);
    doc.close();                                 // REQUIRED (footer/page-count written on close)
}
```

Then wire it to a controller endpoint (follow the existing controllers under
`com.spa.smart_gate_springboot` and a `?format=csv|xlsx|pdf` switch where one applies) and a frontend
`responseType: 'blob'` API call (see `frontend/src/instance.js` / the feature slices).

## Verifying output visually

No PDF test suite. To eyeball without booting Spring/DB:

1. Generate the dependency classpath once:
   `./mvnw -o -q dependency:build-classpath -Dmdep.outputFile=/tmp/synq_cp.txt`
2. Write a throwaway `PdfPreviewMain` in package `com.spa.smart_gate_springboot.report` that builds a
   sample DTO and calls the export method, passing a mock `HttpServletResponse` (a
   `java.lang.reflect.Proxy` whose `getOutputStream` returns a `ServletOutputStream` wrapping a
   `FileOutputStream`). Compile only the changed report sources into a temp dir layered ahead of
   `target/classes`, then run it (remember JDK 17).
3. Convert page 1 to PNG: `qlmanage -t -s 2000 -o /tmp /tmp/preview.pdf` and inspect.
4. **Delete the throwaway `PdfPreviewMain` before finishing** — it must not ship.

For a no-Java sanity check of the chrome/layout alone, open
[`cooporate_assets/pdf/_preview.html`](../../../cooporate_assets/pdf/_preview.html) in a browser.

Iterate: render → compare → adjust the `BrandedPdf` helpers → re-render.
