# Supplier integration follow-ups

## 2026-09-25 — `SupplierIntegrationTest`

- Scoped staff must include every assigned headquarter in `headquarterIds` on write; empty assignment yields no suppliers in list.

No extra follow-ups from this pass.

## 2026-09-27 — person, brand, active per headquarter

- Write payload is `headquarters[{headquarterId, active}]`. Inactive links still match the headquarter list filter so a paused person can be turned back on.
- Scoped staff must still include every assigned headquarter, even when `active` is false.

No extra follow-ups from this pass.
