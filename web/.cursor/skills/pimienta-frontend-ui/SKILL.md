---
name: pimienta-frontend-ui
description: >-
  Visual UI rules for the Pimienta Alimentos Angular app: Tailwind v4 theme tokens,
  typography (Manrope / Work Sans), Material Symbols, layout and component
  patterns (auth cards, marketing sections, workspace shell). Apply when styling
  or building new pages, components, or templates in frontend/src.
---

# Pimienta frontend — visual UI conventions

Stack: **Angular** (standalone), **Tailwind CSS v4** (`@import 'tailwindcss'`), fonts and icons loaded from `src/index.html`; component-level CSS only when a pattern is repeated (e.g. `.glass-panel` on auth pages).

## Design tokens (`src/styles.css` `@theme`)

- **Brand red (primary):** `--color-primary` (#af101a), `--color-primary-container` (#d32f2f), `--color-on-primary` (#ffffff).
- **Surfaces:** `--color-background`, `--color-surface`, `--color-surface-container-lowest` through `--color-surface-container-highest` (light gray scale for cards and sections).
- **Text:** `--color-on-background`, `--color-on-surface`, `--color-on-surface-variant` (muted body/secondary).
- **Other:** `--color-secondary`, `--color-outline-variant`, `--color-error`, `--color-primary-fixed`, `--color-on-primary-fixed` (tinted surfaces and accents).
- Prefer **`text-[var(--color-…)]`**, **`bg-[var(--color-…)]`**, or matching **Tailwind theme aliases** where they exist (e.g. `bg-primary`, `text-on-primary` if generated from `@theme`).

Do **not** introduce ad-hoc hex colors for core UI; extend `@theme` in `styles.css` if a new semantic token is needed.

## Typography

- **Headlines / titles:** `font-headline` (Manrope). Use for `h1`–`h4` and strong page/section titles.
- **Body / UI:** default (Work Sans via `body`); use `font-body` explicitly when needed for consistency in custom blocks.
- **Hierarchy:** large hero titles may use `text-[clamp(...)]`, `font-extrabold`, tight `tracking`; supporting copy uses `text-sm` / `text-xl` and muted color via `--color-on-surface-variant` or `text-stone-500` where appropriate.

## Icons

- **Material Symbols Outlined** only (linked in `index.html`).
- Default icon class: **`material-symbols-outlined`** (defined in `@layer components` in `styles.css`).
- Filled variant when needed: **`material-symbols-outlined--filled`** (e.g. small trust badges).

## Layout and spacing

- **Page shell:** full-height layouts use `min-h-screen`, flex column, `max-w-*` + horizontal padding (`px-6`, `px-8`, `max-w-7xl` / `max-w-[80rem]` for marketing).
- **Auth-style pages:** centered column `max-w-[440px]`–`max-w-[480px]`, vertical rhythm `gap-8`, card `p-8` / `md:p-12`.
- **Responsive:** use breakpoints (`sm:`, `md:`, `lg:`) for nav visibility, grids, and stacking; prefer `flex` / `grid` with `gap-*` over manual margins for groups.

## Components patterns

- **Primary button / CTA:** red fill `bg-[var(--color-primary)]` or `bg-primary`, `text-[var(--color-on-primary)]`, `rounded-lg` or `rounded-xl`, `font-bold`, hover to `primary-container` or slight motion (`hover:-translate-y-0.5`, `shadow`).
- **Secondary / outline:** border + `bg-surface-container-lowest` or light surface, hover darken surface.
- **Inputs:** `rounded-lg`, `bg-stone-100` (or surface-container), `focus:ring-2` with primary tint `focus:ring-[var(--color-primary)]/20`, no heavy borders unless error state.
- **Cards:** `rounded-xl`, subtle border `border-stone-200/50`, soft shadow `shadow-[0_8px_30px_rgb(0,0,0,0.04)]`; optional top accent bar `h-1` gradient primary → primary-container.
- **“Glass” panels (auth):** class **`glass-panel`** in component CSS: semi-transparent white + `backdrop-filter: blur(12px)` — use sparingly for floating cards over imagery.
- **Hero / marketing sections:** gradient overlays with `color-mix(in srgb, …)` or `from-[var(--color-surface)]`; imagery with `object-cover`, optional `opacity` / `grayscale` for background layers.
- **Workspace app shell:** sidebar offset + main (`ml-64` pattern in workspace); keep content readable on `--color-surface` / `text-on-surface`.

## States and feedback

- **Errors:** `--color-error` or Tailwind red scale; alerts `rounded-lg`, `border`, `bg-red-50`, `text-red-900`, optional `role="alert"`.
- **Loading / disabled:** `disabled:opacity-60`, `disabled:cursor-not-allowed`; swap label text (“Enviando…”) rather than silent buttons.

## Content and accessibility

- **Language:** UI copy is predominantly **Spanish**; keep tone professional (institutional / corporate).
- **Semantics:** use real headings (`h1` once per view when possible), `label` + `for` tied to controls, meaningful `alt` on non-decorative images (empty `alt=""` acceptable for decorative backgrounds).

## What to avoid

- Pasting full HTML documents (`<html>`, `<head>`, CDN Tailwind) into **component templates** — use root layout, `styles.css`, and `index.html` assets only.
- Inline `<style>` blocks in templates when the rule can live in **`styles.css`** (`@layer`) or the component’s **`.css`** file.
- Mixing unrelated icon sets (stick to Material Symbols as linked).

## File touchpoints

| Area            | Location |
|-----------------|----------|
| Global theme    | `frontend/src/styles.css` |
| Entry fonts     | `frontend/src/index.html` |
| Page-specific   | `*.html` + optional `*.css` next to the component |

When adding large new UI surfaces, **mirror existing files** (`pages/auth/register`, `pages/home/home-landing`) before inventing new patterns.
