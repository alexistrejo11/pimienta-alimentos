---
name: pimienta-frontend-ui
description: >-
  Visual UI rules for the Pimienta Alimentos Angular app: Tailwind v4 theme tokens,
  typography (Manrope / Work Sans), Material Symbols, layout and component
  patterns (auth, marketing, workspace). Apply when styling or building pages,
  components, or templates in web/src.
---

# Pimienta frontend — visual UI conventions

Stack: **Angular** (standalone), **Tailwind CSS v4** (`@import 'tailwindcss'`), fonts and icons loaded from `src/index.html`; component-level CSS only when a pattern is repeated.

Source of truth for tokens is **`src/styles.css`**. Do not invent brand colors. If a token is missing, add it to `@theme` / `html.dark` first, then use it.

## Design tokens (`src/styles.css`)

### Light (`@theme`)

| Token | Hex | Role |
|-------|-----|------|
| `--color-primary` | `#af101a` | Brand red. Primary CTAs only. |
| `--color-primary-container` | `#e57373` | Hover / softer red fill. |
| `--color-on-primary` | `#ffffff` | Text/icons on primary. |
| `--color-accent` | `#80c030` | Secondary highlight ( Distintivo H, active nav underline, trust). **Not** primary CTAs. |
| `--color-accent-container` | `#d4edaa` | Accent wash. |
| `--color-on-accent` | `#1a2e0a` | Text on accent fills. |
| `--color-background` | `#f7f6f4` | Page canvas. |
| `--color-surface` | `#f3f2ef` | Main content surface. |
| `--color-surface-container-lowest` | `#ffffff` | Highest lift / innermost panel. |
| `--color-surface-container-low` | `#f0efec` | |
| `--color-surface-container` | `#ebe9e5` | Inputs, recessed wells. |
| `--color-surface-container-high` | `#e4e2dd` | |
| `--color-surface-container-highest` | `#dbd8d2` | |
| `--color-on-background` / `--color-on-surface` | `#1a1c1c` | Body text. |
| `--color-on-surface-variant` | `#5f6367` | Muted / secondary text. |
| `--color-outline-variant` | `#8e918f` | Ghost borders only (low opacity). |
| `--color-secondary` | `#6f7478` | Neutral secondary. |
| `--color-error` | `#ba1a1a` | Errors. |
| `--color-primary-fixed` | `#ffdad6` | Tinted red wash. |
| `--color-on-primary-fixed` | `#410003` | Text on primary-fixed. |

Prefer **`text-[var(--color-…)]`**, **`bg-[var(--color-…)]`**, or Tailwind aliases generated from `@theme` (`bg-primary`, `text-on-surface`).

### Dark (`html.dark`)

Theme is toggled at runtime via `ThemeService` (`localStorage` key `pimienta-theme`). On first visit, `prefers-color-scheme` applies; an inline script in `index.html` sets `html.dark` before paint to avoid flash.

`html.dark` remaps **surfaces and text only**. Brand red and accent stay the same.

| Token | Dark hex |
|-------|----------|
| `--color-background` / `--color-surface-container-lowest` | `#0c0a09` |
| `--color-surface` / `--color-surface-container-low` | `#1c1917` |
| `--color-surface-container` | `#292524` |
| `--color-surface-container-high` | `#44403c` |
| `--color-surface-container-highest` | `#57534e` |
| `--color-on-background` / `--color-on-surface` | `#fafaf9` |
| `--color-on-surface-variant` | `#a8a29e` |
| `--color-outline-variant` | `#78716c` |
| `--color-secondary` | `#a8a29e` |

## Typography

- **Headlines / titles:** `font-headline` (**Manrope** 400/600/700/800). `h1`–`h4` and strong page titles.
- **Body / UI:** **Work Sans** 300/400/500/600 via `body` / `font-body`.
- Muted copy: `--color-on-surface-variant`. Do not introduce a third brand typeface.

## Icons

- **Material Symbols Outlined** only (`index.html`).
- Default: **`material-symbols-outlined`**. Filled: **`material-symbols-outlined--filled`**.

## Layout and spacing

- Full-height shells: `min-h-screen`, flex column, `max-w-*` + `px-6` / `px-8`.
- Auth: centered column `max-w-[440px]`–`max-w-[480px]`, `gap-8`, padding `p-8` / `md:p-12`.
- **No-line rule:** section with **surface-tier shifts**, not 1px solid borders. Ghost border (`outline-variant` at ~30% opacity) only when a control needs an accessible edge.

## Component patterns

- **Primary CTA:** `bg-primary` / `bg-[var(--color-primary)]`, `text-on-primary`, `rounded-lg` or `rounded-xl`, `font-bold`. Hover to `primary-container`.
- **Secondary / outline:** light/dark surface fill, ghost border. Not a second red button.
- **Inputs:** `rounded-lg`, fill `surface-container` (auth uses `.auth-field`). Focus ring `color-mix` with primary at ~20%. No heavy chrome borders.
- **Auth chrome:** `.auth-shell`, `.auth-card` (frosted **surface**, not opaque white cards on dark).
- **Workspace:** sidebar + main on `--color-surface` / `text-on-surface`.

## Theme toggle

- Component: `shared/ui/theme-toggle/` (sun/moon icon, calls `ThemeService`).
- Landing: nav in `pages/home/home/home.html`.
- Workspace: footer in `shared/workspace/workspace-footer/`.

## Shared UI utilities (`styles.css` `@layer components`)

- `.ui-panel` — elevated card/panel (replaces `bg-white dark:bg-stone-950` pairs).
- `.ui-panel-muted` — recessed table header / secondary surface.
- `.ui-chrome` — sidebar/footer chrome.
- `.ui-badge-success` / `-warning` / `-danger` / `-info` / `-neutral` — status pills.

Prefer these over Tailwind `stone-*` + `dark:` duplication.

## What to avoid

- `stone-*`, `bg-white` + `dark:bg-stone-*` in workspace templates — use semantic tokens or `.ui-*` classes.
- Ad-hoc hex for core UI (purple/teal Material defaults, “AI dashboard” palettes, neon gradients).
- Generic **elevated card grids**: stacked `shadow-lg` cards, thick borders, and decorative Material cards as the default layout. Depth comes from **surface tiers** and spacing.
- Mixing icon families.
- Pasting full HTML documents or CDN Tailwind into component templates.
- Inline `<style>` when the rule belongs in `styles.css` or the component `.css`.

Auth and marketing may use a single contained panel (`.auth-card`, philosophy blocks). That is not a license to wrap every list row in a card.

## File touchpoints

| Area | Location |
|------|----------|
| Global theme | `src/styles.css` (`@theme` + `html.dark` + `.ui-*`) |
| Theme init script | `src/index.html` |
| Theme service | `src/app/core/theme/theme.service.ts` |
| Landing layout | `src/app/pages/home/home/home.css` |
| Page-specific | `*.html` + optional `*.css` |

When adding large new UI surfaces, **mirror existing files** (`pages/auth/login`, `pages/home/home`, workspace pages) before inventing new patterns.
