## Overview

REST over HTTPS with JSON payloads. The production base URL is `https://api.pimienta-alimentos.com`; operational endpoints use the `/api/v1` prefix. The live OpenAPI 3.1 contract identifies the service as **Pimienta Alimentos API**, version `v1`.

## Documentation Tooling

- Swagger UI: [https://api.pimienta-alimentos.com/swagger-ui](https://api.pimienta-alimentos.com/swagger-ui)
- OpenAPI JSON: [https://api.pimienta-alimentos.com/v3/api-docs](https://api.pimienta-alimentos.com/v3/api-docs)

The runtime OpenAPI document is the authoritative detailed contract, including request bodies and schemas.

## Authentication

Staff endpoints use a Bearer JWT obtained from the authentication endpoints. Send it on every protected request:

```http
Authorization: Bearer <access_token>
```

`POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`, and `GET /health` do not require a staff token. POS enrollment and refresh establish device credentials; device synchronization then uses the device JWT returned by that flow.

## Request and Response Conventions

JSON is the standard media type. Spreadsheet exports use the XLSX media type. Deletes normally return `204 No Content`; certain action endpoints return `200 OK`.

Most list endpoints accept zero-based pagination:

```http
GET /api/v1/employees?page=0&size=20
Authorization: Bearer <access_token>
```

The standard paginated response contains `items` and `metadata`:

```json
{
  "items": [],
  "metadata": {
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

`GET /api/v1/headquarters` is an exception and returns a Spring-style page with `content` instead of `items`.

## Endpoints

| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/api/v1/auth/login` | Authenticate a staff user and obtain tokens. | none |
| POST | `/api/v1/auth/refresh` | Exchange a refresh token for a new access token. | none |
| GET | `/api/v1/users/me` | Read the current staff profile. | required |
| GET, POST, PUT, DELETE | `/api/v1/employees` | Manage employees and related HR workflows. | required |
| GET, POST, PUT, DELETE | `/api/v1/contracts` | Manage employment contracts and renewals. | required |
| GET, POST, PUT, DELETE | `/api/v1/opportunities`, `/api/v1/projects` | Manage CRM opportunities and projects. | required |
| GET, POST, PUT, DELETE | `/api/v1/tasks` | Manage operational tasks, assignments, and checklists. | required |
| GET, POST, PUT, DELETE | `/api/v1/headquarters` | Manage company locations and their POS settings. | required |
| GET, POST, PUT, DELETE | `/api/v1/inventory/*` | Manage items, locations, stock, movements, and transactions. | required |
| GET, POST, PUT, DELETE | `/api/v1/payroll/*` | Manage payroll records, periods, payments, debts, and summaries. | required |
| GET, POST | `/api/v1/files/*` | Manage company files, resources, uploads, and download URLs. | required |
| GET | `/api/v1/notifications/*` | Read notification administration and manager logs. | required |
| GET, POST, PUT, DELETE | `/api/v1/pos/admin/*` | Manage POS operators, devices, enrollment codes, incidents, and reports. | required |
| POST | `/api/v1/pos/devices/enroll` | Enroll a POS device using a one-time code. | enrollment flow |
| POST | `/api/v1/pos/devices/refresh` | Refresh device credentials. | device refresh flow |
| GET | `/api/v1/pos/sync/bootstrap` | Obtain the initial device dataset. | device token |
| GET | `/api/v1/pos/sync/changes` | Read changes after a synchronization cursor. | device token |
| POST | `/api/v1/pos/sync/events` | Submit an idempotent batch of POS events. | device token |

## Error Handling

Errors use the `ApiErrorResponse` shape:

```json
{
  "errorCode": "...",
  "message": "...",
  "traceId": "...",
  "context": {},
  "fieldErrors": [{ "field": "...", "message": "..." }]
}
```

Common statuses are `400` for invalid input, `401` for missing or invalid credentials, `403` for insufficient access, `404` for missing records, `409` for conflicts, and `429` for rate limiting. Clients receiving `429` should honor `Retry-After` and `X-RateLimit-*` headers when present.
