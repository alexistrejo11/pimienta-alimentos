# Inventory module — integration test follow-ups

## Notes from InventoryIntegrationTest (initial pass)

- **Location `occupiedCapacity` vs inventory saves:** Domain `Inventory.addStock` / `removeStock` mutates the associated `StorageLocation`, but persistence originally only wrote the `inventory_stock` row, so `StorageLocation.removeStock` during a later **sale** could see a stale `occupiedCapacity` from the database. `InventoryRepositoryImpl.save` now persists the embedded location after saving inventory so stock and capacity stay aligned.

- **Purchase / sale / transfer auto-complete:** `InventoryTransactionManagementUseCasesImpl` finishes many transaction types with `finishCompleted`, so the HTTP API often returns **`COMPLETED`** immediately. Endpoints `POST .../transactions/{id}/submit`, `approve`, and `complete` are then mostly relevant for other lifecycles or future drafts; calling **`POST .../cancel`** on a completed purchase returns **400** `INVALID_ARGUMENT` (domain guard), which the tests lock in as current behavior.
- **Stock in blocked locations:** Creating initial stock in a **blocked** location surfaces **`BusinessValidationException`** → **`VALIDATION_FAILED`** (HTTP 400), not a storage-specific code; worth a product pass if APIs should distinguish “blocked location” from generic validation.

No extra blockers from this pass.

## POS B1 (2026-09-08)

- Barcode uniqueness now returns `ITEM_BARCODE_ALREADY_EXISTS` (409), including soft-deleted rows (D5). Covered by `HeadquarterPosCatalogIntegrationTest`.
- `StorageLocationResponse` gained optional `headquarterId` for `LocationType.POS` rows.
- No extra follow-ups from this pass.
