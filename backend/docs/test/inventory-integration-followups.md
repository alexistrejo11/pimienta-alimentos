# Inventory integration follow-ups

## 2026-09-14 — HQ inventory boundary refactor

- **Count sessions at warehouse locations:** Sessions scoped by `storage_locations.headquarter_id`; warehouse rows with `headquarter_id IS NULL` remain ADMIN-only via `requireOwnedLocation`. Managers counting non-HQ warehouse stock is not supported until warehouse ownership is modeled per HQ.
- **POS operator inventory writes:** `POST /inventory/transactions/sale` is now ADMIN/MANAGER only; cafeteria stock reduction should flow through device `SALE_CONFIRMED` only.
- **Tablet waste/restock:** `WASTE_RECORDED` / `RESTOCK_RECORDED` are ledger-only after Phase 2; HQ scrap uses `POST /inventory/transactions/scrap`.

No extra follow-ups from RBAC/count-session test pass beyond the items above.
