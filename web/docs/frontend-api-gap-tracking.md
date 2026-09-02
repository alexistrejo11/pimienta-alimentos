# Front ↔ API (`docs/api-docs.yaml`): auditoría y pendientes

Referencia: [api-docs.yaml](./api-docs.yaml) (OpenAPI 3.1) y código en `src/app/core/**`.

## Llamadas HTTP que usa el front hoy

| Servicio | Método | Ruta | ¿En OpenAPI? |
|----------|--------|------|--------------|
| `AuthService` | POST | `/api/v1/auth/register` | Sí |
| `AuthService` | POST | `/api/v1/auth/login` | Sí |
| `UserProfileService` | GET, PATCH | `/api/v1/users/me` | Sí |
| `UserProfileService` | GET | `/api/v1/users/me/dashboard` | Sí |
| `EmployeeService` | GET | `/api/v1/employees`, `.../{id}`, `.../statistics`, `.../summary` | Sí |
| `HeadquarterService` | GET | `/api/v1/headquarters`, `.../{id}`, `.../statistics` | Sí |
| `TaskService` | GET | `/api/v1/tasks`, `.../{id}` | Sí |
| `CrmService` | GET | `/api/v1/opportunities`, `.../{id}`, `.../{id}/summary` | Sí |
| `CrmService` | GET | `/api/v1/projects`, `.../{id}`, `.../{id}/summary`, `.../{id}/milestones` | Sí |
| `ContractService` | GET, POST | `/api/v1/contracts` | Sí |
| `HomeLanding` | POST | `CONTACT_ENDPOINT` (ver abajo) | No es Pimienta |

Todas las rutas anteriores existen en la spec; métodos y prefijos (`/api/v1`) coinciden con `API_BASE_URL`.

## Desajustes que afectan comportamiento

### 1. Registro (`POST /api/v1/auth/register`)

- **Resuelto en el front:** `AuthService` y la pantalla de registro usan `RegisterResponse`; se muestra el mensaje del servidor y no se guardan tokens. Tras aprobación administrativa, el usuario usa **Iniciar sesión**.

### 2. Formulario de contacto (landing)

- `src/app/pages/home/contact-endpoint.ts` apunta por defecto a `https://httpbin.org/post`.
- No hay endpoint de contacto en `api-docs.yaml` para Pimienta. **Pendiente:** definir backend o servicio externo y sustituir `CONTACT_ENDPOINT`.

### 3. OpenAPI y parámetros `filter` / `pageable` “required”

En varios `GET` la spec declara parámetros compuestos `filter` u objetos `pageable` como obligatorios. En Spring suelen resolverse con `@ModelAttribute` y valores por defecto para `page`/`size`. Si aparecieran **400** en listados, revisar binding real vs documentación generada.

## Cobertura de API: cosas documentadas que el front aún no usa

Útil para priorizar producto (el workspace ya tiene rutas placeholder: `clients`, `inventory`, `tasks` antiguas, etc.):

- **Auth:** `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout` — los DTOs existen en `auth.dto.ts`, pero no hay métodos en `AuthService` ni flujo de renovación de token / cierre de sesión contra API.
- **Contratos:** muchas rutas bajo `/api/v1/contracts/{id}` (renovar, extender, etc.) — el front solo lista y crea.
- **CRM:** creación/actualización de oportunidades y proyectos, transiciones de pipeline, import/export, tareas anidadas, etc.
- **Tareas:** creación, PATCH estado/asignación/checklist, export/import — el front solo lista y detalle.
- **Empleados:** alta, baja, import, etc. — el front solo lectura + estadísticas.
- **Sedes:** POST/PATCH/DELETE, import/export — el front solo lectura.
- **Inventario, payroll, usuarios admin:** módulos en OpenAPI sin integración en este Angular.

## Cambio aplicado en código (hitos de proyecto)

`GET /api/v1/projects/{projectId}/milestones` devuelve **`PagedResponse`** (`items` + `metadata`), no un array. `CrmService.listMilestones` ahora interpreta `items` para mantener el resto de la UI igual.

---

*Generado como seguimiento de alineación front/API; actualizar cuando cambie la spec o los servicios.*
