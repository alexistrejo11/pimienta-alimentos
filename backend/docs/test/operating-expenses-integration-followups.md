# OperatingExpenses integration follow-ups

## 2026-10-14 — Phase 2

- The global servlet multipart cap stays at 10 MB so existing file and photo uploads are unchanged. OperatingExpense creation enforces 5 MB per image and 6 MB across the files in that request.
- `VALIDATION_FAILED` still resolves to the shared Spanish sentence. The specific reason (`UNSUPPORTED_TYPE`, `FILE_TOO_LARGE`, `TOTAL_TOO_LARGE`, `TOO_MANY_ATTACHMENTS`) is in `context.reason`.
- `DIRECTOR` is global only inside the operating-expense access policy. `HeadquarterAccessService` still treats that role as sede-scoped for other modules.
- Hibernate 7 maps `List<String>` as `jsonb`. `V22` creates `attachment_keys` as `jsonb`.
- Soft-deleted operating expenses keep their S3 objects. There is no purge job.

No extra follow-ups from this pass.

## 2026-10-22 — Summary breakdown

- `byDay` omits calendar days with no matching expenses. The gastos page zero-fills a range of at most 31 days.
- A tie on amount uses the higher count, then the category name.

No extra follow-ups from this pass.
