## Problem / Vision

Pimienta Alimentos needs consistent operational records that staff can trust across people, commercial work, company locations, inventory, and cafeterias. This service provides the shared source of those records and coordinates the information that the staff workspace and cafeteria devices need.

## Users / Actors

- **Administrative staff** create and maintain the operational records needed for daily work.
- **Managers** work with the company locations and records assigned to them.
- **Administrators** approve access and manage company-wide operations.
- **Cafeteria devices** register their identity, obtain approved operational data, and report completed activity.

## Scope

**In scope:** staff access, users, employees, attendance, schedules, contracts, CRM, inventory, payroll, tasks, locations, files, notifications, and POS administration and synchronization.

**Out of scope:** public web presentation, POS screen interaction, payment processing, delivery logistics, and unverified POS hardware integrations.

## Domain Model

An **Employee** has employment information, attendance, work schedules, and may be associated with contracts and payroll records. A **User** has an approval state and role-based access; managers are scoped to assigned **Headquarters**.

**Opportunities**, **Projects**, and **Tasks** represent commercial and operational work. **Inventory Items**, locations, stock, and transactions record inventory movement. **POS Devices** belong to a headquarters and synchronize approved catalog, configuration, and operational event information for the cafeteria.

## Business Rules

- Registration does not grant staff access until an administrator approves the user.
- Headquarters access limits a manager to the locations assigned to that manager; an administrator has company-wide access.
- POS events are accepted with idempotency handling so a retried device request does not create the same confirmed event twice.
