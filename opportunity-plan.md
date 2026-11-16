# Plan: Convocatorias (`opportunity`)

Estado: **MVP F0–F5 implementado** · 2026-11-16  
Post: `feat: remove CRM module`. Caso de referencia: ESFM/IPN (`context.md`).

Principio: **archivo operativo + lifecycle corto**. No CRM, no ERP de ejecución, no workflow engine, no CRUD plano.

---

## 0. Qué falta definir (poco)

Casi todo de producto ya está cerrado. Para arrancar **no bloquea nada**: D9–D14 se adoptan como decididas (recomendaciones). Solo quedan detalles de implementación que se resuelven en la fase correspondiente.

| Tema | Estado | Dónde se cierra |
|------|--------|----------------|
| Producto D1–D8 | Cerrado | §1 |
| Dominio D9–D14 | **Cerrado por defecto** (abajo) | §1 / §3 |
| Labels español de enums | Pendiente trivial | Al tocar UI (`enum-labels.ts`) |
| Número exacto Flyway | Pendiente trivial | Siguiente = `V27__…` (hoy último `V26`) |
| Wire JSON de cada endpoint | Pendiente | Fase F1–F3 al escribir controllers (OpenAPI `Doc*`) |
| Historial de transiciones (tabla audit) | **Fuera del MVP** | Basta `updatedAt` + timestamps de hito |
| Reabrir terminales | **Fuera del MVP** | Post-MVP si hace falta |

Si más adelante quieres cambiar D14 (bloquear → avisar), es un cambio de una regla en `markSubmitted`.

---

## 1. Decisiones (todas cerradas)

| ID | Decisión |
|----|----------|
| D1 | Módulo `opportunity` · `/api/v1/opportunities` |
| D2 | UI: **Convocatorias** |
| D3 | Estados fijos (abajo) |
| D4 | Datos + transiciones explícitas |
| D5 | `WON` → crear/vincular `business_contract` |
| D6 | Checklist interno; sin `tasks.opportunity_id` |
| D7 | MVP hasta envío + resultado |
| D8 | Front ERP **ADMIN**; API **ADMIN** |
| D9 | `organizations` dentro del módulo `opportunity` |
| D10 | Requisitos = tabla hija (no JSON) |
| D11 | Propuestas = filas con `kind` (`TECHNICAL` / `ECONOMIC` / `COMBINED`) |
| D12 | Nuevo `ContractCategory.INSTITUTIONAL` |
| D13 | Docs vía `files` (`module=opportunity` + `entityType` + `entityId`) |
| D14 | `markSubmitted` **bloquea** si hay mandatory no `DONE`/`WAIVED` |

### Estados

```
DRAFT → UNDER_REVIEW → PREPARING → SUBMITTED → WON | LOST | ABANDONED
                         ↘ CANCELLED
```

| Acción | Transición |
|--------|------------|
| `submitForReview` | `DRAFT` → `UNDER_REVIEW` |
| `startPreparing` | `UNDER_REVIEW` → `PREPARING` |
| `markSubmitted` | `PREPARING` → `SUBMITTED` (+ gate D14) |
| `markWon` | `SUBMITTED` → `WON` (+ contrato) |
| `markLost` | `SUBMITTED` → `LOST` |
| `abandon` | no terminal → `ABANDONED` |
| `cancel` | antes de `SUBMITTED` → `CANCELLED` |

---

## 2. Alcance MVP vs fuera

### Entra (cliente usable)

- Organizaciones + convocatorias
- Lifecycle con botones de acción
- Checklist de requisitos + evidencia (files)
- Documentos de la convocatoria (files)
- Paquete(s) de propuesta + archivos
- Ganar/perder/abandonar; al ganar, contrato `INSTITUTIONAL` vinculado
- Pantallas ERP ADMIN: lista, detalle, formularios mínimos

### No entra

- Workflows / plantillas enterprise / costos
- Post-adjudicación operativa (POS, sede, nómina del contrato)
- Reabrir terminales, audit log dedicado, roles no-ADMIN
- Módulo `tasks` colgado a la convocatoria

### Criterio de hecho (demo cliente)

1. Alta org IPN–ESFM  
2. Alta convocatoria 2027 + PDF  
3. Checklist con avances y evidencia  
4. Propuesta adjunta  
5. Enviar → ganar o perder  
6. Si gana → aparece contrato en Contratos y link en la convocatoria  

---

## 3. Modelo (resumen para implementar)

```
organizations 1 ──< opportunities
                       ├── opportunity_requirements
                       ├── opportunity_proposals
                       └── contract_id? → business_contracts
files: module=opportunity, entityType=opportunity|requirement|proposal
```

Campos: ver tablas en §3 del historial de diseño (mínimos ya acordados). Enums VARCHAR + `ck_*` (convención backend).

**Files**

| Uso | `entityType` | `entityId` |
|-----|--------------|------------|
| Anexos convocatoria | `opportunity` | opportunity id |
| Evidencia requisito | `requirement` | requirement id |
| Propuesta | `proposal` | proposal id |

---

## 4. Plan por fases (E2E)

Cada fase deja algo **compilable + usable** (API y/o UI). No esperar al final para ver pantallas: F2 ya es usable para captura; F5 cierra el MVP.

### F0 — Prep (½ día)

**Alcance:** alinear repo y convenciones; cero dominio de negocio aún.

- [x] Confirmar skills: `pimienta-backend-conventions`, domain-style, openapi, IT; front UI skill
- [x] Nav ERP: ítem **Convocatorias** (ADMIN)
- [x] Actualizar `AGENTS.md` (root + backend + web) + este plan

**Salida:** equipo listo; migración `V27`.

---

### F1 — Cimiento DB + API núcleo (backend first)

**Alcance:** organizaciones + convocatorias + estados. Sin checklist/propuestas aún.

**DB (`V27__opportunity_core.sql`)**

- [x] `organizations`
- [x] `opportunities` (+ FK org, `status`, fechas, `contract_id` nullable FK)
- [x] CHECK enums

**Backend**

- [x] Paquete `module.opportunity` hexagonal (`infrastructure/adapter`)
- [x] CRUD/search Organization
- [x] CRUD/search Opportunity + acciones de transición (sin gate de requisitos; `markSubmitted` libre hasta F3)
- [x] `markWon` / `markLost` / etc. sin bridge de contrato aún
- [x] Security: cae en `ADMIN` (`anyRequest`)
- [x] OpenAPI `Doc*` + `OpportunityIntegrationTest`

**Frontend (mínimo usable)**

- [x] Rutas `/app/erp/convocatorias`, `/app/erp/convocatorias/:id`
- [x] Nav ERP ADMIN: Convocatorias
- [x] Lista + detalle + crear/editar + botones de estado
- [x] `enum-labels` para status / kind
- [x] Services HTTP en `core/opportunity/`

**Salida cliente:** puede dar de alta orgs/convocatorias y mover el lifecycle básico (sin checklist ni PDFs de propuesta).

---

### F2 — Files en convocatoria

**Alcance:** adjuntar PDF de convocatoria / anexos.

- [x] Front: upload/list usando API `files` existente (`module=opportunity`, `entityType=opportunity`)
- [x] Detalle convocatoria: sección Documentos
- [x] Añadir opción en filtros de Archivos ERP (`file.enums` → Convocatorias)

**Salida cliente:** convocatoria + documentos oficiales guardados.

---

### F3 — Checklist (requirements) + gate D14

**Alcance:** seguimiento real de preparación.

**DB (`V28__opportunity_requirements.sql`)**

- [x] `opportunity_requirements`

**Backend**

- [x] CRUD requisitos bajo `/opportunities/{id}/requirements`
- [x] Cambiar status del ítem (`PUT …/status`)
- [x] Evidencia = files `entityType=requirement`
- [x] `markSubmitted` aplica D14 (bloquear si mandatory pendientes)

**Frontend**

- [x] Lista checklist en detalle
- [x] Alta/edición rápida de ítems
- [x] Upload evidencia por ítem
- [x] Feedback claro si `markSubmitted` falla por gate (+ aviso en PREPARING)

**Salida cliente:** preparar una convocatoria con requisitos y evidencia; no deja “enviar” a medias.

---

### F4 — Propuestas

**Alcance:** contenedor técnico/económico + archivos.

**DB (`V29__opportunity_proposals.sql`)**

- [x] `opportunity_proposals`

**Backend**

- [x] CRUD proposals bajo `/opportunities/{id}/proposals`
- [x] Files `entityType=proposal`

**Frontend**

- [x] Sección Propuesta(s) en detalle (kind + notas + archivos)

**Salida cliente:** expediente completo pre-envío (docs + checklist + propuesta).

---

### F5 — Bridge contrato + cierre MVP

**Alcance:** resultado comercial usable con el resto del ERP.

**Backend**

- [x] `ContractCategory.INSTITUTIONAL` (+ `V30__contract_category_institutional.sql`)
- [x] `markWon`: crea contrato (nombre/fechas desde opportunity) + set `contractId`
- [x] Vincular existente: nice-to-have omitido; crear-auto si `contractId` es null
- [x] IT: won → contrato `INSTITUTIONAL` visible en `/contracts/{id}`

**Frontend**

- [x] En detalle: link a `/app/erp/contratos` con `contractId`
- [x] Labels categoría institucional (+ opción en alta de Contratos)
- [x] Recorrido demo §2 posible end-to-end

**Salida cliente:** MVP cerrado end-to-end.

---

## 5. Orden de arranque recomendado (esta semana)

| Día | Fase | Foco |
|-----|------|------|
| 1 | F0 + F1 DB/domain | Migración + aggregates + use cases estados |
| 2 | F1 API + IT | Controllers + tests verdes |
| 3 | F1 UI | Lista/detalle/acciones usable |
| 4 | F2 + F3 | Files + checklist + gate |
| 5 | F4 + F5 | Propuestas + contrato + demo |

Ajustar si el día 1–2 se alarga: **no empezar F3 sin UI de F1**; el cliente necesita ver pantallas pronto.

---

## 6. Superficie API (borrador)

Prefijo: `/api/v1`

| Método | Ruta | Fase |
|--------|------|------|
| CRUD/search | `/organizations` | F1 |
| CRUD/search | `/opportunities` | F1 |
| POST | `/opportunities/{id}/submit-for-review` | F1 |
| POST | `/opportunities/{id}/start-preparing` | F1 |
| POST | `/opportunities/{id}/mark-submitted` | F1/F3 |
| POST | `/opportunities/{id}/mark-won` | F1/F5 |
| POST | `/opportunities/{id}/mark-lost` | F1 |
| POST | `/opportunities/{id}/abandon` | F1 |
| POST | `/opportunities/{id}/cancel` | F1 |
| CRUD | `/opportunities/{id}/requirements` | F3 |
| PATCH | `/opportunities/{id}/requirements/{rid}/status` | F3 |
| CRUD | `/opportunities/{id}/proposals` | F4 |
| (existente) | `/files/...` con module opportunity | F2+ |

Nombres finales de path pueden alinearse al estilo del repo al implementar (kebab, sin duplicar verbos raros).

---

## 7. UI (borrador)

| Ruta | Contenido | Fase |
|------|-----------|------|
| `/app/erp/convocatorias` | Lista + filtros status/org/search | F1 |
| `/app/erp/convocatorias/nueva` | Form alta | F1 |
| `/app/erp/convocatorias/:id` | Detalle: datos, estados, docs, checklist, propuestas, contrato | F1→F5 |
| Orgs | Picker en form + alta rápida modal/página simple | F1 |

Patrón visual/HTTP: igual que Empleados / Contratos / Tareas (signals + services Observable).

---

## 8. Fuera de alcance (post-MVP posibles)

1. Reabrir terminales  
2. Plantillas de checklist reutilizables  
3. Recordatorios/notificaciones por `submissionDeadline`  
4. Costeo / utilidad de la propuesta económica  
5. Servicio operativo post-contrato  
6. Audit trail de transiciones  

---

## 9. Cómo empezar mañana

1. Abrir `backend/.agents/skills/pimienta-backend-conventions/SKILL.md`  
2. Crear `V27__opportunity_core.sql` (`organizations` + `opportunities`)  
3. Scaffold `module.opportunity` + Organization/Opportunity + transiciones F1  
4. En paralelo o seguido: rutas Angular Convocatorias  

Este archivo es la **única fuente de plan** del módulo (root). No hay carpeta `docs/v2/opportunity`.
