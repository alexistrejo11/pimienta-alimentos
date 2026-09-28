# Payroll module — follow-ups after integration tests

Items to revisit for correctness, API semantics, or UX. None of these block the happy-path integration flow unless noted.

## HTTP and errors

- **`IllegalArgumentException` → 400 `INVALID_ARGUMENT`:** Missing payroll record (e.g. bad id on adjustment) returns generic **400** with message **"Invalid request."** instead of **404** with a domain error code. Clients cannot distinguish “not found” from other bad input without parsing logs.

- **Empty import file:** Controller throws **`ResponseStatusException`** (400) with reason **"Archivo vacío"**; the global handler maps this to **`INVALID_ARGUMENT`** / **"Bad request."** on the wire, so the Spanish reason is not exposed consistently in **`ApiErrorResponse`**.

## Domain / data integrity

- **Payments without payroll record:** **`RegisterPayrollPaymentRequest`** has no **`payrollRecordId`**. **`saveDebt`** uses **`saved.getPayrollRecordId()`**, which can be **null**, so debts may not link back to a record. Decide whether payments must reference a **`PayrollRecord`** and enforce it at the use case or DB layer.

- **`employeeId` on records:** No FK to the employees table in JPA; arbitrary ids persist. Acceptable for an internal module, or add validation if referential integrity matters.

## Adjustments and status

- **`registerAdjustment` status rule:** Net after adjustment **`<= 0`** maps the record to **`DEFERRED`**, otherwise **`PENDING`**. Large discounts relative to net can flip status in ways finance may want to validate or document.

- **Synthetic payment on adjustment:** When there is no prior payment for the record, the use case builds a **synthetic** **`PayrollPayment`** for the response. **`paymentIdOrSynthetic`** in logs may be **null** id — document for API consumers if **`id`** can be null in edge cases (check **`PayrollPaymentResponse`** serialization).

## Bulk export

- **Export pagination:** **`exportPayrollRecords`** walks pages with an internal **`PAGE_SIZE` (500)** and uses **`Pageable`** mainly for **sort**. Query params **`page` / `size`** on **`GET /export`** may look like normal pagination but do not limit the export the same way as list endpoints. Consider documenting or aligning behavior.

## Concurrency

- **Version field (resolved 2026-09-27):** **`BaseJpaEntity`** now extends **`VersionedJpaEntity`**, which declares **`@Version`** and seeds new rows at **1**. Payroll entities have JPA optimistic locking; a stale update raises **`ObjectOptimisticLockingFailureException`** instead of last-write-wins. Mappers pass **`domain.getVersion()`** through (null for new rows); do not assign or bump it manually.

## Tests

- **`PayrollIntegrationTest`** asserts **`PagedResponse`** shape (**`items`**, **`metadata`**) and XLSX headers for export. If the API switches to raw **`Page`**, update assertions accordingly.
