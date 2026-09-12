---
name: pimienta-pos-ui
description: >-
  Visual UI rules for the Android POS: same Pimienta tokens as the Angular web app
  (brand red, surfaces, Manrope / Work Sans), dark-first workspace palette, and
  operational layout (rows and surface splits, not generic Material cards).
  Apply when changing Compose theme, colors, typography, or POS screens.
---

# Pimienta POS — visual UI conventions

Match **web** tokens in `web/src/styles.css`. Compose mappings live in `ui/theme/`. Do not use Material dynamic color or the default purple/teal scaffold.

## Color (same hex as Angular)

Light and dark values are declared in `Color.kt` / `Theme.kt`.

- **Primary CTA:** `#af101a` on `#ffffff`. Hover/container `#e57373`. Primary stays red in dark mode (web does not remap it).
- **Accent (not CTAs):** `#80c030`, container `#d4edaa`, on-accent `#1a2e0a`.
- **Light canvas:** background `#f7f6f4`, surface `#f3f2ef`, text `#1a1c1c`, muted `#5f6367`.
- **Dark canvas (workspace):** background `#0c0a09`, surface `#1c1917`, containers `#292524` / `#44403c` / `#57534e`, text `#fafaf9`, muted `#a8a29e`.
- **Error:** `#ba1a1a`.
- Paint the **window** (status bar, nav bar, `windowBackground`) from the active scheme. A Light XML theme parent will otherwise leave a white flash behind Compose.

Debug builds may toggle light/dark; the window and `Surface` must both follow that flag.

## Typography

- **Titles:** Manrope (`R.font.manrope`) — bold / extrabold headlines.
- **Body, labels, buttons:** Work Sans (`R.font.work_sans`).
- Fonts are bundled under `res/font/` for offline POS. Do not switch to the device default family.

## Layout (operational, not marketing)

- Landscape split: catalog vs cart/cobro. Separate regions with **surface color**, not a stacked card for each product.
- Product lines are **rows** (name, meta, price, action). Do not wrap each item in `Card` / `ElevatedCard` / large rounded “dashboard” tiles with drop shadows.
- Compact controls: small corner radius (`extraSmall` / ~8.dp `rounded-lg` for buttons). Primary actions use brand red fill; unselected actions are flat with a ghost outline at most.
- Inputs (search): filled `surfaceContainer`, no heavy outline; focus uses primary at low opacity.
- Copy can stay operational English or Spanish per existing screens; do not restyle with marketing glass heroes on the sale floor.

## What to avoid

- Generic AI/Material starter UI: purple primary, white scaffold, elevated card lists, pill-heavy dashboards.
- Changing primary to mint/green in dark mode.
- Leaving `Sale` (or any root) without a scheme `background` — transparent Compose shows the light window.
---
