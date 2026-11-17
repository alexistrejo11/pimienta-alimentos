# Employees module — integration test follow-ups

## Notes from employee–contract decoupling (2026-11-16)

- Employee lifecycle is now a direct `PATCH /api/v1/employees/{id}/status` mutation (`ACTIVE` | `INACTIVE` | `RESIGNED` | `TERMINATED` | `DOWN`). Contract create only validates employee existence.
- Historical statuses migrated in `V31__employee_status_lifecycle.sql` (`FIRED`→`TERMINATED`; onboarding/leave statuses→`INACTIVE`).
- Attendance / payroll / task assignment still do not gate on employee status (pre-existing).
- Update form clears `terminationDate` when moving to `ACTIVE`/`INACTIVE`; exit statuses stamp today when missing.
- **No extra follow-ups from this pass** beyond the known ungated attendance/assignment behavior.
