# API Gaps — Frontend vs. Backend

Document generated: **2026-05-14**

This file tracks cases where the frontend had to make assumptions, work around missing features, or
where a small backend change would unlock a better UX. Items are classified as **limitation**
(functional today but degraded UX) or **missing** (feature cannot be implemented without a backend change).

---

## 1. Attendance — employee name not included in `AttendanceResponse`

**Type:** limitation  
**Affected features:** `AsistenciaHoyModal`, `AsistenciaBusquedaModal`, `EmpleadoAsistenciaCard`

`AttendanceResponse` returns `employeeId` (integer) but no name fields. The global today/search
panels therefore show only the numeric ID, which is not human-friendly.

**Recommended backend change:**  
Add `employeeFirstName` and `employeeLastName` (or a nested `employeeName` string) to
`AttendanceResponse`. Alternatively, expose a lightweight `EmployeeMiniResponse` sub-object.

**Workaround implemented:** display raw `employeeId`. No additional API calls per row to avoid
N+1 requests.

---

## 2. Attendance `startWorkday` — `headquarterId` required but no HQ selector in the UI

**Type:** limitation  
**Affected features:** `EmpleadoAsistenciaCard` check-in form

The `StartWorkdayRequest` schema requires a `headquarterId`. The card currently exposes a plain
number input defaulting to `1`. A real workflow needs either:

- a) a dropdown populated from `GET /api/v1/headquarters` (already available in the app via `SedesService`); or
- b) the backend inferring the HQ from the employee's assignment.

**Recommended backend change:**  
Make `headquarterId` optional and infer it from the employee's primary site if omitted.

**Workaround implemented:** raw number input, default `1`.

---

## 3. `EmployeeListItemResponse` — no `photoUrl`

**Type:** limitation  
**Affected features:** `EmpleadoRowComponent` avatar in the employee list table

The list endpoint returns a lightweight DTO without the photo URL. The list row falls back to
initials-based avatar. Photo is shown only in the full detail view (`EmployeeResponse`).

**Recommended backend change:**  
Add `photoUrl` (nullable/empty string) to `EmployeeListItemResponse` so avatars are consistent
between the list and detail views.

---

## 4. Work schedule — no slot-level IDs in `WorkDayScheduleSlotResponse`

**Type:** limitation  
**Affected features:** `EmpleadoHorarioCard`

The `PUT /work-schedule` endpoint does a full replacement, which the frontend already implements
correctly. However, the GET response does not include slot IDs, so optimistic partial updates
are not possible without a full round-trip. This is acceptable for the current workflow (edit all
slots at once and save).

No action required unless granular slot editing per-day becomes a requirement.

---

## 5. Attendance evidence photos — only URLs returned, no inline thumbnails

**Type:** limitation  
**Affected features:** `EmpleadoAsistenciaCard`, `AsistenciaHoyModal`

Check-in/out evidence photos are returned as presigned S3 URLs. The frontend links out to them
(`<a target="_blank">`). If inline previews are desired, no backend change is needed — the
presigned URL can be used in an `<img>` tag. This was not implemented to keep the table compact;
a lightbox could be added frontend-only.

---

## 6. No pagination controls in `AsistenciaHoyModal`

**Type:** limitation  
**Affected features:** `AsistenciaHoyModal`

The today modal fetches up to 50 records (`size=50`). If a large organization has more than 50
attendances in one day, older records won't appear. The API supports full pagination via
`pageable` params.

**Frontend improvement (no backend change needed):** add a "load more" or page selector to the
modal. Deferred to a future iteration.

---

## Summary table

| # | Feature | Type | Requires backend change? |
|---|---------|------|--------------------------|
| 1 | Employee name in AttendanceResponse | limitation | yes — recommended |
| 2 | HQ selector for check-in | limitation | nice-to-have |
| 3 | Photo in EmployeeListItemResponse | limitation | yes — recommended |
| 4 | Slot IDs in schedule response | limitation | no action needed |
| 5 | Evidence photo thumbnails | limitation | no — frontend only |
| 6 | Pagination in today modal | limitation | no — frontend only |
