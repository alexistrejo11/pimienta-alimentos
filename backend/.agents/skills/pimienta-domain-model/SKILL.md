---
name: pimienta-domain-model
description: >-
  Paradigma de modelos de dominio en Pimienta (BaseDomain, SafeBuilder, null-safety,
  persistencia relajada). Usar al crear o refactorizar agregados en backend Java.
---

**English (canonical):** see **`pimienta-domain-repository-style`** and **`pimienta-backend-conventions`** in this folder for the same rules and API documentation patterns, kept up to date for new modules.

# Dominio Pimienta (hexagonal / repositorio)

## Rol del dominio

El backend se orienta a un **repositorio de datos**: la base puede tener `NULL` en muchos campos no críticos. El **dominio** no propaga `null` hacia arriba: expone **valores por defecto** (cadenas vacías, enums centinela, objetos vacíos) para que la aplicación y los adaptadores trabajen sin `NullPointerException`.

## BaseDomain

- Todo agregado o entidad de dominio con identidad y auditoría debe **extender** `io.github.alexistrejo11.pimienta.shared.BaseDomain<ID>`.
- Campos comunes: `id`, `createdAt`, `updatedAt`, `deletedAt`, `version`.
- No sustituir `BaseDomain` con records anónimos de “params” para armar el agregado: el estado vive en la clase de dominio y en su **builder**.

## SafeBuilder (patrón Employee / Contract)

- `public static SafeBuilder builder()` devuelve el builder anidado.
- Métodos `with*` **normalizan** entradas `null` (por ejemplo, texto vacío, compactación de moneda, `blank` → `null` donde corresponda a persistencia).
- **No** usar records `*RegisterParams`, `*ReviseParams`, `*ReconstructParams` para el agregado: el flujo es `Contract.builder()....register()` / `revise(existing)` / `reconstruct()`.
- `build()` o `reconstruct()`: ensamblaje desde persistencia **sin** validación de reglas de negocio del alta.
- `register()`: alta con validación de invariantes de creación.
- `revise(Existing)`: nueva instancia para actualización conservando identidad, historia relevante y auditoría, con validación de revisión.

## Getters y setters seguros

- Los **getters** devuelven defaults estables si el campo interno es `null` (por ejemplo `""`, enum `UNDEFINED`, cero donde aplique).
- Los **setters** reescriben `null` al default apropiado para no “romper” el agregado después de un merge parcial.
- Cuando haya que escribir en JPA un valor distinto del centinela de presentación (por ejemplo no persistir `UNDEFINED`), exponer métodos explícitos `getXxxOrNull()` solo donde haga falta el mapeo.

## Enums centinela

- Si un enum puede faltar en BD, añadir un literal **`UNDEFINED`** (o equivalente) y usarlo solo en **lectura** vía getters; validar en `register`/`revise` que no se den de alta contratos con categoría o tipo de plazo indefinido en sentido de “no especificado”.

## JPA (adaptador de salida)

- Quitar `nullable = false` en columnas que **no** sean críticas para integridad (nombres descriptivos opcionales, categorías recuperables, etc.).
- Mantener restricciones fuertes donde el negocio o el locking lo exijan (por ejemplo `created_at`, `updated_at`, `version`, contadores que no admiten ausencia en el modelo físico).
- El dominio tolera `null` desde BD; el mapper usa `Contract.builder()....reconstruct()`.

## Referencia en código

- Estilo de referencia empleados: `module/employees/core/domain/Employee.java`.
- Contrato alineado: `module/contract/core/domain/Contract.java` y `BusinessContractJpaEntity`.

Al añadir módulos nuevos, **reaplicar** este skill antes de introducir DTOs de params en el dominio o `@Column(nullable = false)` por defecto en todo.
