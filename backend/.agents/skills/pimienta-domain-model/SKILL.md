---
name: pimienta-domain-model
description: >-
  Alias (Spanish) for Pimienta domain paradigm. Canonical rules live in
  pimienta-domain-repository-style — read that skill instead when creating or refactoring
  aggregates in backend Java. Use only if the user asks in Spanish for dominio/SafeBuilder.
---

**Canonical (English):** open **`pimienta-domain-repository-style`** and **`pimienta-backend-conventions`**.
Do **not** invent rules here that are missing from those skills. If they conflict, English wins.

# Dominio Pimienta (resumen)

- Agregados = **holders de estado** (`BaseDomain` + `SafeBuilder`); workflow en use cases.
- `register()` / `reconstruct()`; `revise()` solo donde el módulo ya lo tenga (p. ej. Contract).
- Sin Spring/JPA en `core/domain`. Validación Jakarta en DTOs HTTP.
- JPA: `nullable = true` en columnas no críticas; mappers con `blankToNull` / `*OrNull` en código nuevo.
- Referencia: `module/employees`, `module/contract`, `module/task`, `module/headquarter`.
