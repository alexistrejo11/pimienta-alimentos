## Overview

Pimienta POS is a native Android application with a local-first design. Compose renders the operational interface, Room stores the device state and outbox, and WorkManager schedules network-constrained synchronization through the backend API.

## Operational Flow

```mermaid
flowchart LR
  Operator[Operator] --> UI[Compose sale and manager screens]
  UI --> Repository[POS repository]
  Repository --> Room[(Device Room database)]
  Room --> Outbox[Pending POS events]
  Outbox --> Worker[WorkManager sync worker]
  Worker --> API[Operations API]
  API --> Worker
  Worker --> Room
```

The local database is the immediate operational source for a device. Sandbox and production data are isolated. Production synchronization runs only when network conditions are available; the app does not make a completed local sale depend on a live connection.

## Enrollment and Synchronization

```mermaid
sequenceDiagram
  participant Device as POS device
  participant API as Operations API
  Device->>API: Enroll with code
  API-->>Device: Device credentials
  Device->>API: Bootstrap catalog and settings
  API-->>Device: Approved location data and cursor
  Device->>API: Send queued events, maximum 50
  API-->>Device: Accepted, duplicate, rejected, or review-required outcomes
  Device->>API: Request incremental changes with cursor
```

The worker refreshes credentials after `401`, requires re-enrollment after a refresh failure or `403`, and reboots its local view from a bootstrap after a `409` cursor response. It marks accepted, duplicate, and review-required events as terminal; rejected events remain blocked for operator follow-up.

## Boundaries

The POS owns local sale interaction, device state, and its outbox. The backend owns device authorization, central catalog and configuration, event acceptance, and company-wide records. The web workspace is the central administration surface for the data that the device consumes.
