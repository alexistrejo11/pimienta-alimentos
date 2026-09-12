# POS integration (backend, historical)

> Superseded on 2026-09-08. The canonical cross-app contract and Android
> implementation plan are now in
> [mobile/pos/docs/integration](../../../../mobile/pos/docs/integration/README.md).
> Keep this directory only as the implementation-era record; do not update it
> or use it as a second source of truth.

Estado: **specs alineadas** · corte 1 decidido · 2026-09-08

Preparar el API Spring Boot para el punto de venta Android **sin implementar todavía la sincronización**. Esta carpeta es el contrato y el diseño del servidor. El POS móvil se engancha después, cuando el backend exista de verdad.

Decisiones: [01-decisiones-abiertas.md](01-decisiones-abiertas.md) · Contratos JSON: [contracts/](contracts/README.md) · Plan: [07-plan-de-modulos.md](07-plan-de-modulos.md) · **Progreso:** [09-implementation-tracker.md](09-implementation-tracker.md)

## Cómo leer

1. Producto: [`mobile/pos/docs/product/`](../../../../mobile/pos/docs/product/README.md)
2. Auditoría: [00-auditoria-backend-y-pos.md](00-auditoria-backend-y-pos.md)
3. Decisiones cerradas: [01-decisiones-abiertas.md](01-decisiones-abiertas.md)
4. Diseño: [02](02-contexto-y-limites.md)–[06](06-admin-api-web.md)
5. Ejemplos HTTP: [contracts/](contracts/README.md)
6. Orden de implementación: [07-plan-de-modulos.md](07-plan-de-modulos.md)
7. Checklist vivo (marcar avance): [09-implementation-tracker.md](09-implementation-tracker.md)

Si producto y arquitectura del móvil entran en conflicto, primero se aclara la regla de negocio y después se actualiza este contrato. **Este directorio es la fuente de verdad del servidor**; `mobile/pos/docs/architecture/04-sincronizacion-y-api.md` es un bosquejo previo.

## Estados

- **borrador** — propuesta
- **decidido** — aprobado para contratos o código
- **implementado** — ya existe en el backend

## Índice

| Doc | Corte | Estado | Contenido |
|-----|-------|--------|-----------|
| [00-auditoria…](00-auditoria-backend-y-pos.md) | 0 | factual | Qué hay hoy en código |
| [01-decisiones…](01-decisiones-abiertas.md) | 1 | decidido | D1–D10 |
| [02-contexto…](02-contexto-y-limites.md) | 2 | decidido | Límites de `module.pos` |
| [03-modelo…](03-modelo-de-dominio-backend.md) | 2 | decidido | Agregados e invariantes |
| [04-impacto…](04-impacto-en-modulos-existentes.md) | 2 | decidido | Headquarter / inventory / security |
| [05-device-api.md](05-device-api.md) | 3 | decidido | `/api/v1/pos/**` device |
| [06-admin-api-web.md](06-admin-api-web.md) | 3 | decidido | Web + `/pos/admin/**` |
| [07-plan…](07-plan-de-modulos.md) | 4 | decidido | Cortes B0–B7 (orden) |
| [08-fuera…](08-fuera-de-alcance.md) | 4 | decidido | No entra |
| [09-tracker…](09-implementation-tracker.md) | 4 | activo | Checklist de progreso |
| [contracts/](contracts/README.md) | 3 | decidido | JSON de ejemplo |

## Convenciones

- Prosa en español. Campos/rutas/eventos en inglés.
- Controllers thin, hexagonal — [backend/AGENTS.md](../../../AGENTS.md).
- Device API nueva; Admin web **extiende** recursos existentes (sin CRUD `/admin` duplicado).
- Wire POS: centavos enteros; maestros Long como string decimal; hechos UUID.
- No implementar networking Android ni `PosController` real hasta B2+.

## Relación con el POS

| Lado | Rol |
|------|-----|
| Tablet | Autoridad de la venta cobrada y hechos pendientes |
| Backend | Catálogo, operadores, config, inventario consolidado, incidencias |
| Web | Administración maestra, reportes, enrolamiento, Superadmin |
