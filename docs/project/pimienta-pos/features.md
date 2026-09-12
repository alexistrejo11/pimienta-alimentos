## Sale Operation

- Runtime mode selection and production enrollment establish whether the device operates with sandbox data or company data.
- Profile and PIN access, opening cash, catalog selection, cart, checkout, and lock state support the basic operator flow.
- Landscape and portrait layouts adapt the sales experience to the device orientation.

## Manager Operation

- A manager panel provides daily dashboard, daily cash close, inventory, history, and device/peripheral status areas.
- Local activity includes sales, waste, restock, cash cancellations, and withdrawals for later central reconciliation.
- History and reprint-oriented work are represented locally; hardware controls remain placeholder or disabled where no verified integration exists.

## Synchronization

- The app can enroll and refresh a device, read its identity, bootstrap initial data, retrieve changes, and submit event batches.
- Batches contain up to 50 eligible events and run periodically every 15 minutes when network is available.
- The implementation supports retries and recovery paths, but production readiness of the end-to-end contract and physical cafeteria operation has not yet been independently verified.
