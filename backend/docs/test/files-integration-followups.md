# Files module — integration test follow-ups

## Notes from initial `FileIntegrationTest` pass (2026-06-04)

- **`FileAssetNotFoundException`** extends plain `RuntimeException`, not `ResourceNotFoundException` / `PimientaException`. Missing assets return **500** with `INTERNAL_ERROR` instead of **404** with a stable `FILE_ASSET_NOT_FOUND` code. Consider aligning with other modules.
- Tests use **`StubFileStoragePort`** (`@Primary` `FileStoragePort`) so **no S3 `putObject` / `deleteObject` / presign** runs; uploads use a few-byte `MockMultipartFile` only.
- **`GET /api/v1/files/resources`** has no `getById` or `delete` endpoints (by design); management covers full admin lifecycle.

## 2026-09-11 — Role hardening

- `/api/v1/files/resources/**` is **ADMIN-only** (MANAGER no longer allowed). Happy-path IT uses ADMIN; MANAGER expects **403**.
