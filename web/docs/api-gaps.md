# API Gaps — Frontend vs. Backend

Document generated: **2026-05-14** — updated **2026-09-11**

This file tracks cases where the frontend had to make assumptions, work around missing features, or
where a small backend change would unlock a better UX.

---

## 1. Attendance — employee name not included in `AttendanceResponse`

**Type:** limitation  
**Status:** **RESOLVED (2026-09-11)**

`AttendanceResponse` now includes `employeeFullName`. Modales de asistencia y búsqueda muestran el nombre.

---

## 2. Attendance `startWorkday` — `headquarterId` required but no HQ selector in the UI

**Type:** limitation  
**Status:** **RESOLVED (2026-09-11)**

`EmpleadoAsistenciaCard` usa `app-headquarter-select`; se eliminó el default `headquarterId = 1`.

---

## 3. `EmployeeListItemResponse` — no `photoUrl`

**Type:** limitation  
**Affected features:** `EmpleadoRowComponent` avatar in the employee list table

The list endpoint returns a lightweight DTO without the photo URL. The list row falls back to
initials-based avatar.

**Recommended backend change:** Add `photoUrl` to `EmployeeListItemResponse`.

---

## 4. Work schedule — no slot-level IDs in `WorkDayScheduleSlotResponse`

**Type:** limitation — no action required for current workflow.

---

## 5. Attendance evidence photos — only URLs returned

**Type:** limitation — frontend-only thumbnails possible; not implemented.

---

## 6. No pagination controls in `AsistenciaHoyModal`

**Type:** limitation — deferred.

---

## 7. CRM clients — no list API for project `clientId`

**Type:** missing  
**Status:** **RESOLVED (2026-09-11)**

Added `GET/POST /api/v1/clients` and `app-client-select` on project create form.

---

## Summary table

| # | Feature | Type | Status |
|---|---------|------|--------|
| 1 | Employee name in AttendanceResponse | limitation | resolved |
| 2 | HQ selector for check-in | limitation | resolved |
| 3 | Photo in EmployeeListItemResponse | limitation | open |
| 4 | Slot IDs in schedule response | limitation | N/A |
| 5 | Evidence photo thumbnails | limitation | frontend only |
| 6 | Pagination in today modal | limitation | deferred |
| 7 | CRM clients API | missing | resolved |
