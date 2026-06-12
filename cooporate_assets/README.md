# Synq Africa — Corporate Assets

Brand system for branded PDFs and documents. Built on the **official Synq Africa logo** pulled from
[synqafrica.co.ke](https://synqafrica.co.ke), with the palette confirmed against the live app theme
([`frontend/tailwind.config.js`](../frontend/tailwind.config.js)).

## What's here

```
cooporate_assets/
├── logos/   # official Synq Africa logo files (do not redraw — these are the brand)
└── pdf/     # branded-PDF chrome — letterhead header + footer bands (source of truth)
```

## How to preview

Open in a browser:

- [`logos/_preview.html`](logos/_preview.html) — every logo variant, light + dark
- [`pdf/_preview.html`](pdf/_preview.html) — a mock *SMS Delivery Statement* showing the header band,
  stat cards, zebra table, status pills, watermark, and footer band rendered together

## Brand fundamentals

| | |
|---|---|
| **Company** | Synq Africa Holdings Limited |
| **Tagline** | *Value Added Mobile Solutions* |
| **Logo / mark** | A multi-colour **spark / firework** of radiating strokes that forms the **silhouette of Africa** — energy + continental reach. Pulled from the website; **never redraw it.** |
| **Wordmark** | "**Synq**" in brown · "**Africa**" in terracotta |
| **Primary color** | Brown `#69472E` — wordmark, footer rule, table headers, primary surfaces |
| **Accent color** | Terracotta `#D96C3B` (hover `#b8562a`) — CTAs, header hairline rule, "Africa" wordmark |
| **Dark surface** | `#13161D` — reversed logo backgrounds, slide title pages |
| **Cream / off-white** | `#F2EFEA` light tile · `#fafaf9` page surface · `#FAF8F5` zebra stripe |
| **Ink / Muted text** | `#334155` body · `#64748b` muted labels |
| **Wordmark / heading font** | Geometric sans — Poppins / Montserrat (system-sans fallback) |
| **Body / secondary font** | Helvetica Neue / system sans |
| **Status colours** | green `#047857`/`#a7f3d0` · amber `#b45309`/`#fde68a` · red `#b91c1c`/`#fecaca` |

> The logo's spark spans more hues than the two brand colours (golds, reds, greens) — that gradient
> is the mark's own palette; for **document chrome and type** stick to brown + terracotta + the
> supporting greys above. These are the same tokens the app ships (`saBrown`, `saOrange`, `saDark`,
> `primary`, `accent` in the Tailwind config) — keep brand and product in lockstep; don't invent hexes.

## 1. Logos — `logos/`

Official files. **Do not recreate or trace the mark** — use these as-is.

| File | Source | Use it for |
|---|---|---|
| `SA-Logo.png` | `wp-content/uploads/2024/06/SA-Logo.png` | Primary horizontal logo on **light** backgrounds — letterhead, web header, PDF header band |
| `SA-Logo-W.png` | `wp-content/uploads/2024/06/SA-Logo-W.png` | **Reversed** logo for **dark** backgrounds (`#13161D`) |
| `synq-africa-mark.png` | app `sync-logo.png` (what the synqafrica subdomain renders) | Square / stacked lockup — app icon, avatar, and the **faded document watermark** |

## 2. Branded PDF chrome — `pdf/`

The letterhead **header** and **footer** bands rendered at the top and bottom of every branded PDF
page. SVGs are the **source of truth**; `header.svg` embeds the official `SA-Logo.png`.

| File | What it is |
|---|---|
| `pdf/header.svg` | Full-width letterhead band — official logo + contact, terracotta hairline rule |
| `pdf/footer.svg` | Full-width footer band — brown rule, registered name, services line, contact |

**Convention (matches the codebase):** brand-asset changes originate here in `cooporate_assets/` and
are **mirrored into** [`src/main/resources/branding/`](../src/main/resources/) as the PNGs the PDF
engine loads from the classpath. Export each SVG to PNG before mirroring:

```bash
# rsvg-convert resolves the relative <image href> to the logo; Inkscape/Affinity work too:
rsvg-convert -w 2000 cooporate_assets/pdf/header.svg > src/main/resources/branding/header.png
rsvg-convert -w 2000 cooporate_assets/pdf/footer.svg > src/main/resources/branding/footer.png
```

The document body (stat cards, zebra tables, status pills, page numbers, watermark) is owned by the
**`branded-pdf` skill** — see [`.claude/skills/branded-pdf/SKILL.md`](../.claude/skills/branded-pdf/SKILL.md).
That skill is the entry point whenever you add or restyle a PDF export. Don't hand-roll fonts,
colours, or tables in a report — reuse the shared chrome so every document matches.

## Conventions

- **The logo is fixed art** — never redraw, recolour, or trace it. Need a new size? Re-export from
  the official PNG (or ask the website owner for the vector).
- **PDF chrome SVGs are source of truth**; the PNGs in `resources/branding/` are generated from them
  — never hand-edit the PNGs.
- **All previews** are named `_preview.html` and live beside the assets they preview.
- **Brand tokens** mirror the app's Tailwind config — if a hex moves, move it in both places.
