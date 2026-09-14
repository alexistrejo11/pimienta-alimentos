# Physical inventory count workflow

`POST /api/v1/inventory/count-sessions` opens a `FULL` count for every existing stock row at a location, or a `PARTIAL` count for the requested `itemIds`. A location can have only one `DRAFT` or `SUBMITTED` session. Responses are blind until approval: expected quantity and variance are not returned while the session is open.

The creator records counted quantities with `POST /{id}/responses`, then submits with `POST /{id}/submit`. A different authorized staff user approves with `POST /{id}/approve`; the service re-reads current stock and applies `current stock + (counted - expected)`. The resulting adjustment is appended to the inventory movement ledger, preserving movements made after the count snapshot. Sessions may be cancelled before approval.
