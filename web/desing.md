# Design System Strategy: The Precision Kitchen

## 1. Overview & Creative North Star

The Creative North Star for this design system is **"The Precision Kitchen."**

In the B2B world of hospitals, schools, and industrial dining, excellence is measured by the intersection of clinical hygiene and culinary warmth. This design system moves away from the generic "corporate template" and instead adopts a **High-End Editorial** aesthetic. We achieve this through intentional asymmetry, expansive white space, and a sophisticated layering of monochromatic surfaces. The goal is to make the user feel they are interacting with a service that is as meticulously organized as a Michelin-star kitchen and as reliable as a surgical suite.

By breaking the traditional grid with overlapping elements and high-contrast typography, we convey a brand that is authoritative, modern, and deeply invested in the quality of care.

---

## 2. Implementation (codebase)

The marketing site lives in the **Angular** app under `frontend/src/app/home/`: standalone components, child routes, and **Tailwind CSS v4** (`@theme` tokens in `src/styles.css` plus utility classes in templates).

| Area | Role |
|------|------|
| `home.html` + `site-nav` / `site-footer` | Shell layout and shared chrome. |
| `home-landing` | Main page sections (hero, nosotros, historia, filosofía, contacto) and contact form (`HttpClient` → `contact-endpoint.ts`). |
| `portal-page`, `aviso-privacidad-page`, `terminos-servicio-page`, `calidad-higienica-page` | Former standalone HTML pages as routed components. |
| `landing-nav-state` | Scroll-spy state for active nav while on `/`. |

**Conventions**

- **Tokens** are declared in `src/styles.css` via `@theme` (aligned with the former `:root` palette).
- **External assets**: Google Fonts and Material Symbols are linked from `src/index.html`; hero/about images use the same remote URLs as before (`brand.ts`).

---

## 3. Colors

Our palette is rooted in the "Pimienta Alimentos" heritage: a vibrant, appetizing red balanced by a foundation of clinical whites and charcoal grays.

- **Primary (`--color-primary`, `#af101a`)**: Used sparingly for high-impact CTAs and critical brand moments. It represents the "Pimienta" (Pepper)—the passion and heat behind the service.

- **Neutral foundation**: We use `--color-surface-container-lowest` (`#ffffff` in light theme) for primary content areas to emphasize hygiene and cleanliness.

- **The "No-Line" Rule**: Traditional 1px solid borders are strictly prohibited for sectioning. Boundaries must be defined solely through background color shifts. For example, a card on `--color-surface-container-lowest` should sit on a page body (`--color-surface`) or a section background (`--color-surface-container-low`). This creates a seamless, high-end feel.

- **Surface hierarchy & nesting**: Treat the UI as a series of physical layers. Use the hierarchy from `--color-surface-container-low` to `--color-surface-container-highest` to define depth. An inner container should always be a tier higher or lower than its parent to establish a visual relationship without needing a stroke.

- **The "Glass & Gradient" Rule**: To provide visual "soul," use subtle linear gradients on hero overlays or main CTAs where appropriate (e.g. `linear-gradient` from surface to transparent). For floating navigation, implement **glassmorphism** in CSS: semi-transparent background plus `backdrop-filter: blur(...)` (and `-webkit-backdrop-filter` for Safari)—not a framework utility.

- **Dark theme**: Prefer **system preference** via `@media (prefers-color-scheme: dark)` adjusting the same token names, so we do not require a manual light/dark class on `<html>` unless product asks for a toggle later.

---

## 4. Typography

This system utilizes a dual-font strategy to balance corporate authority with modern legibility.

- **Display & headlines (`--font-headline`, Manrope)**: A geometric sans-serif that feels architectural and precise. Large, bold headers convey confidence and scale.

- **Body & labels (`--font-body`, Work Sans)**: A highly legible typeface optimized for screen readability. It is used for all functional text, ensuring that nutritional data, schedules, and reports are easily digestible for professionals in high-stress environments like hospitals.

- **Editorial hierarchy**: Use a high contrast between display sizes and body sizes. Section titles at roughly `2.25rem` with heavy weight (`800`) read as editorial display, not mere bold body copy.

---

## 5. Elevation & Depth

Depth in this design system is an expression of organization and hygiene.

- **The layering principle**: Avoid heavy drop shadows as a default. Instead, stack your surface tiers. A card on `--color-surface-container-lowest` placed on `--color-surface-container-low` creates a "soft lift" that feels integrated and natural.

- **Ambient shadows**: When an element must float (e.g. hero actions or cards), use diffused shadows with a large blur and low opacity. Tint shadow toward `--color-on-surface` rather than pure gray when possible.

- **The "ghost border" fallback**: If a container needs a boundary for accessibility, use a 1px stroke using `--color-outline-variant` at **low opacity** (e.g. `color-mix(in srgb, var(--color-outline-variant) 30%, transparent)`). It should be barely perceptible.

- **Glassmorphism**: The fixed nav (`.site-nav`) uses semi-transparent white (or dark) plus `backdrop-filter` for a frosted overlay. Keep blur radius moderate (e.g. `12px`) so content behind remains subtly visible.

---

## 6. Components (CSS mapping)

### Buttons

- **Primary**: `.btn.btn--primary` — `--color-primary` background, `--color-on-primary` text, radius `--radius-lg` (`0.25rem`) for compact actions; hero uses `.btn--hero` and `.btn--hero-primary` / `.btn--hero-secondary` with `--radius-xl`.

- **Secondary / outline**: Hero secondary uses surface background and a ghost border derived from `--color-outline-variant`.

- **Tertiary**: Text links in the nav use `.site-nav__link` with primary color on active/hover—reserve for navigation, not dense button rows.

### Input fields

- **Style**: `.form__input` / `.form__textarea` — `--color-surface-container-high` fill, no heavy border; on focus, a soft ring using `box-shadow` + `color-mix` with primary (ghost focus, not a hard outline).

- **Typography**: Labels use `.form__label` (uppercase, tracked); inputs use body font at `1rem`.

### Cards & lists

- **The "no-divider" rule**: Avoid horizontal rules between list items; use spacing (`gap`) or alternating surfaces. The about section meta area uses a single top border on the stats row only where separation is needed—keep it rare.

- **Cards**: Philosophy cards use `--radius-3xl` (`1.5rem`) for large containers; bento layout is implemented with CSS Grid (`.bento`) and explicit placement at `min-width: 768px`.

### Selection & status

- **Chips**: Hero eyebrow uses pill styling (`border-radius: 9999px`) and primary at low mix opacity for background.

- **Status indicators**: For hygiene or quality alerts, use `--color-error` (`#ba1a1a`) with high-contrast labels—do not dilute with non-semantic colors.

---

## 7. Do's and Don'ts

### Do

- **DO** use white space as a structural element. If a section feels crowded, increase padding rather than adding a border.

- **DO** align typography to a consistent vertical rhythm (section padding `6rem`, container horizontal padding `2rem`).

- **DO** use imagery of fresh ingredients and sterile environments behind glassmorphic overlays to reinforce the brand's commitment to quality.

- **DO** keep HTML and CSS in separate files; add new sections as new blocks in `styles.css` rather than inline styles in `main.html`.

### Don't

- **DON'T** add a utility CSS framework for this landing unless the product scope changes—tokens and components should stay maintainable in one stylesheet.

- **DON'T** use 100% opaque, high-contrast borders for every container. It breaks the editorial flow and feels "cheap."

- **DON'T** use harsh, unlayered shadows. Prefer surface tiers and ambient shadow when shadow is needed.

- **DON'T** over-apply the vibrant red. It is a "spice"—use it to draw attention, not to overwhelm the interface. The dominant impression should be the interplay of white and charcoal neutrals.

- **DON'T** reuse a single radius for everything. Mix `--radius-xl` / `--radius-3xl` for large containers and `--radius-lg` for interactive controls to create visual interest.
