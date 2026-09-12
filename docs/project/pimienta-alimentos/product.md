## Problem / Vision

Pimienta Alimentos needs one reliable way to run its daily company operations while serving school cafeterias. The suite brings public company information, staff work, operational records, and cafeteria sales into connected applications instead of disconnected manual processes.

## Users / Actors

- **Visitors and clients** learn about Pimienta Alimentos, its services, and its commitments through the public website.
- **Administrative staff** manage people, work, commercial activity, documents, and company locations.
- **Managers** oversee the work and point-of-sale operation assigned to their locations.
- **Cafeteria operators** open a shift, sell products, and close their operation at the point of sale.
- **Administrators** configure locations, access, devices, catalog data, and operational follow-up.

## Scope

**In scope:** public company information; staff access; employee, attendance, contract, payroll, CRM, task, inventory, file, notification, and location operations; school-cafeteria sales and their central follow-up.

**Out of scope:** customer online ordering and payment gateways, delivery logistics, and unattended hardware operation. The POS hardware integrations remain outside the current verified delivery.

## Platforms

| Platform | Covers | Docs |
|---|---|---|
| Web app | Public company site, staff workspace, and central POS administration | `pimienta-web/product.md` |
| API | Shared operational records, access control, and POS synchronization | `pimienta-backend/product.md` |
| Android POS | On-site school-cafeteria sales and local shift operation | `pimienta-pos/product.md` |

## Product Rules

- Staff access requires administrator approval and follows the permissions assigned to the person.
- Managers only operate on the company locations assigned to them; administrators retain global access.
- A cafeteria sale is recorded locally first so the operator can continue working during an interruption, then is reconciled with the central records.
