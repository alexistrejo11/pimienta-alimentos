# Account headquarter access follow-ups

2026-09-09 — `AccountHeadquarterAccessIntegrationTest`

No extra follow-ups from this pass.

## 2026-09-11 — Role hardening (ADMIN / MANAGER)

- Non-POS APIs (inventory, employees, user management, etc.) are now **ADMIN-only** at the filter level. MANAGER may use `/users/me`, `/pos/admin/**`, and HQ `pos-settings` / `pos-catalog` (HQ-scoped).
- Former inventory HQ-scoping tests for MANAGER were replaced with **403** assertions.

No extra follow-ups from this pass.

## 2026-09-25 — Director and floor staff

- `DIRECTOR` reads sales for the assigned sede and is denied device revoke.
- `MANAGER` is denied ERP (`/employees`), sales reports, and device revoke. Inventory and POS catalog of the assigned sede stay allowed.
- Closed-shift history is enforced in the shift controller for floor staff, not only in the router.

No extra follow-ups from this pass.
