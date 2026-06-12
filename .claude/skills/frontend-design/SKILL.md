---
name: frontend-design
description: Synq Africa frontend design system — typography scale, cards, badges, buttons, inputs, and sleek sticky layout for the React + antd + Tailwind reseller portal. Use when building or restyling any frontend screen, component, card, table, form, modal, or layout in frontend/, or when the user asks to make the UI cleaner / sleeker / match the design / fix font sizes / fix spacing. Adapted from the Nineyard Capital portal design language, mapped onto the Synq brand palette.
---

# Synq Africa Frontend Design System

A sleek, editorial design language for the reseller portal: generous whitespace, soft
cards, a clear typographic hierarchy, and restrained use of the brand accent. Ported from
the Nineyard Capital investor portal and recolored to the **Synq Africa** brand.

**Stack:** React 18 + antd 5 + Vite + Tailwind. Tailwind is already wired up
(`frontend/tailwind.config.js`, `frontend/postcss.config.js`, `@tailwind` directives in
`frontend/src/index.css`). Prefer Tailwind utility classes + the component classes below.
When using antd components, restyle via `className`/tokens rather than fighting them.

---

## 1. Brand tokens (source of truth)

Synq's palette already lives in `frontend/src/index.css` `:root` and `tailwind.config.js`.
Use these — do **not** introduce navy/gold from the source design.

| Role | Token | Hex | Notes |
|------|-------|-----|-------|
| Primary brand | `brand` / `saBrown` | `#69472E` | Nav, buttons, active states, headings on light |
| Accent | `brand-accent` / `saOrange` | `#D96C3B` | Sparing — CTAs, highlights, progress, key stat |
| Dark surface | `saDark` | `#13161D` | Dark cards / dark sections |
| Body text (ink) | `ink` | `#334155` | Default paragraph text |
| Muted text | `muted` | `#64748b` | Captions, labels, secondary |
| Surface bg | `surface` | `#fafaf9` | Warm off-white page background |
| Hairline border | `border` | `rgba(0,0,0,0.06)` | Card + divider borders |

**Shadows / radius (the "soft card" feel):**
```
shadow-card: 0 4px 6px -1px rgba(0,0,0,.1), 0 2px 4px -1px rgba(0,0,0,.06)
shadow-lift: 0 20px 25px -5px rgba(0,0,0,.1), 0 10px 10px -5px rgba(0,0,0,.04)
rounded-card: 12px
```

To enable these names, merge into `theme.extend` in `tailwind.config.js`:
```js
extend: {
  colors: {
    primary: '#69472E', accent: { DEFAULT: '#D96C3B', hover: '#b8562a' },
    surface: '#fafaf9', muted: '#64748b', ink: '#334155',
    border: 'rgba(0,0,0,0.06)',
  },
  boxShadow: {
    card: '0 4px 6px -1px rgba(0,0,0,.1), 0 2px 4px -1px rgba(0,0,0,.06)',
    lift: '0 20px 25px -5px rgba(0,0,0,.1), 0 10px 10px -5px rgba(0,0,0,.04)',
  },
  borderRadius: { card: '12px' },
}
```

---

## 2. Typography — the "good font sizes"

This is the part to copy faithfully. Two families, one tight scale.

- **Body / UI:** `DM Sans` (already the project default) — `font-sans`.
- **Headings / display numbers:** a serif gives the editorial feel. The project default is
  fine; if a display serif is wanted, load `Playfair Display` and use it for `h1–h3` and
  big stat numbers only.
- **Line height:** body `1.6`. Headings `leading-tight` / `leading-snug`.

| Use | Classes |
|-----|---------|
| Page title | `text-3xl font-medium text-primary leading-tight` (serif optional) |
| Card / section heading | `text-lg font-medium text-primary leading-snug` |
| Big stat number | `text-4xl font-semibold text-primary` (accent for the hero metric) |
| Inline stat | `text-2xl font-semibold` |
| Body | `text-sm text-ink leading-relaxed` |
| Secondary / caption | `text-xs text-muted` |
| Micro label (over stats) | `text-[10px] uppercase tracking-wider text-muted` |
| Section eyebrow (gold→accent) | `text-xs font-semibold tracking-widest uppercase text-accent` |

**Rule of thumb:** most UI text is `text-sm`. Step up to `text-lg`+ only for headings and
stat numbers. Labels go `text-xs`/`text-[10px]` uppercase. This restraint is what makes it
read as "clean" rather than noisy.

---

## 3. Component classes

Drop these into `frontend/src/index.css` under `@layer components` (recolored to Synq).
Then use the class names in JSX. They compose with antd via `className`.

```css
@layer components {
  /* ── Cards ── */
  .card        { @apply bg-white rounded-card p-6 border border-border shadow-card; }
  .card-hover  { @apply transition-all duration-300 hover:-translate-y-2 hover:shadow-lift; }

  /* ── Buttons ── */
  .btn-primary {
    @apply inline-flex items-center justify-center px-6 py-3 bg-accent text-white text-sm
           font-semibold rounded-md transition-all duration-300
           hover:bg-accent-hover hover:-translate-y-0.5 hover:shadow-lift
           disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none;
  }
  .btn-outline {
    @apply inline-flex items-center justify-center px-6 py-3 border-2 border-primary
           text-primary text-sm font-semibold rounded-md transition-all duration-300
           hover:bg-primary hover:text-white disabled:opacity-50;
  }
  .btn-ghost {
    @apply inline-flex items-center justify-center px-4 py-2 text-sm font-medium text-ink
           rounded-md transition-colors hover:bg-black/5 disabled:opacity-50;
  }

  /* ── Inputs (when not using antd) ── */
  .input {
    @apply block w-full px-4 py-2.5 border border-gray-200 rounded-md text-sm text-ink
           bg-white transition-colors placeholder:text-gray-400
           focus:outline-none focus:ring-2 focus:ring-accent/40 focus:border-accent
           disabled:bg-gray-50 disabled:text-gray-400;
  }

  /* ── Section bits ── */
  .section-tag   { @apply inline-block text-xs font-semibold tracking-widest uppercase text-accent mb-4; }
  .section-title { @apply text-3xl font-medium text-primary leading-tight; }
  .gold-divider  { @apply w-12 h-0.5 mt-4 bg-accent; }
  .stat-number   { @apply text-4xl font-semibold text-primary; }

  /* ── Status badges (pills) ── */
  .badge          { @apply inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium; }
  .badge-pending  { @apply badge bg-amber-50   text-amber-700   ring-1 ring-amber-200; }
  .badge-approved { @apply badge bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200; }
  .badge-active   { @apply badge bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200; }
  .badge-rejected { @apply badge bg-red-50     text-red-700     ring-1 ring-red-200; }
  .badge-open     { @apply badge bg-blue-50    text-blue-700    ring-1 ring-blue-200; }
  .badge-funded   { @apply badge bg-purple-50  text-purple-700  ring-1 ring-purple-200; }
}
```

You also need the `accent-hover` color used above — add `accent: { hover: '#b8562a' }`
(included in the config block in §1).

---

## 4. Card pattern (the reference component)

A card = soft container → header row with title + status pill → optional description →
a metrics strip bordered top/bottom → a progress/CTA footer. Keep gaps at `gap-4`,
padding `p-6`.

```jsx
<div className="card card-hover flex flex-col gap-4 group cursor-pointer">
  {/* Header */}
  <div className="flex items-start justify-between">
    <h3 className="font-medium text-primary text-lg leading-snug group-hover:text-accent transition-colors">
      {title}
    </h3>
    <span className="badge-open ml-3 flex-shrink-0">{status}</span>
  </div>

  {description && (
    <p className="text-sm text-muted leading-relaxed line-clamp-2">{description}</p>
  )}

  {/* Metrics strip — bordered top & bottom */}
  <div className="grid grid-cols-3 gap-4 py-4 border-t border-b border-border">
    <div>
      <p className="text-2xl font-semibold text-accent">{rate}%</p>
      <p className="text-[10px] uppercase tracking-wider text-muted mt-0.5">Rate</p>
    </div>
    {/* …two more stats… */}
  </div>

  {/* Progress */}
  <div>
    <div className="flex justify-between text-xs text-muted mb-1.5">
      <span>{fmt(raised)} sent</span>
      <span className="font-medium text-ink">{pct}%</span>
    </div>
    <div className="w-full bg-gray-100 rounded-full h-1.5">
      <div className="rounded-full h-1.5 bg-accent transition-all duration-500"
           style={{ width: `${Math.min(pct, 100)}%` }} />
    </div>
  </div>
</div>
```

Number formatting: `Number(n||0).toLocaleString('en-KE')`.

---

## 5. Layout — sleek sticky shell

- **Page wrapper:** `min-h-screen flex flex-col bg-surface`.
- **Top nav:** `fixed top-0 inset-x-0 z-50 border-b border-border` with a glass background:
  `style={{ background:'rgba(255,255,255,0.95)', backdropFilter:'blur(10px)' }}`.
  Inner: `max-w-7xl mx-auto px-4 sm:px-6 py-4 flex items-center justify-between`.
- **Content:** `flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 pt-24 sm:pt-28 pb-12`
  (top padding clears the fixed nav).
- **Grids:** card grids use `grid gap-6 sm:grid-cols-2 lg:grid-cols-3`.
- **Active nav link:** `text-accent`; idle `text-ink hover:text-accent`.
- **Mobile:** hamburger → right-side drawer `w-72 max-w-[85vw] bg-white shadow-xl`, with a
  `bg-black/40` scrim. Hide desktop links under `md:`.

Counts/alerts use a small red pill: `min-w-[18px] h-[18px] rounded-full bg-red-600 text-white text-[11px] font-semibold`.

---

## 6. Principles

1. **Whitespace over borders.** Separate with space and one hairline (`border-border`), not boxes inside boxes.
2. **One accent, used sparingly.** Terracotta (`accent`) for the single most important thing per view — the hero stat, the primary CTA, the active link. Everything else is brand brown, ink, or muted.
3. **Type does the hierarchy.** Size + weight + color, not background fills.
4. **Soft, lift on hover.** `rounded-card`, `shadow-card`, `hover:-translate-y-2 hover:shadow-lift` for interactive cards.
5. **Tables:** right-align money, status as a pill, zebra rows via `odd:bg-surface`, header `text-xs uppercase tracking-wider text-muted`.

---

## 7. Applying it

When asked to style/build a screen:
1. If the Tailwind tokens (§1) or `@layer components` block (§3) aren't in the project yet, add them first — they're the foundation.
2. Build with the card pattern (§4) and layout shell (§5).
3. Hold to the type scale (§2) and the one-accent rule (§6).
4. For antd components, keep them but align colors via `className` and antd `theme.token` (`colorPrimary: '#69472E'`).
