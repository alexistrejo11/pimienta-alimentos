# Front ↔ API: auditoría y pendientes

Referencia: [api-docs.yaml](./api-docs.yaml) (OpenAPI 3.1, regenerar con backend en marcha) y código en `src/app/core/**`.

## Regenerar OpenAPI

Con el backend corriendo en `localhost:8080`:

```bash
curl -s http://localhost:8080/v3/api-docs -o web/docs/api-docs.yaml
```

## Matriz create/update (formularios web vs backend)

Estado tras auditoría 2026-09-11:

| Módulo | Request backend | Campos @NotNull backend | Front (antes) | Estado |
|--------|-----------------|-------------------------|---------------|--------|
| POS operadores | `CreatePosOperatorRequest` | `headquarterIds` @NotEmpty | Input manual / omitido | **Fixed** (plan POS) |
| Tareas create | `TaskRequest` | solo `title` | IDs sede/proyecto/opp/creador | **Fixed** — selectores |
| Tareas assign | `AssignTaskRequest` | `employeeId` | Input número | **Fixed** — `app-employee-select` |
| Proyectos create | `CreateProjectRequest` | `clientId`, valores, código | `clientId` sin API | **Fixed** — `GET/POST /clients` + selector |
| Oportunidades | `CreateOpportunityRequest` | contacto, empresa, valor | OK en validación | **Fixed** — vendedor con selector |
| Nómina registro | `RegisterPayrollRecordRequest` | `employeeId`, fechas, bruto | IDs manuales | **Fixed** — selectores |
| Asistencia check-in | `StartWorkdayRequest` | `headquarterId` | Default `1` | **Fixed** — `app-headquarter-select` |
| Contratos | `CreateContractRequest` | dominio EMPLOYEE/FIXED_TERM | Selectores ya OK | OK |
| Auth register | `RegisterResponse` | — | Tokens no guardados | OK (resuelto antes) |

### Paginación (`PagedResponse` vs Spring `Page`)

- `GET /headquarters` → `content` (`SpringDataPage`)
- Resto de listados → `items` + `metadata` (`PagedResponse`)

## Nuevos endpoints backend (esta iteración)

| Método | Ruta | Uso en web |
|--------|------|------------|
| GET | `/api/v1/clients` | `ClientService.list`, `app-client-select` |
| POST | `/api/v1/clients` | Alta de clientes CRM (futuro) |
| — | `AttendanceResponse.employeeFullName` | Modales y búsqueda de asistencia |

## Servicios web añadidos/extendidos

- `EmployeeService.listActive()` → `/employees/active`
- `ClientService`, `EmployeeLookupService`, `CrmLookupService`, `ClientLookupService`, `PayrollLookupService`
- Componentes: `app-employee-select`, `app-client-select`, `app-opportunity-select`, `app-project-select`, `app-payroll-period-select`

## Pendientes conocidos

- Formulario de contacto landing (`CONTACT_ENDPOINT` → httpbin) — sin API Pimienta.
- `EmployeeListItemResponse` sin `photoUrl` (api-gaps §3).
- Auth refresh/logout no integrados en `AuthService`.
- Win opportunity (`POST …/win`) sin UI.

---

*Actualizar cuando cambie la spec o los servicios.*
