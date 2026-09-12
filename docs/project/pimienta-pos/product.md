## Problem / Vision

School cafeterias need to continue selling even when their connection is interrupted, while the company still needs reliable central follow-up. The POS gives cafeteria operators a focused sales tool and lets the company reconcile that activity with its broader operations.

## Users / Actors

- **Cafeteria operators** open a cash session, select products, collect a sale, and lock the device when they leave it.
- **Managers** review the current operation, inventory activity, sales history, and daily closing information.
- **Administrators** provision devices and maintain the centrally approved data that a cafeteria receives.

## Scope

**In scope:** device enrollment, operator access, opening cash, product sale and checkout, manager review, local inventory activity, history, daily cash close, and later synchronization with central records.

**Out of scope:** payment gateway integration, verified peripheral support, and unattended recovery of hardware failures. These need operational validation before they can be treated as delivered behavior.

## App Screens & Navigation

**Navigation:** The app begins with runtime mode selection and production enrollment. The operational flow moves through profile and PIN access, opening cash, catalog, cart, checkout, and lock state. Manager access opens operational sections for daily dashboard, daily closing, inventory, history, and status/peripherals.

**Key journey - cafeteria sale:** An operator opens the session, chooses products, completes a sale, and receives a local record immediately. When network conditions permit, the device sends the record for central reconciliation without preventing the next sale.

## Product Rules

- A production device must be enrolled before it can retrieve company-approved cafeteria data.
- A sale remains locally recorded until the device receives a terminal synchronization outcome.
- Central reports must make delayed or offline device data visible rather than treating absent synchronization as zero activity.
